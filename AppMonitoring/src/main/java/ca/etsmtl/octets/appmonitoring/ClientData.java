package ca.etsmtl.octets.appmonitoring;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

class ClientData implements Runnable, IClientConnection {
   private static final Logger LOGGER = Logger.getLogger(ClientData.class);

   private IConnectionHolder connectionHolder;

   private Socket mClient;

   private BufferedReader mReader;
   private OutputStream mWriter;
   
   private Hashtable<String, ObjectHolder> mMonitorObject = new Hashtable<String, ObjectHolder>();
   private Hashtable<String, MonitoredObject> mRootList;

   private Thread mThread;
   private Boolean mCanRun = true;

   private ReentrantLock mWriterMutex = new ReentrantLock();
   private ReentrantLock mMonitorListMutex = new ReentrantLock();

   private MonitoredObject.WizeUpdater mWizeUpdater = new MonitoredObject.WizeUpdater();
   
   private int refreshRate = 50; // pms

   private final DataPacketProto.FrameData.VarData.Builder varDataBuilder = DataPacketProto.FrameData.VarData.newBuilder();
   private final DataPacketProto.FrameData.Type.Builder typeBuilder = DataPacketProto.FrameData.Type.newBuilder();
   private final DataPacketProto.FrameData.Value.Builder valueBuilder = DataPacketProto.FrameData.Value.newBuilder();

   public ClientData(IConnectionHolder connectionHolder, Socket iClient, Hashtable<String, MonitoredObject> iRootList) throws IOException {
      LOGGER.info(iClient.getInetAddress().getHostName() +  " : Initialing connection");

      this.connectionHolder = connectionHolder;

      mClient = iClient;
      mRootList = iRootList;

      mReader = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
      mWriter = mClient.getOutputStream();

      mThread = new Thread(this);
      mThread.start();


      mUpdater = new Thread(new Runnable() {
         @Override
         public void run() {
            while(mCanRun) {
               UpdateClient();
               try {
                  Thread.sleep(refreshRate);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }
      });
      mUpdater.setName(iClient.getInetAddress().getHostName() + " object monitor.");
      mUpdater.start();

      LOGGER.info(iClient.getInetAddress().getHostName() + " : connection initialized.");

      for (MonitoredObject wObj : mRootList.values()) {
         UpdateObject(wObj);
      }


   }

   //>ObjectData:Path:[content]:Type:[content]:Value:[content]:Visibility:[content]
   public void UpdateClient() {

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

               varDataBuilder.build().writeTo(mWriter);

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
         mWriter.flush();   
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
            if(mReader.ready()) {
               String wLine = mReader.readLine();
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
         if(mReader != null)
            mReader.close();
         if(mWriter != null)
            mWriter.close();
      } catch (IOException e) { }

      if(connectionHolder != null) connectionHolder.unRegisterClient(this);

      LOGGER.info(mClient.getInetAddress().getHostName() + " : disconnected.");
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
   }
}
