package com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.Midi;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLed;

public class PadButton implements RgbButton {

   public static final int UNDO = 0;
   public static final int REDO = 1;
   public static final int STEP_UNDO = 2;
   public static final int STEP_REDO = 3;
   public static final int QUANTIZE = 4;
   public static final int QUANTIZE_50 = 5;
   public static final int NUDGE_LEFT = 6;
   public static final int NUDGE_RIGHT = 7;
   public static final int CLEAR = 8;
   public static final int CLEAR_AUTO = 9;
   public static final int COPY = 10;
   public static final int PASTE = 11;
   public static final int SEMI_MINUS = 12;
   public static final int SEMI_PLUS = 13;
   public static final int OCT_MINUS = 14;
   public static final int OCT_PLUS = 15;

   public static final int PAD_NOTE_OFFSET = 60;

   private final HardwareButton hwButton;
   private final int index;
   private final MultiStateHardwareLight light;

   public PadButton(final int index, final MaschineExtension driver) {
      final HardwareSurface surface = driver.getSurface();
      final MidiIn midiIn = driver.getMidiIn();
      this.index = index;
      final String id = "PAD_" + (index + 1);
      hwButton = surface.createHardwareButton(id);
      hwButton.pressedAction()
         .setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, PAD_NOTE_OFFSET + index));
      hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, PAD_NOTE_OFFSET + index));

      light = surface.createMultiStateHardwareLight(id + "-light");
      light.state().setValue(RgbLed.OFF);
      light.state().onUpdateHardware(hwState -> driver.updatePadLed(this));
      hwButton.setBackgroundLight(light);
   }

   @Override
   public int getMidiDataNr() {
      return index + PAD_NOTE_OFFSET;
   }

   @Override
   public int getMidiStatus() {
      return Midi.NOTE_ON;
   }

   @Override
   public MultiStateHardwareLight getLight() {
      return light;
   }

   @Override
   public HardwareButton getHwButton() {
      return hwButton;
   }

   public int getIndex() {
      return index;
   }

}
