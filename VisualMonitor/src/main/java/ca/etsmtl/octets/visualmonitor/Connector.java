package ca.etsmtl.octets.visualmonitor;

import ca.etsmtl.octets.appmonitoring.DataPacketProto;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData;
import static ca.etsmtl.octets.appmonitoring.DataPacketProto.FrameData.VarData;

public class Connector implements Runnable {
   private static final Logger logger = Logger.getLogger(Connector.class);

   public final List<IListenFrame> listenerList = new Vector<>();

   private BufferedInputStream bufferedInputStream;
   private OutputStream outputStream;
   private Socket socket;

   private Boolean running = false;

   private long updateSpeed = 1000; //ms

   public void connect(String hostname, int port) throws IOException {
      logger.debug("Initializing connection with " + hostname + " on port " + port);
      socket = new Socket(hostname,port);
      bufferedInputStream = new BufferedInputStream(socket.getInputStream());
      logger.debug("Connection initialized.");

      if(socket.isConnected()) {
         new Thread(this).run();
      }
   }

   @Override
   public void run() {
      long remainingTime;
      running = true;
      StopWatch stopWatch = new StopWatch();
      try {
         while(running) {
            stopWatch.start();
            readFromClient();
            writeToClient();
            stopWatch.stop();

            remainingTime = updateSpeed - stopWatch.getTime();

            if(remainingTime > 0) {
               try {
                  Thread.sleep(remainingTime);
               } catch (InterruptedException e) {
                  logger.warn("Interrupted",e);
               }
            }


         }

      } catch (IOException e) {
         logger.error("Running error",e);
      } finally {
         close();
      }
   }


   private void writeToClient() {

   }

   public void readFromClient() throws IOException {
      if(bufferedInputStream.available() > 0) {
         FrameData frameData = FrameData.parseFrom(bufferedInputStream);
         Collections.sort(frameData.getVarDataList(),new Comparator<VarData>() {
            @Override
            public int compare(VarData o1, VarData o2) {
               return (int)(o2.getDate() - o1.getDate());
            }
         });

         for(VarData varData : frameData.getVarDataList()) {
            for (IListenFrame listenFrame : listenerList) {
               listenFrame.OnFrameReaded(varData);
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

   private void close() {
      running = false;
      try {
         bufferedInputStream.close();
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
   }

   public interface IListenFrame {
      void OnFrameReaded(VarData varData);
   }
}
