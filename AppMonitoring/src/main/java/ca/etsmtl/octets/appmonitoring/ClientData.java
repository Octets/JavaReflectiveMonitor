package ca.etsmtl.octets.appmonitoring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

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
   private final ReentrantLock mMonitorListMutex = new ReentrantLock();

   private IConnectionHolder connectionHolder;

   private Socket socket;
   private InputStream inputStream;
   private OutputStream outputStream;
   
   private Hashtable<String, ObjectHolder> mMonitorObject = new Hashtable<String, ObjectHolder>();
   private Hashtable<String, MonitoredObject> mRootList;

   private Thread mThread;
   private Boolean mCanRun = true;

   private MonitoredObject.WizeUpdater mWizeUpdater = new MonitoredObject.WizeUpdater();
   
   private int refreshRate = 50; // pms

   private final VarData.Builder varDataBuilder = VarData.newBuilder();
   private final DataPacketProto.FrameData.Type.Builder typeBuilder = DataPacketProto.FrameData.Type.newBuilder();
   private final DataPacketProto.FrameData.Value.Builder valueBuilder = DataPacketProto.FrameData.Value.newBuilder();

   @Autowired
   private ClientConnectionHolder clientConnectionHolder;

   public ClientData(IConnectionHolder connectionHolder, Socket iClient, Hashtable<String, MonitoredObject> iRootList) throws IOException {
      LOGGER.info(iClient.getInetAddress().getHostName() +  " : Initialing connection");

      this.connectionHolder = connectionHolder;

      socket = iClient;
      mRootList = iRootList;

      inputStream = socket.getInputStream();
      outputStream = socket.getOutputStream();

      mThread = new Thread(this);
      mThread.start();

      clientConnectionHolder.registerClient(this);

      LOGGER.info(iClient.getInetAddress().getHostName() + " : connection initialized.");

      for (MonitoredObject wObj : mRootList.values()) {
         UpdateObject(wObj);
      }


   }

   //>ObjectData:Path:[content]:Type:[content]:Value:[content]:Visibility:[content]
   @Override
   public void triggerUpdate() {

      mWriterMutex.lock();
      mMonitorListMutex.lock();
      for (ObjectHolder wHolder : mMonitorObject.values()) {
         wHolder.getObjectSpy().registerForUpdate(mWizeUpdater);
      }
      
      mWizeUpdater.executeUpdate();
      for (ObjectHolder wHolder : mMonitorObject.values()) {
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
      mMonitorListMutex.unlock();
      mWriterMutex.unlock();
   }
   private void UpdateObject(MonitoredObject wObj) {
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

   //>ObjectRequest:PATH:[content]
   @Override
   public void run() {
      while(mCanRun)
      {
         try
         {
            if(inputStream.ready()) {
               String wLine = inputStream.readLine();
               if(wLine.startsWith(">ObjectRequest:") && wLine.toUpperCase().indexOf(cPath) != -1 )
               {
                  String wPath = wLine.substring(wLine.toUpperCase().indexOf(cPath)+cPath.length());
                  
                  MonitoredObject wObj = MonitoredObject.ObjectNavigation(mRootList, wPath, null);
                  if(wObj != null)
                  {
                     UpdateObject(wObj);
                     for (String wField : wObj.mFieldNames) {
                        MonitoredObject wTemp = MonitoredObject.ObjectNavigation(wObj.mChildren, wField, wObj);
                        if(wTemp != null)
                        UpdateObject(wTemp);
                     }
                  }
               }
               else if(wLine.startsWith(">RMonitorObject:") && wLine.toUpperCase().indexOf(cPath) != -1) {
                  String wPath = wLine.substring(wLine.toUpperCase().indexOf(cPath)+cPath.length());
                  mMonitorListMutex.lock();
                  if(!mMonitorObject.containsKey(wPath)) {
                    MonitoredObject wObj = MonitoredObject.ObjectNavigation(mRootList, wPath, null);
                    if(wObj != null) {
                       mMonitorObject.put(wPath, new ObjectHolder(wObj));
                    }
                  }
                  mMonitorListMutex.unlock();
               }
               else if(wLine.startsWith(">URMonitorObject:") && wLine.toUpperCase().indexOf(cPath) != -1) {
                  String wPath = wLine.substring(wLine.toUpperCase().indexOf(cPath)+cPath.length());
                  mMonitorListMutex.lock();
                  if(mMonitorObject.containsKey(wPath))
                     mMonitorObject.remove(wPath);
                  mMonitorListMutex.unlock();
               }
               UpdateClient();
            }
            else {
               Thread.sleep(refreshRate);
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
         if(mWriter != null)
            mWriter.close();
      } catch (IOException e) { }

      if(connectionHolder != null) connectionHolder.unRegisterClient(this);

      LOGGER.info(socket.getInetAddress().getHostName() + " : disconnected.");
   }

   public void readFrameData() throws IOException {
      DataPacketProto.FrameData frameData = FrameData.parseFrom(inputStream);
      for(FrameData.RequestData requestData : frameData.getRequestedDataList()) {
         switch (requestData.getMode()) {
            case QUERY:

               break;
            case REGISTER:

               break;
            case UNREGISTER:

               break;
         }
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
      clientConnectionHolder.unRegisterClient(this);
   }
}
