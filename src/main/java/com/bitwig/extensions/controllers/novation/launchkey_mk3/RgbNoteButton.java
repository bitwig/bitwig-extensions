package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;

public class RgbNoteButton extends RgbButton {

   public RgbNoteButton(final LaunchkeyMk3Extension driver, final String name, final int index, final int noteNr) {
      super(driver, name, index, noteNr, 0);
      final MidiIn midiIn = driver.getMidiIn();
      final String format = String.format("(status==%d && data1==%d && data2>0)", 0x90 | channel, noteNr);
      hwButton.pressedAction().setActionMatcher(midiIn.createActionMatcher(format));
      hwButton.releasedAction()
         .setActionMatcher(
            midiIn.createActionMatcher(String.format("(status==%d && data1==%d && data2==0)", 0x90 | channel, noteNr)));
      hwLight.state().onUpdateHardware(this::updateState);
   }

   private void updateState(final InternalHardwareLightState state) {
      if (state instanceof RgbState) {
         final RgbState rgbState = (RgbState) state;
         switch (rgbState.getState()) {
            case NORMAL:
               midiOut.sendMidi(0x90, number, rgbState.getColorIndex());
               break;
            case FLASHING:
               midiOut.sendMidi(0x90, number, rgbState.getAltColor());
               midiOut.sendMidi(0x91, number, rgbState.getColorIndex());
               break;
            case PULSING:
               midiOut.sendMidi(0x92, number, rgbState.getColorIndex());
               break;
         }
      } else {
         midiOut.sendMidi(0x90, number, 0);
      }
   }


}
