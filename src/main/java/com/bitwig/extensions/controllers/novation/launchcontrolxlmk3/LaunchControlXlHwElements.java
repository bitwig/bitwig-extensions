package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchLight;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LaunchControlXlHwElements {

   private final LaunchRelativeEncoder[] relativeEncoders = new LaunchRelativeEncoder[24];
   private final LaunchAbsoluteEncoder[] absoluteEncoders = new LaunchAbsoluteEncoder[24];
   private final LaunchLight[] encoderLights = new LaunchLight[24];
   private final LaunchButton[] rowButtons = new LaunchButton[16];

   private final HardwareSlider[] sliders = new HardwareSlider[8];
   private final HardwareButton shiftButton;
   private final Map<CcConstValues, LaunchButton> launchButtons = new HashMap<>();
   private final BooleanValueObject shiftState = new BooleanValueObject();

   public LaunchControlXlHwElements(final HardwareSurface surface, final LaunchControlMidiProcessor midiProcessor) {

      shiftButton = surface.createHardwareButton("SHIFT_BUTTON");
      midiProcessor.setCcMatcher(shiftButton, 0x3F, 0x6);
      shiftButton.isPressed().addValueObserver(pressed -> shiftState.set(pressed));
      Arrays.stream(CcConstValues.values())
         .forEach(state -> launchButtons.put(state, new LaunchButton(state, surface, midiProcessor)));

      for (int i = 0; i < 24; i++) {
         encoderLights[i] = new LaunchLight("ENC", i, 0xD + i, surface, midiProcessor);
         relativeEncoders[i] = new LaunchRelativeEncoder(i, surface, midiProcessor, encoderLights[i]);
         absoluteEncoders[i] = new LaunchAbsoluteEncoder(i, surface, midiProcessor, encoderLights[i]);
      }

      for (int i = 0; i < 8; i++) {
         sliders[i] = surface.createHardwareSlider("SLIDER_" + (i + 1));
         midiProcessor.setAbsoluteCcMatcher(sliders[i], 0x5 + i, 0xF);
         rowButtons[i] = new LaunchButton("SOLO_ARM", i, 0x25 + i, 0, surface, midiProcessor);
         rowButtons[i + 8] = new LaunchButton("SELECT_MUTE", i, 0x2D + i, 0, surface, midiProcessor);
      }
      midiProcessor.registerUpdater(this::handleForceUpdate);
   }

   private void handleForceUpdate() {
      for (int i = 0; i < 24; i++) {
         encoderLights[i].forceUpdate();
      }
      for (final LaunchButton rowButton : rowButtons) {
         rowButton.forceUpdate();
      }
      for (final LaunchButton button : launchButtons.values()) {
         button.forceUpdate();
      }
   }

   public BooleanValueObject getShiftState() {
      return shiftState;
   }

   public void bindShiftPressed(final Layer layer, final Consumer<Boolean> consumer) {
      layer.bind(shiftButton, shiftButton.pressedAction(), () -> consumer.accept(true));
      layer.bind(shiftButton, shiftButton.releasedAction(), () -> consumer.accept(false));
   }

   public LaunchRelativeEncoder getRelativeEncoder(final int row, final int index) {
      return relativeEncoders[row * 8 + index];
   }

   public LaunchAbsoluteEncoder getAbsoluteEncoder(final int row, final int index) {
      return absoluteEncoders[row * 8 + index];
   }

   public LaunchButton getRowButtons(final int row, final int index) {
      return rowButtons[row * 8 + index];
   }

   public HardwareSlider getSlider(final int index) {
      return sliders[index];
   }

   public LaunchButton getButton(final CcConstValues constValue) {
      return launchButtons.get(constValue);
   }
}
