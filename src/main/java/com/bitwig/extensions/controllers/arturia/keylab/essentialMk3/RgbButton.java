package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.SysExHandler;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

public class RgbButton {
   private final byte[] rgbCommand = {(byte) 0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x04, //
      0x01, // 7 - Patch Id
      0x16, // 8 - Command
      0x02, // 9 - Pad ID
      0x00, // 10 - Red
      0x00, // 11 - Green
      0x00, // 12 - blue
      0x01, // state
      (byte) 0xF7};

   private final SysExHandler sysExHandler;
   private final HardwareButton hwButton;
   private final MultiStateHardwareLight light;
   private final int padId;
   private final int noteValue;
   private TimedEvent currentTimer;

   public enum Type {
      NOTE,
      CC
   }

   public RgbButton(final String name, final int padId, final Type type, final int value, final int channel,
                    final HardwareSurface surface, final SysExHandler sysExHandler) {
      this.padId = padId;
      noteValue = value;
      this.sysExHandler = sysExHandler;
      final MidiIn midiIn = sysExHandler.getMidiIn();
      sysExHandler.registerButton(this);
      rgbCommand[9] = (byte) padId;
      hwButton = surface.createHardwareButton(name + "_" + value);
      if (type == Type.NOTE) {
         hwButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, value));
         hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, value));
      } else {
         hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, value, 127));
         hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, value, 0));
      }
      hwButton.isPressed().markInterested();
      light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + "_" + value);
      light.setColorToStateFunction(RgbLightState::forColor);
      hwButton.setBackgroundLight(light);
      light.state().onUpdateHardware(this::updateState);
//        if (bankId.getIndex() == -1) { // Individual updates handled
//            light.state().onUpdateHardware(this::updateState);
//        }
   }

   public RgbButton(final CCAssignment hwElement, final Type type, final int channel, final SysExHandler sysExHandler,
                    final HardwareSurface surface) {
      this("PAD_" + hwElement.toString(), hwElement.getItemId(), type, hwElement.getCcId(), channel, surface,
         sysExHandler);
   }

   public Integer getNoteValue() {
      return noteValue;
   }

   public MultiStateHardwareLight getLight() {
      return light;
   }

   private void updateState(final InternalHardwareLightState state) {
      if (state instanceof final RgbLightState ligtState) {
         ligtState.apply(rgbCommand);
         sysExHandler.sendSysex(rgbCommand);
      } else {
         setRgbOff();
         sysExHandler.sendSysex(rgbCommand);
      }
   }

   public void forceDelayedRefresh() {
      sysExHandler.sendDelayed(rgbCommand, 50);
   }

   public void refresh() {
      sysExHandler.sendSysex(rgbCommand);
   }

   private void setRgbOff() {
      rgbCommand[10] = 0;
      rgbCommand[11] = 0;
      rgbCommand[12] = 0;
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
   }

   public void bindPressed(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bindPressed(final Layer layer, final Action action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   /**
    * Binds the given action to a button. Upon pressing the button the action is immediately executed. However, while
    * holding the button, the action repeats after an initial delay.
    *
    * @param layer           the layer this is bound to
    * @param pressedAction   action to be invoked and after a delay repeat
    * @param repeatDelay     time in ms until the action gets repeated
    * @param repeatFrequency time interval in ms between repeats
    */
   public void bindRepeatHold(final Layer layer, final Runnable pressedAction, final int repeatDelay,
                              final int repeatFrequency) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> initiateRepeat(pressedAction, repeatDelay, repeatFrequency));
      layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
   }

   /**
    * Binds the given action to a button. Upon pressing the button the action is immediately executed. However, while
    * holding the button, the action repeats after an initial delay.
    *
    * @param layer           the layer this is bound to
    * @param pressedAction   only called once when button is pressed
    * @param repeatAction    the action to be repeated (also called on first down)
    * @param releaseAction   action callback when button released
    * @param repeatDelay     time in ms until the action gets repeated
    * @param repeatFrequency time interval in ms between repeats
    */
   public void bindRepeatHold(final Layer layer, final Runnable pressedAction, final Runnable repeatAction,
                              final Runnable releaseAction, final int repeatDelay, final int repeatFrequency) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> {
         pressedAction.run();
         initiateRepeat(repeatAction, repeatDelay, repeatFrequency);
      });
      layer.bind(hwButton, hwButton.releasedAction(), () -> {
         cancelEvent();
         releaseAction.run();
      });
   }

   private void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
      action.run();
      currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
      sysExHandler.queueTimedEvent(currentTimer);
   }

   private void cancelEvent() {
      if (currentTimer != null) {
         currentTimer.cancel();
         currentTimer = null;
      }
   }

   public void bindReleased(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.releasedAction(), action);
   }

   public void bindLight(final Layer layer, final Supplier<RgbLightState> lightSource) {
      layer.bindLightState(lightSource::get, light);
   }

   public void bindLight(final Layer layer, final RgbLightState color, final RgbLightState holdColor) {
      hwButton.isPressed().markInterested();
      layer.bindLightState(() -> hwButton.isPressed().get() ? holdColor : color, light);
   }


   public void bind(final Layer layer, final Runnable action, final RgbLightState pressOn,
                    final RgbLightState pressOff) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(() -> hwButton.isPressed().get() ? pressOn : pressOff, light);
   }

   public void bind(final Layer layer, final HardwareActionBindable action, final RgbLightState pressOn,
                    final RgbLightState pressOff) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(() -> hwButton.isPressed().get() ? pressOn : pressOff, light);
   }


   public void bindToggle(final Layer layer, final SettableBooleanValue value, final RgbLightState onColor,
                          final RgbLightState offColor) {
      value.markInterested();
      layer.bind(hwButton, hwButton.pressedAction(), value::toggle);
      layer.bindLightState(() -> value.get() ? onColor : offColor, light);
   }

}
