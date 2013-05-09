package ca.etsmtl.octets.appmonitoring;

import org.apache.log4j.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

public class ClientConnection implements Runnable {
   final String cPath = "PATH:";
   
   String mName;
   
   Socket mClient;
   
   BufferedReader mReader;
   BufferedWriter mWriter;
   
   Hashtable<String, ObjectHolder> mMonitorObject = new Hashtable<String, ObjectHolder>();
   
   Hashtable<String, ObjectSpy> mRootList;
   
   Thread mThread;
   Thread mUpdater;
   Boolean mCanRun = true;
   
   ReentrantLock mWriterMutex = new ReentrantLock();
   ReentrantLock mMonitorListMutex = new ReentrantLock();
   
   ObjectSpy.WizeUpdater mWizeUpdater = new ObjectSpy.WizeUpdater();
   
   final int mWaitTime = 50; // pms
   
   Logger log = Logger.getLogger(getClass());
   
   public ClientConnection(Socket iClient, Hashtable<String, ObjectSpy> iRootList) throws IOException {
      mClient = iClient;
      
      mRootList = iRootList;
      mName = iClient.getInetAddress().getHostAddress() +":"+ iClient.getPort();
      
      mReader = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
      mWriter = new BufferedWriter(new OutputStreamWriter(mClient.getOutputStream()));
      
      mThread = new Thread(this);
      mThread.start();
      
      mUpdater = new Thread(new Runnable() {
         @Override
         public void run() {
            while(mCanRun) {
               UpdateClient();
               try {
                  Thread.sleep(mWaitTime);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }
      });
      mUpdater.setName(mName + " object monitor.");
      mUpdater.start();
      
      log.info(mName + " - is connected to remote Network Monitor");
      for (ObjectSpy wObj : mRootList.values()) {
         UpdateObject(wObj);
      }
   }

   //>ObjectData:Path:[content]:Type:[content]:Value:[content]:Visibility:[content]:
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
               
               String wData = ">ObjectData:Path:" + wPath;
               
               wData += ":Type:" + wHolder.getType();
               wData += ":Value:"+ URLEncoder.encode(wHolder.toString(), "UTF-8");
               wData += ":Visibility:" + wHolder.getVisibility();
               
               mWriter.write(wData);
               mWriter.newLine();
               mWriter.flush();
               wHolder.setRequestUpdate(false);  
            }
         }
         catch(ConcurrentModificationException e) {
         }
         catch(IOException e){
            close();
         }
      }
      mMonitorListMutex.unlock();
      mWriterMutex.unlock();
   }
   private void UpdateObject(ObjectSpy wObj) {
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
         mWriter.write(wData);
         mWriter.newLine();
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
                  
                  ObjectSpy wObj = ObjectSpy.SpyNavigation(mRootList, wPath, null);
                  if(wObj != null)
                  {
                     UpdateObject(wObj);
                     for (String wField : wObj.mFieldNames) {
                        ObjectSpy wTemp = ObjectSpy.SpyNavigation(wObj.mChildren, wField, wObj);
                        if(wTemp != null)
                        UpdateObject(wTemp);
                     }
                  }
               }
               else if(wLine.startsWith(">RMonitorObject:") && wLine.toUpperCase().indexOf(cPath) != -1) {
                  String wPath = wLine.substring(wLine.toUpperCase().indexOf(cPath)+cPath.length());
                  mMonitorListMutex.lock();
                  if(!mMonitorObject.containsKey(wPath)) {
                    ObjectSpy wObj = ObjectSpy.SpyNavigation(mRootList, wPath, null);
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
               Thread.sleep(mWaitTime);
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
      } catch (IOException e) {
      }
      log.info(mName + " - disconnected from remote Network Monitor.");
   }

   public void close() {
      mCanRun = false;
   }
}
