package com.bitwig.extensions.framework.time;

public abstract class AbstractTimedEvent implements TimedEvent {
   protected final long startTime;
   protected boolean completed;
   protected final long delayTime;

   public AbstractTimedEvent(final long delayTime) {
      startTime = System.currentTimeMillis();
      completed = false;
      this.delayTime = delayTime;
   }

   public void cancel() {
      completed = true;
   }

   public boolean isCompleted() {
      return completed;
   }

}
