package ca.etsmtl.octets.appmonitoring;

interface IClientConnection {

   void triggerUpdate();


   int getRefreshRate();
   void setRefreshRate(int refreshRate);

   void close();
}
