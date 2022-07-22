package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;

public class RgbCcButton extends RgbButton {

   public RgbCcButton(final LaunchkeyMk3Extension driver, final String name, final int index, final int channel,
                      final int ccNr) {
      super(driver, name, index, channel, ccNr);
      final MidiIn midiIn = driver.getMidiIn();
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0));
      hwLight.state().onUpdateHardware(this::updateState);
   }

   private void updateState(final InternalHardwareLightState state) {
      if (state instanceof RgbState) {
         final RgbState rgbState = (RgbState) state;
         switch (rgbState.getState()) {
            case NORMAL:
               midiOut.sendMidi(0xB0, number, rgbState.getColorIndex());
               break;
            case FLASHING:
               midiOut.sendMidi(0xB1, number, rgbState.getColorIndex());
               break;
            case PULSING:
               midiOut.sendMidi(0xB2, number, rgbState.getColorIndex());
               break;
         }
      } else {
         midiOut.sendMidi(0xB0, number, 0);
      }
   }


}
