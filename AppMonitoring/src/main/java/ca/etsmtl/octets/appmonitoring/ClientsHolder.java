package ca.etsmtl.octets.appmonitoring;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;

import java.util.*;

class ClientsHolder implements Runnable, IConnectionHolder {
   private final static Logger logger = Logger.getLogger(ClientsHolder.class);
   private final static int NUMBER_OF_SAMPLE = 20;

   private final List<IClientConnection> connectionList = new ArrayList<IClientConnection>();
   private final Deque<Long> updateAverage = new ArrayDeque<Long>();

   private long executionTime = 1000; //ms

   private boolean running = false;

   StopWatch stopWatch = new StopWatch();

   public ClientsHolder() {

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
   public long getExecutionTime() {
      return executionTime;
   }

   @Override
   public void setExecutionTime(long time) {
      executionTime = time;
   }

   @Override
   public void run(){
      running = true;
      while(running) {
         stopWatch.start();
         for(IClientConnection clientConnection : connectionList) {
            clientConnection.triggerUpdate();
         }
         stopWatch.stop();
         long runTime = stopWatch.getTime();
         updateAverage.add(runTime);
         stopWatch.reset();

         long remainingTime = executionTime - runTime;

         if(remainingTime > 0) {
            try {
               Thread.sleep(remainingTime);
            } catch (InterruptedException e) {
               logger.warn("Interrupted", e);
            }
         }

         if(updateAverage.size() >= NUMBER_OF_SAMPLE) {
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

      updateAverage.clear();
      return sums / valueList.length;
   }

   @Override
   public void closeConnections() {
      running = false;
   }
}
