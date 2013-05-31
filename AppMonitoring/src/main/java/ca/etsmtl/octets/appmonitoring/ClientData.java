package ca.etsmtl.octets.appmonitoring;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData;
import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.RequestData;
import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarData;

class ClientData implements Runnable, IClientConnection {
   private static final Logger LOGGER = Logger.getLogger(ClientData.class);

   private final ReentrantLock monitorListMutex = new ReentrantLock();
   private final ReentrantLock frameDataMutex = new ReentrantLock();

   private Socket socket;
   private DataInputStream dataInputStream;
   private DataOutputStream dataOutputStream;
   
   private Hashtable<String, ObjectHolder> monitorList = new Hashtable<>();
   private Hashtable<String, MonitoredObject> mRootList;

   private Thread mThread;
   private Boolean mCanRun = true;

   private MonitoredObject.AutoUpdater autoUpdater = new MonitoredObject.AutoUpdater();
   
   private int refreshRate = 10; // pms

   private final FrameData.Builder frameDataBuilder = FrameData.newBuilder();

   private IConnectionHolder connectionHolder;

   public ClientData(IConnectionHolder connectionHolder, Socket iClient, Hashtable<String, MonitoredObject> iRootList) throws IOException {
      LOGGER.info(iClient.getInetAddress().getHostName() +  " : Initialing connection");
      this.connectionHolder = connectionHolder;

      socket = iClient;
      mRootList = iRootList;

      dataInputStream = new DataInputStream(socket.getInputStream());
      dataOutputStream = new DataOutputStream(socket.getOutputStream());

      mThread = new Thread(this);
      mThread.start();

      connectionHolder.registerClient(this);

      LOGGER.info(iClient.getInetAddress().getHostName() + " : connection initialized.");

      for (MonitoredObject wObj : mRootList.values()) {
         updateObject(wObj);
      }
   }

   @Override
   public void triggerUpdate() {
      final VarData.Builder varDataBuilder = VarData.newBuilder();
      final FrameData.Type.Builder typeBuilder = FrameData.Type.newBuilder();
      final FrameData.Value.Builder valueBuilder = FrameData.Value.newBuilder();

      monitorListMutex.lock();
      for (ObjectHolder wHolder : monitorList.values()) {
         wHolder.getMonitoredObject().registerForUpdate(autoUpdater);
      }
      
      autoUpdater.executeUpdate();
      for (ObjectHolder objectHolder : monitorList.values()) {
         try
         {
            if(objectHolder.CheckForUpdate()) {
               String wPath = objectHolder.getName();
               if(!objectHolder.getPath().isEmpty())
                  wPath = objectHolder.getPath() + "." + wPath;

               typeBuilder.clear();
               typeBuilder.setName(objectHolder.getType());

               varDataBuilder.clear();
               varDataBuilder.setPath(wPath);
               varDataBuilder.setType(typeBuilder.build());

               String value = objectHolder.getStringValue();

               valueBuilder.clear();
               if(value == null) {
                  valueBuilder.setIsNull(true);
               } else {
                  valueBuilder.setIsNull(false);

                  valueBuilder.setValue(value);
                  varDataBuilder.setData(valueBuilder.build());
               }

               varDataBuilder.setDate(Calendar.getInstance().getTime().getTime());
               objectHolder.setRequestUpdate(false);

               frameDataMutex.lock();
               frameDataBuilder.addVarData(varDataBuilder);
               frameDataMutex.unlock();
            }
         }
         catch(ConcurrentModificationException e) {
            LOGGER.debug("ConcurrentModificationException",e);
         }
      }
      monitorListMutex.unlock();
   }

   private void updateObject(MonitoredObject monitoredObject) {
      final VarData.Builder varDataBuilder = VarData.newBuilder();
      final FrameData.Type.Builder typeBuilder = FrameData.Type.newBuilder();

      String path = monitoredObject.mName;
      if(!monitoredObject.mPath.isEmpty())
         path = monitoredObject.mPath + "." + path;

      typeBuilder.setName(monitoredObject.getTypeName());

      if(monitoredObject.getValueModifiers() != null) {
         for(FrameData.VarModifier modifier : monitoredObject.getValueModifiers()) {
            varDataBuilder.addValueModifier(modifier);
         }
      }

      if(monitoredObject.getClassModifiers() != null) {
         for (FrameData.VarModifier modifier : monitoredObject.getClassModifiers()) {
            varDataBuilder.addClassModifier(modifier);
         }
      }

      varDataBuilder.setType(typeBuilder.build());
      varDataBuilder.setPath(path);

      varDataBuilder.setData(FrameData.Value.newBuilder().setValue(monitoredObject.getStringValue())
              .setIsNull(monitoredObject.getStringValue() == null));
      varDataBuilder.setDate(Calendar.getInstance().getTime().getTime());


      frameDataMutex.lock();
      frameDataBuilder.addVarData(varDataBuilder.build());
      frameDataMutex.unlock();
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
         catch(InterruptedException | IOException e) {
            e.printStackTrace();
            close();
         }
      }
      
      //Close
      try {
         if(dataInputStream != null)
            dataInputStream.close();
         if(dataOutputStream != null)
            dataOutputStream.close();
      } catch (IOException e) {
         LOGGER.debug("Error closing stream.");
      }

      if(connectionHolder != null) connectionHolder.unRegisterClient(this);

      LOGGER.info(socket.getInetAddress().getHostName() + " : disconnected.");
   }

   private void readFromInputStream() throws IOException {

      if(dataInputStream.available() != 0) {
         byte[] toRead = new byte[dataInputStream.readInt()];
         if(dataInputStream.read(toRead) != -1) {
            FrameData frameData = DataPacketProto.FrameData.parseFrom(toRead);
            for(RequestData requestData : frameData.getRequestedDataList()) {
               runRequestData(requestData);
            }
         }
      }
   }

   private void writeBufferOutputStream() throws IOException {
      frameDataMutex.lock();
      final FrameData frameData = this.frameDataBuilder.build();
      this.frameDataBuilder.clear();
      frameDataMutex.unlock();

      try {
         if(frameData.getVarDataList().size() > 0) {
            byte[] bytes = frameData.toByteArray();
            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
            dataOutputStream.flush();
         }
      } catch (IOException e) {
         LOGGER.error("Error writing FrameData.",e);
         throw e;
      }
   }

   void runRequestData(RequestData requestData) {
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
