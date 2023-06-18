package com.bitwig.extensions.controllers.maudio.oxygenpro.control;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimedEvent;

public abstract class OxygenButton {

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

   public void bindPressed(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bindPressed(final Layer layer, final HardwareActionBindable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bindRelease(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.releasedAction(), action);
   }

}
