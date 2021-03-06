package ca.etsmtl.octets.appmonitoring;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public final class ClientManager implements Runnable {
   private static final Logger logger = Logger.getLogger(ClientManager.class);
   public final static int SERVER_PORT = 12012;
   private final static int WAIT_TIME = 500;

   private final IConnectionHolder connectionHolder = new ClientsHolder();

   private final Hashtable<String, MonitoredObject> mToBeWatch = new Hashtable<>();

   private Boolean mCanRun = false;
   private ServerSocket mServerSocket = null;

   public ClientManager() throws IOException {
      mServerSocket = new ServerSocket(SERVER_PORT);
   }

   @Override
   public void run() {
      mCanRun = true;
      logger.info("Initializing server socket.");
      while(mCanRun) {
         try {
            mServerSocket.setSoTimeout(WAIT_TIME);

            Socket wClient = mServerSocket.accept();
            
            ClientData client = new ClientData(connectionHolder,wClient, mToBeWatch);
            connectionHolder.registerClient(client);

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
      mToBeWatch.put(name, new MonitoredObject(object,name,"",null,null));
   }

   public boolean containsName(String name) {
      return mToBeWatch.containsKey(name);
   }
   
   public void close() {
      mCanRun = false;
      connectionHolder.closeConnections();
   }
}
