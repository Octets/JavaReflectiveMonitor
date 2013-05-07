package ca.etsmtl.octets.appmonitoring;

import java.util.ConcurrentModificationException;

public class ObjectHolder {
   public static final String XMLNAME = "ObjectData";
   
   //private Hashtable<String, ObjectHolder> mRegisteredTable = new Hashtable<String, ObjectHolder>();

   private String mVisibility;
   
   private String mLastValue = "";
   private Boolean mRequestUpdate;
   
   private ObjectSpy mSource;
   
   public ObjectHolder(ObjectSpy iSource) {
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
   public String getVisibility() {
      return mVisibility;
   }
   public String getType() {
      return mSource.getTypeName();
   }
   public ObjectSpy getObjectSpy() {
      return mSource;
   }
   
   @Override
   public String toString() {
      return mLastValue;
   }
}
