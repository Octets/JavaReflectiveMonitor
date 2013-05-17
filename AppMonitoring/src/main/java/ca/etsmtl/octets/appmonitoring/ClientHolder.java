package ca.etsmtl.octets.appmonitoring;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

class ClientHolder implements Runnable, IConnectionHolder {
   private final static Logger logger = Logger.getLogger(ClientHolder.class);
   private final static int NUMBER_OF_SAMPLE = 20;

   private final List<IClientConnection> connectionList = new ArrayList<IClientConnection>();
   private final Deque<Long> updateAverage = new ConcurrentLinkedDeque<Long>();

   private boolean running = false;

   StopWatch stopWatch = new StopWatch();

   public ClientHolder() {

   }

   @Override
   public void registerClient(IClientConnection remoteClient) {
      if(!connectionList.contains(remoteClient))
         connectionList.add(remoteClient);
   }

   @Override
   public void unRegisterClient(IClientConnection remoteClient) {
      if(connectionList.contains(remoteClient))
         connectionList.remove(remoteClient);
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

   @Override
   public void closeConnections() {
      running = false;
   }
}
