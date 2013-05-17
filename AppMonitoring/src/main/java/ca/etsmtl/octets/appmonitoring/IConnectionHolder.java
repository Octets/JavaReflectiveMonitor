package ca.etsmtl.octets.appmonitoring;

interface IConnectionHolder {
   void registerClient(IClientConnection remoteClient);
   void unRegisterClient(IClientConnection remoteClient);

   void closeConnections();
}
