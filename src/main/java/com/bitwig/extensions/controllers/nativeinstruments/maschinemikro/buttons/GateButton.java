package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.TrackLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.BiConsumer;

public class GateButton {
   public static final int STD_REPEAT_DELAY = 400;
   public static final int STD_REPEAT_FREQUENCY = 50;
   private static final Runnable DO_NOTHING = () -> {
   };

   protected final MidiProcessor midiProcessor;
   protected HardwareButton hwButton;
   private TimedEvent currentTimer;
   private long recordedDownTime;
   protected final int midiId;

   protected GateButton(int midiId, MidiProcessor midiProcessor) {
      this.midiId = midiId;
      this.midiProcessor = midiProcessor;
   }

   public void bindEmptyAction(TrackLayer layer) {
      layer.bind(hwButton, hwButton.pressedAction(), DO_NOTHING);
      layer.bind(hwButton, hwButton.releasedAction(), DO_NOTHING);
   }

   public void bindIsPressedTimed(final Layer layer, BiConsumer<Boolean, Long> handler) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> {
         recordedDownTime = System.currentTimeMillis();
         handler.accept(true, 0L);
      });
      layer.bind(hwButton, hwButton.releasedAction(), () -> {
         handler.accept(false, System.currentTimeMillis() - recordedDownTime);
      });
   }

   public void bind(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bind(hwButton, hwButton.releasedAction(), action);
   }

   public void bind(final Layer layer, final SettableBooleanValue value) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
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
