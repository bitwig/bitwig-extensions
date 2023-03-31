package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.akai.apcmk2.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.DrumPadLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.SessionLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.SliderLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.led.SingleLedState;
import com.bitwig.extensions.framework.di.Context;

public class AkaiApcMiniExtension extends AbstractAkaiApcExtension {

    protected AkaiApcMiniExtension(final AkaiApcMiniDefinition definition, final ControllerHost host,
                                   ApcConfiguration configuration) {
        super(definition, host, configuration);
        controlLayerClass = SliderLayer.class;
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

        final MidiProcessor midiProcessor = diContext.getService(MidiProcessor.class);
        final SessionLayer sessionLayer = diContext.getService(SessionLayer.class);
        midiProcessor.setModeChangeListener(mode -> {
            if (mode == 0) {
                drumPadLayer.setIsActive(false);
                DebugApc.println(" Refresch > ");
                hwElements.refreshGridButtons();
            } else if (mode == 2) {
                DebugApc.println(" Drum Mode");
                drumPadLayer.setIsActive(true);
            } else if (mode == 1) {
                DebugApc.println(" Key Mode");
                drumPadLayer.setIsActive(false);
            }
        });
    }


}
