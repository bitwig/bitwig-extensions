package com.bitwig.extensions.controllers.akai.mpkminiplus;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.IntConsumer;

public class MpkButton {
   public static final int STD_REPEAT_DELAY = 400;
   public static final int STD_REPEAT_FREQUENCY = 50;

   private MpkMiniPlusControllerExtension midiProcessor;
   protected HardwareButton hwButton;
   private TimedEvent currentTimer;
   private long recordedDownTime;

   public MpkButton(MpkMiniPlusControllerExtension midiProcessor, HardwareButton hwButton) {
      this.hwButton = hwButton;
      this.midiProcessor = midiProcessor;
   }

   /**
    * Binds the given action to a button. Upon pressing the button the action is immediately executed. However while
    * holding the button, the action repeats after an initial delay. The standard delay time of 400ms and repeat
    * frequency of 50ms are used.
    *
    * @param layer  the layer this is bound to
    * @param action action to be invoked and after a delay repeat
    */
   public void bindRepeatHold(final Layer layer, final IntConsumer action) {
      layer.bind(hwButton, hwButton.pressedAction(),
         () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
      layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
   }

   public void bindPressed(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void initiateRepeat(final IntConsumer action, final int repeatDelay, final int repeatFrequency) {
      action.accept(0);
      currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
      midiProcessor.queueEvent(currentTimer);
   }

   private void cancelEvent() {
      if (currentTimer != null) {
         currentTimer.cancel();
         currentTimer = null;
      }
   }
}
