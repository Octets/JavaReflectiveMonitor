package ca.etsmtl.octets.appmonitoring;

interface IConnectionHolder {
   void registerClient(IClientConnection remoteClient);
   void unRegisterClient(IClientConnection remoteClient);

   long getExecutionTime();
   void setExecutionTime(long time);

   void closeConnections();
}
