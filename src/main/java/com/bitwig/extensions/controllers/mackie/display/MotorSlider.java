package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;

public class MotorSlider {

   private final HardwareSlider fader;
   private final FaderResponse response;
   private final HardwareButton touchButton;

   public MotorSlider(final String name, final int pitchBendChannel, final int touchNote, final HardwareSurface surface,
                      final MidiIn midiIn, final MidiOut midiOut) {
      fader = surface.createHardwareSlider(name + "_FADER");
      fader.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(8));

      response = new FaderResponse(midiOut, 8);

      touchButton = surface.createHardwareButton(name + "_TOUCH");
      touchButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, touchNote));
      touchButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, touchNote));
      fader.setHardwareButton(touchButton);
   }

   public void bindParameter(final Layer layer, final Parameter parameter) {
      layer.addBinding(new FaderBinding(parameter, response));
      layer.addBinding(new AbsoluteHardwareControlBinding(fader, parameter));
   }

   public HardwareSlider getFader() {
      return fader;
   }

   public void sendValue(final int value) {
      response.sendValue(0);
   }

}
