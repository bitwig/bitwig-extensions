package com.bitwig.extensions.framework.time;

public class TimedDelayEvent extends AbstractTimedEvent {
   private final Runnable timedAction;

   public TimedDelayEvent(final Runnable timedAction, final long delayTime) {
      super(delayTime);
      this.timedAction = timedAction;
   }

   public void process() {
      if (completed) {
         return;
      }
      final long passedTime = System.currentTimeMillis() - startTime;
      if (passedTime >= delayTime) {
         timedAction.run();
         completed = true;
      }
   }
}
