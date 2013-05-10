package ca.etsmtl.octets.appmonitoring;

interface IClientConnection {

   int getRefreshRate();
   void setRefreshRate(int refreshRate);

   void close();
}
