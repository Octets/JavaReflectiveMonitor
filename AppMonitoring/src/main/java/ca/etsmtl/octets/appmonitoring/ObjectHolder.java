package ca.etsmtl.octets.appmonitoring;

import java.util.ConcurrentModificationException;

class ObjectHolder {

   private String mLastValue = "";
   private Boolean mRequestUpdate;
   
   private MonitoredObject mSource;
   
   public ObjectHolder(MonitoredObject iSource) {
      mSource = iSource;
      try
      {
         mLastValue = mSource.toString();   
      }
      catch(ConcurrentModificationException e) {
         mLastValue = "";
      }
      mRequestUpdate = true;
   }
   
   public Boolean CheckForUpdate() {
      try
      {
         mRequestUpdate = mRequestUpdate | !mLastValue.equals(mSource.toString());
         if(mRequestUpdate)
            mLastValue = mSource.toString();   
      }
      catch(ConcurrentModificationException e) {
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
   
   @Override
   public String toString() {
      return mLastValue;
   }
}
