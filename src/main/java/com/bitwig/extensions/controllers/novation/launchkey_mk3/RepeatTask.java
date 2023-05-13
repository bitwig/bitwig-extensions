package com.bitwig.extensions.controllers.novation.launchkey_mk3;

public class RepeatTask {

   private final int initTime;
   private final int repeatTime;
   private final Runnable action;
   private final long startTime;
   private long lastRepeat = -1;

   RepeatTask(final int initTime, final int repeatTime, final Runnable action) {
      this.initTime = initTime;
      this.repeatTime = repeatTime;
      this.action = action;
      startTime = System.currentTimeMillis();
   }

   public void ping() {
      final long nowTime = System.currentTimeMillis();
      final long diff = nowTime - startTime;
      if (diff > initTime) {
         if (lastRepeat == -1) {
            action.run();
            lastRepeat = nowTime;
         } else {
            final long diffRepeat = nowTime - startTime;
            if (diffRepeat > repeatTime) {
               action.run();
               lastRepeat = nowTime;
            }
         }
      }
   }
}
