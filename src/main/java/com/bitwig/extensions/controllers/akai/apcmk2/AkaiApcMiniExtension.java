package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.DrumPadLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.SessionLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.SliderLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.led.SingleLedState;
import com.bitwig.extensions.controllers.akai.apcmk2.midi.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.midi.MidiProcessorBuffered;
import com.bitwig.extensions.framework.di.Context;

public class AkaiApcMiniExtension extends AbstractAkaiApcExtension {

   protected AkaiApcMiniExtension(final AkaiApcMiniDefinition definition, final ControllerHost host,
                                  ApcConfiguration configuration) {
      super(definition, host, configuration);
      controlLayerClass = SliderLayer.class;
   }

   @Override
   protected MidiProcessor createMidiProcessor(MidiIn midiIn, MidiOut midiOut) {
      return new MidiProcessorBuffered(getHost(), midiIn, midiOut);
   }

   @Override
   protected void init(final Context diContext) {
      final DrumPadLayer drumPadLayer = diContext.create(DrumPadLayer.class);
      final SingleLedButton stopAllButton = hwElements.getSceneButton(7);
      ViewControl viewControl = diContext.getService(ViewControl.class);
      final Track rootTrack = viewControl.getRootTrack();
      stopAllButton.bindPressed(shiftLayer, rootTrack.stopAction());
      stopAllButton.bindLightPressed(shiftLayer, SingleLedState.OFF, SingleLedState.ON);
      final HardwareSlider masterSlider = hwElements.getSlider(8);
      mainLayer.bind(masterSlider, rootTrack.volume());
      SessionLayer sessionLayer = diContext.getService(SessionLayer.class);
      final MidiProcessor midiProcessor = diContext.getService(MidiProcessor.class);
      midiProcessor.setModeChangeListener(mode -> changeMode(drumPadLayer, sessionLayer, mode));
   }

   private void changeMode(DrumPadLayer drumPadLayer, SessionLayer sessionLayer, int mode) {
      if (mode == 0) { // Session Mode
         drumPadLayer.setIsActive(false);
         sessionLayer.setIsActive(true);
         shiftLayer.setIsActive(true);
         hwElements.refreshStatusButtons();
         hwElements.refreshGridButtons();
      } else if (mode == 2) { // Drum Mode
         drumPadLayer.setIsActive(true);
         sessionLayer.setIsActive(false);
         drumPadLayer.refreshButtons();
         hwElements.refreshStatusButtons();
      } else if (mode == 1) { // Key Mode
         drumPadLayer.setIsActive(false);
         sessionLayer.setIsActive(false);
         hwElements.refreshStatusButtons();
      }
   }


}
