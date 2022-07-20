package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class LpButton {
   protected HardwareButton hwButton;
   protected MultiStateHardwareLight light;
   protected final MidiOut midiOut;

   protected LpButton(final String id, final HardwareSurface surface, final MidiOut midiOut) {
      super();
      this.midiOut = midiOut;
      hwButton = surface.createHardwareButton(id);
      light = surface.createMultiStateHardwareLight(id + "-light");
      light.state().setValue(RgbState.of(0));
      hwButton.isPressed().markInterested();
   }

   protected void initButtonNote(final MidiIn midiIn, final int notevalue) {
      hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, notevalue));
      hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, notevalue));
   }

   protected void initButtonCc(final MidiIn midiIn, final LabelCcAssignments ccAssignment) {
      hwButton.pressedAction().setActionMatcher(ccAssignment.createMatcher(midiIn, 127));
      hwButton.releasedAction().setActionMatcher(ccAssignment.createMatcher(midiIn, 0));
   }

   protected void initButtonCc(final MidiIn midiIn, final int ccValue) {
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccValue, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccValue, 0));
   }

   public HardwareButton getHwButton() {
      return hwButton;
   }

   public MultiStateHardwareLight getLight() {
      return light;
   }

   public void bind(final Layer layer, final Runnable action, final BooleanSupplier state, final LaunchColor color) {
      final RgbState onState = RgbState.of(color.getHiIndex());
      final RgbState offState = RgbState.of(color.getLowIndex());
      if (state instanceof BooleanValue) {
         ((BooleanValue) state).markInterested();
      }
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(() -> pressedStateCombo(hwButton.isPressed(), state, onState, offState), light);
   }

   private RgbState pressedStateCombo(final BooleanValue actionValue, final BooleanSupplier state,
                                      final RgbState onState, final RgbState offState) {
      if (!state.getAsBoolean()) {
         return RgbState.of(0);
      }
      if (actionValue.get()) {
         return onState;
      }
      return offState;
   }

   public void bind(final Layer layer, final Runnable action, final LaunchColor onColor) {
      final RgbState onState = RgbState.of(onColor.getHiIndex());
      final RgbState offState = RgbState.of(onColor.getLowIndex());
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(() -> hwButton.isPressed().get() ? onState : offState, light);
   }

   public void bind(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bind(final Layer layer, final Runnable action, final Supplier<InternalHardwareLightState> supplier) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(supplier, light);
   }

   public void bindPressed(final Layer layer, final Consumer<Boolean> target,
                           final Function<Boolean, RgbState> colorFunction) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      layer.bindLightState(() -> colorFunction.apply(hwButton.isPressed().get()), light);
   }

   public void bindPressed(final Layer layer, final Consumer<Boolean> target, final LaunchColor onColor) {
      final RgbState onState = RgbState.of(onColor.getHiIndex());
      final RgbState offState = RgbState.of(onColor.getLowIndex());
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      layer.bindLightState(() -> hwButton.isPressed().get() ? onState : offState, light);
   }

   public void bindPressed(final Layer layer, final Consumer<Boolean> target,
                           final Supplier<InternalHardwareLightState> supplier) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      layer.bindLightState(supplier, light);
   }

   public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
      layer.bindLightState(supplier, light);
   }

   public void bindPressed(final Layer layer, final Consumer<Boolean> target, final LaunchColor onColor,
                           final LaunchColor offColor) {
      final RgbState onState = RgbState.of(onColor);
      final RgbState offState = RgbState.of(offColor);
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      layer.bindLightState(() -> hwButton.isPressed().get() ? onState : offState, light);
   }

   public void bindPressed(final Layer layer, final SettableBooleanValue value, final LaunchColor color) {
      final RgbState onState = RgbState.of(color.getHiIndex());
      final RgbState offState = RgbState.of(color.getLowIndex());
      layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
      layer.bindLightState(() -> value.get() ? onState : offState, light);
   }

   public void bindToggle(final Layer layer, final SettableBooleanValue boolValue, final LaunchColor onColor,
                          final LaunchColor offColor) {
      final RgbState onState = RgbState.of(onColor);
      final RgbState offState = RgbState.of(offColor);
      boolValue.markInterested();
      layer.bind(hwButton, hwButton.pressedAction(), boolValue::toggle);
      layer.bindLightState(() -> boolValue.get() ? onState : offState, light);
   }

   public void bindToggle(final Layer layer, final SettableBooleanValue boolValue, final RgbState onState,
                          final RgbState offState) {
      boolValue.markInterested();
      layer.bind(hwButton, hwButton.pressedAction(), boolValue::toggle);
      layer.bindLightState(() -> boolValue.get() ? onState : offState, light);
   }

   public void bindToggle(final Layer layer, final SettableBooleanValue boolValue) {
      boolValue.markInterested();
      layer.bind(hwButton, hwButton.pressedAction(), boolValue::toggle);
   }

}
