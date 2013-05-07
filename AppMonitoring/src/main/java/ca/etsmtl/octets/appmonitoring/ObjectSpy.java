package ca.etsmtl.octets.appmonitoring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.*;

public class ObjectSpy {
   protected Class<?> mClass = null;
   protected Object mObject = null;
   
   protected final Hashtable<String, Field> mFields = new Hashtable<String, Field>();
   
   protected final Hashtable<String, ObjectSpy> mChildren = new Hashtable<String, ObjectSpy>();
   
   protected Vector<String> mFieldNames = new Vector<String>();
   
   protected String mPath;
   protected String mName;
   
   protected ObjectSpy mParent;
   
   public ObjectSpy(Object iWatched, String iName, String iPath, String iVisibility, ObjectSpy iParent)
   {
      mParent = iParent;
      mName = iName;
      mPath = iPath;
      mClass = iWatched.getClass();
      
      mObject = iWatched;
      
      List<Field> wFieldList = gatherFields(mClass);
      {
         int itt = 0;
         for (Field field : wFieldList) {
            mFields.put(field.getName(), field);
            mFieldNames.add(field.getName());
         }  
      }
   }
   
   public String getTypeName() {
      return mClass.getName();
   }
   
   public Object getFieldObject(String iName)
   {
      try {
         mFields.get(iName).setAccessible(true);
         return mFields.get(iName).get(mObject);
      } catch (NullPointerException e) {
         System.out.println(iName + " invalid var");
      }
      catch (IllegalArgumentException e) {
         //e.printStackTrace();
         System.out.println(iName + " invalid var");
      } catch (IllegalAccessException e) {
         //e.printStackTrace();
         System.out.println(iName + " invalid var");
      }
      return null;
   }
   public List<String> getFieldsName() {
      return mFieldNames;
   }
   
   @Override
   public String toString() {
      String wReturn = "";
      try {
         wReturn = mObject.toString();
      }
      catch (ConcurrentModificationException e) {
         
      }
      return wReturn;
   }
   
   private List<Field> gatherFields(Class<?> input)
   {
      List<Field> wFields = new Vector<Field>();
      if(input.getSuperclass() != null) {
         wFields.addAll(gatherFields(input.getSuperclass()));
      }
      for (Field field : input.getDeclaredFields()) {
         wFields.add(field);
      }
      
      return wFields;
   }
   
   public void updateFromParent() {
      if(mParent != null) {
         mObject = mParent.getFieldObject(mName);
      }
   }
   
   public void registerForUpdate(WizeUpdater iUpdater) {
      if(mParent != null)
         mParent.registerForUpdate(iUpdater);
      iUpdater.requestUpdate(this);
   }
   
   public static ObjectSpy SpyNavigation(Hashtable<String, ObjectSpy> iDict, String iPath, ObjectSpy iSender) {
      String[] wList = iPath.split("\\.");
      if(iPath.isEmpty() || iPath == null)
         throw new InvalidParameterException("Path invalid");
      
      if(wList.length >1) {
         String wNextPath = iPath.substring(wList[0].length() +1);
         
         ObjectSpy wTemp = SpyNavigation(iDict, wList[0],iSender);
         return SpyNavigation(wTemp.mChildren, wNextPath,wTemp);
      }
      else if(iDict.containsKey(iPath)) {
         return iDict.get(iPath);
      }
      else if(iSender != null && iSender.mFieldNames.contains(iPath)) {
         String wPath = null;
         
         if(iSender.mPath.isEmpty())
            wPath = iSender.mName;
         else
            wPath = iSender.mPath + "." + iSender.mName;
            
         Object wObject = iSender.getFieldObject(iPath);
         if(wObject != null) {
            
            String wModif = "NONE";
            if(Modifier.isPrivate(iSender.mFields.get(iPath).getModifiers()))
               wModif = "PRIVATE";
            else if(Modifier.isPublic(iSender.mFields.get(iPath).getModifiers()))
               wModif = "PUBLIC";
            else if(Modifier.isProtected(iSender.mFields.get(iPath).getModifiers()))
               wModif = "PROTECTED";
            
            ObjectSpy wTemp = new ObjectSpy(iSender.getFieldObject(iPath),iPath,wPath, wModif,iSender);
            iSender.mChildren.put(iPath, wTemp);
            return wTemp;
         }
      }
      
      return null;
   }
   
   public static class WizeUpdater {
      
      public List<ObjectSpy> mObjectList = new Vector<ObjectSpy>();
      
      private final static ObjectSorter mSorter = new ObjectSorter();
      
      void requestUpdate(ObjectSpy iObject) {
         if(!mObjectList.contains(iObject)) {
            mObjectList.add(iObject);
         }
      }
      
      /**
       * Execute an update to refresh the intern object of the spy.
       */
      public void executeUpdate() {
         Collections.sort(mObjectList, mSorter);
         for(int i = 0; i < mObjectList.size(); ++i) {
            mObjectList.get(i).updateFromParent();
         }
         mObjectList.clear();
      }
      
      private static class ObjectSorter implements Comparator<ObjectSpy> {

         public ObjectSorter() {
         }
         
         @Override
         public int compare(ObjectSpy o1, ObjectSpy o2) {
            return o1.mPath.compareTo(o2.mPath);
         }
         
      }
   }
}
