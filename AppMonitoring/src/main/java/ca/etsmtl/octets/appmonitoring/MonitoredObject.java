package ca.etsmtl.octets.appmonitoring;

import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.*;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarScope;

class MonitoredObject {
   private final static Logger LOGGER = Logger.getLogger(MonitoredObject.class);

   protected Class<?> mClass = null;
   protected Object mObject = null;
   
   protected final Hashtable<String, Field> mFields = new Hashtable<String, Field>();
   
   protected final Hashtable<String, MonitoredObject> mChildren = new Hashtable<String, MonitoredObject>();
   
   protected Vector<String> mFieldNames = new Vector<String>();
   
   protected String mPath;
   protected String mName;
   
   protected MonitoredObject mParent;
   
   public MonitoredObject(Object iWatched, String iName, String iPath, String iVisibility, MonitoredObject iParent)
   {
      mParent = iParent;
      mName = iName;
      mPath = iPath;
      mClass = iWatched.getClass();
      
      mObject = iWatched;
      
      List<Field> wFieldList = listFields(mClass);
      {
         for (Field field : wFieldList) {
            mFields.put(field.getName(), field);
            mFieldNames.add(field.getName());
         }  
      }
      VarScope[] varScope = getVarScope(mClass.getModifiers());
   }

   private VarScope[] getVarScope(int modifier) {
      List<VarScope> varScopeList = new ArrayList<VarScope>();
      if(Modifier.isPublic(modifier)) {
         varScopeList.add(VarScope.PUBLIC);
      } if(Modifier.isProtected(modifier)){
         varScopeList.add(VarScope.PROTECTED);
      } if(Modifier.isPrivate(modifier)) {
         varScopeList.add(VarScope.PRIVATE);
      } if (Modifier.isAbstract(modifier)) {

      } if (Modifier.isFinal(modifier)) {

      } if( Modifier.isInterface(modifier)) {

      } if( Modifier.isStatic(modifier)) {

      } if( Modifier.isNative(modifier)) {

      } if( Modifier.isSynchronized(modifier)) {

      } if( Modifier.isStrict(modifier)) {

      } if( Modifier.isVolatile(modifier)) {

      } if( Modifier.isTransient(modifier)) {

      }
      VarScope[] varScopes = new VarScope[varScopeList.size()];
      return varScopeList.toArray(varScopes);
   }
   
   public String getTypeName() {
      return mClass.getName();
   }

   public String getStringValue() {
      String wReturn = "";
      try {
         wReturn = mObject.toString();
      }
      catch (ConcurrentModificationException e) {
         LOGGER.debug("ConcurrentModificationException",e);
      }
      return wReturn;
   }
   
   public Object getFieldObject(String fieldName)
   {
      try {
         Field field = mFields.get(fieldName);
         field.setAccessible(true);
         return field.get(mObject);
      } catch (NullPointerException e) {
         LOGGER.debug(fieldName + " invalid pointer",e);
      } catch (IllegalArgumentException e) {
         LOGGER.debug(fieldName + " invalid argument", e);
      } catch (IllegalAccessException e) {
         LOGGER.debug(fieldName + " invalid access", e);
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
   
   private List<Field> listFields(Class<?> input)
   {
      List<Field> wFields = new ArrayList<Field>();
      if(input.getSuperclass() != null && input.equals(Object.class)) {
         wFields.addAll(listFields(input.getSuperclass()));
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
   
   public static MonitoredObject ObjectNavigation(Hashtable<String, MonitoredObject> iDict, String iPath, MonitoredObject iSender) {
      String[] wList = iPath.split("\\.");
      if(iPath.isEmpty())
         throw new InvalidParameterException("Path invalid");
      
      if(wList.length >1) {
         String wNextPath = iPath.substring(wList[0].length() +1);
         
         MonitoredObject wTemp = ObjectNavigation(iDict, wList[0], iSender);
         return ObjectNavigation(wTemp.mChildren, wNextPath, wTemp);
      }
      else if(iDict.containsKey(iPath)) {
         return iDict.get(iPath);
      }
      else if(iSender != null && iSender.mFieldNames.contains(iPath)) {
         String wPath;
         
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
            
            MonitoredObject wTemp = new MonitoredObject(iSender.getFieldObject(iPath),iPath,wPath, wModif,iSender);
            iSender.mChildren.put(iPath, wTemp);
            return wTemp;
         }
      }
      
      return null;
   }
   
   public static class WizeUpdater {
      
      public List<MonitoredObject> mObjectList = new Vector<MonitoredObject>();
      
      private final static Comparator<MonitoredObject> mSorter = new Comparator<MonitoredObject>() {
         @Override
         public int compare(MonitoredObject o1, MonitoredObject o2) {
            return o1.mPath.compareTo(o2.mPath);
         }
      };
      
      void requestUpdate(MonitoredObject iObject) {
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
   }
}
