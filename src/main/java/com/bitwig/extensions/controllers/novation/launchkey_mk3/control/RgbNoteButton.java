package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyConstants;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;

public class RgbNoteButton extends RgbButton {

   private final int colorCodeBase;

   public RgbNoteButton(final LaunchkeyMk3Extension driver, final String name, final int index, final int channel,
                        final int noteNr) {
      super(driver, name, index, channel, noteNr);
      final MidiIn midiIn = driver.getMidiIn();
      final String format = String.format("(status==%d && data1==%d && data2>0)", LaunchkeyConstants.NOTE_ON | channel,
         noteNr);
      hwButton.pressedAction().setActionMatcher(midiIn.createActionMatcher(format));
      hwButton.releasedAction()
         .setActionMatcher(midiIn.createActionMatcher(
            String.format("(status==%d && data1==%d && data2==0)", LaunchkeyConstants.NOTE_ON | channel, noteNr)));
      hwLight.state().onUpdateHardware(this::updateState);
      colorCodeBase = channel == 9 ? (LaunchkeyConstants.NOTE_ON | 9) : LaunchkeyConstants.NOTE_ON;
   }

   private void updateState(final InternalHardwareLightState state) {
      if (state instanceof RgbState) {
         final RgbState rgbState = (RgbState) state;
         switch (rgbState.getState()) {
            case NORMAL:
               midiOut.sendMidi(colorCodeBase, number, rgbState.getColorIndex());
               break;
            case FLASHING:
               midiOut.sendMidi(colorCodeBase, number, rgbState.getAltColor());
               midiOut.sendMidi(colorCodeBase + 1, number, rgbState.getColorIndex());
               break;
            case PULSING:
               midiOut.sendMidi(colorCodeBase + 2, number, rgbState.getColorIndex());
               break;
         }
      } else {
         midiOut.sendMidi(colorCodeBase, number, 0);
      }
   }


}
