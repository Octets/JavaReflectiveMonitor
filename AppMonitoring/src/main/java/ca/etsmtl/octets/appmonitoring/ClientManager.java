package ca.etsmtl.octets.appmonitoring;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class ClientManager implements Runnable, IConnectionHolder {
   public final static int SERVER_PORT = 12012;
   public final static int WAIT_TIME = 500;

   private final Hashtable<String, MonitoredObject> mToBeWatch = new Hashtable<String, MonitoredObject>();

   private final List<IClientConnection> connectedClients = new ArrayList<IClientConnection>();
   
   private Boolean mCanRun = false;
   private ServerSocket mServerSocket = null;
   
   public ClientManager() throws IOException {
      mServerSocket = new ServerSocket(SERVER_PORT);
   }

   @Override
   public void run() {
      mCanRun = true;
      while(mCanRun) {
         try {
            mServerSocket.setSoTimeout(WAIT_TIME);
            Socket wClient = mServerSocket.accept();
            
            ClientData client = new ClientData(this, wClient, mToBeWatch);
            connectedClients.add(client);
         } catch (SocketTimeoutException e) {
            try {
               Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e1) {
               e1.printStackTrace();
            }
         } catch (IOException e) {
             mCanRun = false;
         }
         
      }
   }

   public void registerObject(String name, Object object) {
      mToBeWatch.put(name, new MonitoredObject(object,name,"","NONE",null));
   }

   public boolean containsName(String name) {
      return mToBeWatch.containsKey(name);
   }
   
   public void close() {
      mCanRun = false;
      for(IClientConnection c : connectedClients){
    	  c.close();
      }
   }

   public void unRegisterClient(IClientConnection remoteClient) {
      if(connectedClients.contains(remoteClient))
         connectedClients.remove(remoteClient);
   }
}
