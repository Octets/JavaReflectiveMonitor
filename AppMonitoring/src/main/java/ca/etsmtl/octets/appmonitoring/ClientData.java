package ca.etsmtl.octets.appmonitoring;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData;
import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarData;

class ClientData implements Runnable, IClientConnection {
   private static final Logger LOGGER = Logger.getLogger(ClientData.class);

   private final ReentrantLock mWriterMutex = new ReentrantLock();
   private final ReentrantLock monitorListMutex = new ReentrantLock();

   private Socket socket;
   private BufferedInputStream inputStream;
   private OutputStream outputStream;
   
   private Hashtable<String, ObjectHolder> monitorList = new Hashtable<String, ObjectHolder>();
   private Hashtable<String, MonitoredObject> mRootList;

   private Thread mThread;
   private Boolean mCanRun = true;

   private MonitoredObject.WizeUpdater mWizeUpdater = new MonitoredObject.WizeUpdater();
   
   private int refreshRate = 50; // pms

   private final VarData.Builder varDataBuilder = VarData.newBuilder();
   private final DataPacketProto.FrameData.Type.Builder typeBuilder = DataPacketProto.FrameData.Type.newBuilder();
   private final DataPacketProto.FrameData.Value.Builder valueBuilder = DataPacketProto.FrameData.Value.newBuilder();

   private IConnectionHolder connectionHolder;

   public ClientData(IConnectionHolder connectionHolder, Socket iClient, Hashtable<String, MonitoredObject> iRootList) throws IOException {
      LOGGER.info(iClient.getInetAddress().getHostName() +  " : Initialing connection");
      this.connectionHolder = connectionHolder;

      socket = iClient;
      mRootList = iRootList;

      inputStream = new BufferedInputStream(socket.getInputStream());
      outputStream = socket.getOutputStream();

      mThread = new Thread(this);
      mThread.start();

      connectionHolder.registerClient(this);

      LOGGER.info(iClient.getInetAddress().getHostName() + " : connection initialized.");

      for (MonitoredObject wObj : mRootList.values()) {
         updateObject(wObj);
      }


   }

   //>ObjectData:Path:[content]:Type:[content]:Value:[content]:Visibility:[content]
   @Override
   public void triggerUpdate() {

      mWriterMutex.lock();
      monitorListMutex.lock();
      for (ObjectHolder wHolder : monitorList.values()) {
         wHolder.getObjectSpy().registerForUpdate(mWizeUpdater);
      }
      
      mWizeUpdater.executeUpdate();
      for (ObjectHolder wHolder : monitorList.values()) {
         try
         {
            if(wHolder.CheckForUpdate()) {
               String wPath = wHolder.getName();
               if(!wHolder.getPath().isEmpty())
                  wPath = wHolder.getPath() + "." + wPath;

               typeBuilder.clear();
               typeBuilder.setName(wHolder.getType());

               varDataBuilder.clear();
               varDataBuilder.setPath(wPath);
               varDataBuilder.setType(typeBuilder.build());

               String value = wHolder.getStringValue();

               if(value == null) {
                  varDataBuilder.setIsNull(true);
               } else {
                  varDataBuilder.setIsNull(false);

                  valueBuilder.clear();
                  valueBuilder.setValue(value);
                  varDataBuilder.setData(valueBuilder.build());
               }

               varDataBuilder.build().writeTo(outputStream);

               wHolder.setRequestUpdate(false);  
            }
         }
         catch(ConcurrentModificationException e) {
            LOGGER.debug("ConcurrentModificationException",e);
         }
         catch(IOException e){
            close();
         }
      }
      monitorListMutex.unlock();
      mWriterMutex.unlock();
   }

   private void updateObject(MonitoredObject wObj) {
      mWriterMutex.lock();
      try
      {
         String wPath = wObj.mName;
         if(!wObj.mPath.isEmpty())
            wPath = wObj.mPath + "." + wPath;
         
         String wData = ">ObjectData:Path:" + wPath;
         wData += ":Type:" + wObj.getTypeName();
         wData += ":Value:"+ URLEncoder.encode(wObj.toString(), "UTF-8");
         //wData += ":Visibility:" + wObj.getVisibility();
         //mWriter.write(wData);
         //mWriter.newLine();
         outputStream.flush();
      }
      catch(IOException e) {
         close();
      }
      mWriterMutex.unlock();
   }

   private void updateFieldsObject(MonitoredObject monitoredObject) {
      for(String field : monitoredObject.getFieldsName()) {
         MonitoredObject monitoredField = MonitoredObject.ObjectNavigation(monitoredObject.mChildren,field,monitoredObject);
         if(monitoredField != null) {
            updateObject(monitoredField);
         }
      }
   }

   @Override
   public void run() {
      StopWatch stopWatch = new StopWatch();
      while(mCanRun)
      {
         try
         {
            stopWatch.start();
            readFromInputStream();
            writeBufferOutputStream();
            stopWatch.stop();
            long timeRemaining = refreshRate - stopWatch.getTime();
            stopWatch.reset();
            if(timeRemaining > 0) {
               Thread.sleep(timeRemaining);
            }

         }
         catch(InterruptedException e) {
            e.printStackTrace();
            close();
         } catch (IOException e) {
            e.printStackTrace();
            close();
         }
      }
      
      //Close
      try {
         if(inputStream != null)
            inputStream.close();
         if(outputStream != null)
            outputStream.close();
      } catch (IOException e) {
         LOGGER.debug("Error closing stream.");
      }

      if(connectionHolder != null) connectionHolder.unRegisterClient(this);

      LOGGER.info(socket.getInetAddress().getHostName() + " : disconnected.");
   }

   private void readFromInputStream() throws IOException {
      if(inputStream.available() != 0) {
         FrameData frameData = DataPacketProto.FrameData.parseFrom(inputStream);
         for(FrameData.RequestData requestData : frameData.getRequestedDataList()) {
            runRequestData(requestData);
         }
      }
   }

   private void writeBufferOutputStream() throws IOException {

   }

   public void runRequestData(FrameData.RequestData requestData) throws IOException {
      MonitoredObject monitoredObject = MonitoredObject.ObjectNavigation(mRootList, requestData.getPath(), null);
      if(monitoredObject != null) {
         switch (requestData.getMode()) {
            case QUERY:
               updateObject(monitoredObject);
               updateFieldsObject(monitoredObject);
               break;
            case REGISTER:
               monitorListMutex.lock();
               if(!monitorList.contains(requestData.getPath())) {
                  monitorList.put(requestData.getPath(),new ObjectHolder(monitoredObject));
               }
               monitorListMutex.unlock();
               break;
            case UNREGISTER:
               monitorListMutex.lock();
               if(monitorList.contains(requestData.getPath())) {
                  monitorList.remove(requestData.getPath());
               }
               monitorListMutex.unlock();
               break;
         }
      } else {
         LOGGER.debug("Null object for " + requestData.getPath());
      }
   }

   @Override
   public int getRefreshRate() {
      return refreshRate;
   }

   @Override
   public void setRefreshRate(int refreshRate) {
      this.refreshRate = refreshRate;
   }

   public void close() {
      mCanRun = false;
      connectionHolder.unRegisterClient(this);
      if(mThread != null && mThread.isAlive()) {
         try {
            mThread.join();
         } catch (InterruptedException e) {
            LOGGER.error("Error closing ClientData Thread",e);
         }
      }
   }
}
