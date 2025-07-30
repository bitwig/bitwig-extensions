package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColorState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LaunchButton {
   public static final int STD_REPEAT_DELAY = 400;
   public static final int STD_REPEAT_FREQUENCY = 50;

   private final int index;
   private final MultiStateHardwareLight light;
   protected HardwareButton hwButton;
   protected LaunchControlMidiProcessor midiProcessor;
   protected TimedEvent currentTimer;
   protected long recordedDownTime;
   protected boolean momentaryJustTurnedOn;
   protected final int midiId;
   protected final int channel;

   public LaunchButton(final String name, final int index, final int midiId, final int channel,
                       final HardwareSurface surface, final LaunchControlMidiProcessor midiProcessor) {
      this.midiProcessor = midiProcessor;
      this.midiId = midiId;
      this.index = index;
      this.channel = channel;
      hwButton = surface.createHardwareButton(name + "_" + midiId);
      midiProcessor.setCcMatcher(hwButton, midiId, channel);
      light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
      light.state().onUpdateHardware(this::updateStateCC);
   }

   public LaunchButton(final CcConstValues ccConst, final HardwareSurface surface,
                       final LaunchControlMidiProcessor midiProcessor) {
      this(ccConst.toString(), 0, ccConst.getCcNr(), 0, surface, midiProcessor);
   }

   private void updateStateCC(final InternalHardwareLightState state) {
      if (state instanceof final RgbState rgbState) {
         switch (rgbState.getState()) {
            case NORMAL:
               midiProcessor.sendMidi(0xB0, midiId, rgbState.getColorIndex());
               break;
            case FLASHING:
               midiProcessor.sendMidi(0xB0, midiId, rgbState.getColorIndex());
               midiProcessor.sendMidi(0xB1, midiId, rgbState.getAltColor());
               break;
            case PULSING:
               midiProcessor.sendMidi(0xB2, midiId, rgbState.getColorIndex());
               break;
         }
      } else if (state instanceof final RgbColorState colorState) {
         midiProcessor.sendRgb(midiId, colorState.getColor());
      } else {
         midiProcessor.sendMidi(0xB0, midiId, 0);
      }
   }


   public void bindIsPressed(final Layer layer, final Consumer<Boolean> handler) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> handler.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> handler.accept(false));
   }

   public void bindIsPressed(final Layer layer, final SettableBooleanValue value) {
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

   public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
      layer.bindLightState(supplier, light);
   }

   public void bindRepeatHold(final Layer layer, final Runnable action) {
      layer.bind(
         hwButton, hwButton.pressedAction(),
         () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
      layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
   }

   public void bindRepeatHold(final Layer layer, final Runnable action, final int repeatDelay,
                              final int repeatFrequency) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> initiateRepeat(action, repeatDelay, repeatFrequency));
      layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
   }

   private void cancelEvent() {
      if (currentTimer != null) {
         currentTimer.cancel();
         currentTimer = null;
      }
   }

   private void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
      action.run();
      currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
      midiProcessor.queueEvent(currentTimer);
   }

   public void forceUpdate() {
      updateStateCC(light.state().currentValue());
   }
}
