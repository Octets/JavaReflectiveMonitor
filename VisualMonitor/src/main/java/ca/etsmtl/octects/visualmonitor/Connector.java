package ca.etsmtl.octects.visualmonitor;

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

public class Connector implements Runnable {

   private static final Logger logger = Logger.getLogger(Connector.class);

   private DataPacketProto.FrameData frameData;

   private BufferedInputStream bufferedInputStream;
   private OutputStream outputStream;
   private Socket socket;

   private Boolean running = false;

   private long updateSpeed = 1000; //ms

   public void connect(String hostname, int port) throws UnknownHostException, IOException {
      socket = new Socket(hostname,port);
      bufferedInputStream = new BufferedInputStream(socket.getInputStream());

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
         frameData = DataPacketProto.FrameData.parseFrom(bufferedInputStream);
         Collections.sort(frameData.getVarDataList(),new Comparator<DataPacketProto.FrameData.VarData>() {
            @Override
            public int compare(DataPacketProto.FrameData.VarData o1, DataPacketProto.FrameData.VarData o2) {
               return (int)(o2.getDate() - o1.getDate());
            }
         });

      }
   }

   public long getUpdateSpeed() {
      return updateSpeed;
   }

   public void setUpdateSpeed(long updateSpeed) {
      this.updateSpeed = updateSpeed;
   }

   private void close() {
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
      void OnFrameReaded(DataPacketProto.FrameData.VarData varData);
   }
}
