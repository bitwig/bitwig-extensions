package com.bitwig.extensions.framework.time;

import java.util.function.IntConsumer;

public class TimeRepeatEvent extends AbstractTimedEvent {
   private final int repeatTime;
   private long repeatTimer = -1;
   private int repeatCount = 0;
   private final IntConsumer action;

   public TimeRepeatEvent(final IntConsumer action, final long delayTime, final int repeatTime) {
      super(delayTime);
      this.action = action;
      this.repeatTime = repeatTime;
   }

   public TimeRepeatEvent(final Runnable runAction, final long delayTime, final int repeatTime) {
      super(delayTime);
      this.repeatTime = repeatTime;
      this.action = repeat -> runAction.run();
   }


   @Override
   public void process() {
      if (completed) {
         return;
      }
      final long passedTime = System.currentTimeMillis() - startTime;
      if (repeatTimer == -1 && passedTime > delayTime) {
         action.accept(++repeatCount);
         repeatTimer = System.currentTimeMillis();
      } else if (repeatTimer > 0 && (System.currentTimeMillis() - repeatTimer) >= repeatTime) {
         action.accept(++repeatCount);
         repeatTimer = System.currentTimeMillis();
      }
   }
}
