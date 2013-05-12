package ca.etsmtl.octets.appmonitoring;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ClientConnectionHolder implements Runnable {
   private final static Logger logger = Logger.getLogger(ClientConnectionHolder.class);
   private final static int NUMBER_OF_SAMPLE = 20;

   private final List<IClientConnection> connectionList = new ArrayList<IClientConnection>();
   private final Deque<Long> updateAverage = new ConcurrentLinkedDeque<Long>();

   private boolean running = false;

   StopWatch stopWatch = new StopWatch();

   public ClientConnectionHolder() {

   }

   void registerClient(IClientConnection clientConnection) {
      if(!connectionList.contains(clientConnection))
         connectionList.add(clientConnection);
   }

   void unRegisterClient(IClientConnection clientConnection) {
      if(connectionList.contains(clientConnection))
         connectionList.remove(clientConnection);
   }


   @Override
   public void run() {
      running = true;
      while(running) {
         stopWatch.start();
         for(IClientConnection clientConnection : connectionList) {
            clientConnection.triggerUpdate();
         }
         stopWatch.stop();
         updateAverage.add(stopWatch.getNanoTime());
         stopWatch.reset();

         //Every 2s frame
         if(Calendar.getInstance().getTimeInMillis() % 2000 == 0) {
            logger.debug("Average update time : " + computeAverage() + " ms");
         }
      }
   }

   private long computeAverage() {
      Long[] valueList = (Long[])updateAverage.toArray();
      Long sums = 0L;
      for(Long value : valueList) {
         sums += value;
      }

      return sums / valueList.length;
   }

   public void close() {
      running = false;
   }
}
