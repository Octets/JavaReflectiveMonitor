package ca.etsmtl.octets.visualmonitor;

import com.sun.javafx.tk.Toolkit;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData;
import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarData;
import static ca.etsmtl.octets.visualmonitor.Connector.States.CLOSING;
import static ca.etsmtl.octets.visualmonitor.Connector.States.CONNECTED;
import static ca.etsmtl.octets.visualmonitor.Connector.States.DISCONNECTED;

public class Connector implements Runnable {

   private static final Logger logger = Logger.getLogger(Connector.class);
   public final List<IListenFrame> listenerList = new Vector<>();
   private DataInputStream dataInputStream;
   private OutputStream outputStream;
   private Socket socket;
   private States states = DISCONNECTED;
   private long updateSpeed = 1000; //ms
   private Runnable postDisconnected = null;

   public void connect(String hostname, int port,Controller fxController) throws IOException {
      logger.debug("Initializing connection with " + hostname + " on port " + port);
      socket = new Socket(hostname,port);
      dataInputStream = new DataInputStream(socket.getInputStream());

      outputStream = socket.getOutputStream();
      logger.debug("Connection initialized.");

      if(socket.isConnected()) {
         new Thread(this).start();
      }
      states = CONNECTED;

      listenerList.add(fxController.getDisplayMapper());
      fxController.updateTableLayout();
   }

   @Override
   public void run() {
      long remainingTime;
      StopWatch stopWatch = new StopWatch();
      try {
         while(socket.isConnected() && states == CONNECTED) {
            stopWatch.start();
            readFromClient();
            writeToClient();
            stopWatch.stop();

            remainingTime = updateSpeed - stopWatch.getTime();
            stopWatch.reset();

            if(remainingTime > 0) {
               try {
                  Thread.sleep(remainingTime);
               } catch (InterruptedException e) {
                  logger.warn("Interrupted",e);
               }
            }
         }
      } catch (Exception e) {
         logger.error("Shit appended",e);
      } finally {
         closing();
      }
   }

   private void writeToClient() {

   }

   public void readFromClient() throws IOException {
      if(dataInputStream.available() > 0) {
         byte[] toRead = new byte[dataInputStream.readInt()];
         int read = dataInputStream.read(toRead);
         if(read != -1) {
            FrameData frameData = FrameData.parseFrom(toRead);

            for(VarData varData : frameData.getVarDataList()) {
               for (IListenFrame listenFrame : listenerList) {
                  listenFrame.onFrameReaded(varData);
               }
            }
         }
      }
   }

   public long getUpdateSpeed() {
      return updateSpeed;
   }

   public void setUpdateSpeed(long updateSpeed) {
      this.updateSpeed = updateSpeed;
   }

   public States getStates() {
      return states;
   }

   private void closing() {
      try {
         dataInputStream.close();
      } catch (IOException e) {
         logger.error("Fail at closing",e);
      }
      try {
         outputStream.close();
      } catch (IOException e) {
         logger.error("Fail at closing",e);
      }
      try {
         socket.close();
      } catch (IOException e) {
         logger.error("Fail at closing",e);
      }
      logger.debug("Disconnected from " + socket.getInetAddress().getHostAddress());

      socket = null;
      states = DISCONNECTED;

      if(postDisconnected != null) {
         Toolkit.getToolkit().defer(postDisconnected);
      }
   }

   public void disconnect(Runnable postDisconnected) {
      this.postDisconnected = postDisconnected;
      states = CLOSING;
   }

   public static enum States {
      CONNECTED, CLOSING, DISCONNECTED
   }

   public interface IListenFrame {
      void onFrameReaded(VarData varData);
   }
}
