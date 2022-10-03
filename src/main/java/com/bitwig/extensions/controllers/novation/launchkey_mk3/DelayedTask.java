package com.bitwig.extensions.controllers.novation.launchkey_mk3;

public class DelayedTask {

   private final int delayTime;
   private final Runnable action;
   private final long startTime;

   public DelayedTask(final int delayTime, final Runnable action) {
      this.delayTime = delayTime;
      this.action = action;
      startTime = System.currentTimeMillis();
   }

   public boolean expired() {
      return System.currentTimeMillis() - startTime > delayTime;
   }

   public void execute() {
      action.run();
   }
}
