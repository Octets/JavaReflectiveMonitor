package ca.etsmtl.octets.appmonitoring;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class ClientManager implements Runnable {
   public final static int SERVER_PORT = 12012;
   public final static int WAITTIME = 400;
      
   public Hashtable<String, ObjectSpy> mToBeWatch = new Hashtable<String, ObjectSpy>();
   
   private final ArrayList<ClientConnection> connectedClients = new ArrayList<ClientConnection>();
   
   //List<ClientConnection> mClients = new Vector<ClientConnection>();
   Boolean mCanRun = false;
   ServerSocket mServerSocket = null;
   
   public ClientManager() throws IOException {
      mServerSocket = new ServerSocket(SERVER_PORT);
   }
   
   @Override
   public void run() {
      mCanRun = true;
      while(mCanRun) {
         try {
            mServerSocket.setSoTimeout(WAITTIME);
            Socket wClient = mServerSocket.accept();
            
            ClientConnection client = new ClientConnection(wClient, mToBeWatch);
            connectedClients.add(client);
         } catch (IOException e) {
            try {
               Thread.sleep(WAITTIME);
            } catch (InterruptedException e1) {
               e1.printStackTrace();
            }
         }
         
      }
   }
   
   public void close() {
      mCanRun = false;
      for(ClientConnection c : connectedClients){
    	  c.close();
      }
   }
}
