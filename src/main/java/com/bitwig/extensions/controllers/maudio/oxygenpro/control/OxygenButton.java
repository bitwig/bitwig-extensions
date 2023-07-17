package com.bitwig.extensions.controllers.maudio.oxygenpro.control;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.maudio.oxygenpro.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.Consumer;

public abstract class OxygenButton {
   public static final int STD_REPEAT_DELAY = 400;
   public static final int STD_REPEAT_FREQUENCY = 50;

   protected final MidiProcessor midiProcessor;
   protected HardwareButton hwButton;
   private TimedEvent currentTimer;
   private long recordedDownTime;
   protected final int midiId;

   protected OxygenButton(int midiId, MidiProcessor midiProcessor) {
      this.midiId = midiId;
      this.midiProcessor = midiProcessor;
   }

   public int getMidiId() {
      return midiId;
   }

   public void bind(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bind(hwButton, hwButton.releasedAction(), action);
   }

   public void bind(final Layer layer, final SettableBooleanValue value) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> consumer) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> consumer.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> consumer.accept(false));
   }

   public void bindPressed(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bindPressed(final Layer layer, final HardwareActionBindable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bindRelease(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.releasedAction(), action);
   }

   /**
    * Binds the given action to a button. Upon pressing the button the action is immediately executed. However while
    * holding the button, the action repeats after an initial delay. The standard delay time of 400ms and repeat
    * frequency of 50ms are used.
    *
    * @param layer  the layer this is bound to
    * @param action action to be invoked and after a delay repeat
    */
   public void bindRepeatHold(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(),
         () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
      layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
   }

   public void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
      action.run();
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
