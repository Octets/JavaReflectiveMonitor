package ca.etsmtl.octets.appmonitoring;

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReentrantLock;

class ObjectHolder {

   private final ReentrantLock valueLock = new ReentrantLock();
   private final ReentrantLock errorLock = new ReentrantLock();

   private String mLastValue = "";
   private Boolean mRequestUpdate;
   
   private MonitoredObject mSource;

   private boolean containError;
   
   public ObjectHolder(MonitoredObject iSource) {
      mSource = iSource;
      errorLock.lock();
      containError = false;
      errorLock.unlock();
      try
      {
         valueLock.lock();
         mLastValue = mSource.getStringValue();
         valueLock.unlock();
      }
      catch(ConcurrentModificationException e) {
         mLastValue = null;
         errorLock.lock();
         containError = true;
         errorLock.unlock();
      }
      mRequestUpdate = true;
   }
   
   public Boolean CheckForUpdate() {
      try
      {
         valueLock.lock();
         String stringValue = mSource.getStringValue();
         valueLock.unlock();

         mRequestUpdate = mRequestUpdate | ( stringValue != null && !stringValue.equals(mLastValue) );
         if(mRequestUpdate)
            mLastValue = stringValue;
         errorLock.lock();
         containError = false;
         errorLock.unlock();
      }
      catch(ConcurrentModificationException e) {
         errorLock.lock();
         containError = true;
         errorLock.unlock();
      }
      
      return mRequestUpdate;
   }
   
   public void setRequestUpdate(Boolean iStates) {
      mRequestUpdate = iStates;
   }
   
   public String getPath() {
      return mSource.mPath;
   }
   public String getName() {
      return mSource.mName;
   }
   public String getType() {
      return mSource.getTypeName();
   }
   public MonitoredObject getObjectSpy() {
      return mSource;
   }

   public String getStringValue() {
      valueLock.lock();
      String value = mLastValue;
      valueLock.unlock();

      return value;
   }

   public boolean isContainError() {
      errorLock.lock();
      boolean value = containError;
      errorLock.unlock();

      return value;
   }

}
