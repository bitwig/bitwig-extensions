package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.apcmk2.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.EncoderLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.led.SingleLedState;
import com.bitwig.extensions.controllers.akai.apcmk2.midi.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.midi.MidiProcessorDirect;
import com.bitwig.extensions.framework.di.Context;

public class AkaiApcKeys25Extension extends AbstractAkaiApcExtension {

    protected AkaiApcKeys25Extension(final AkaiApcKeys25Definition definition, final ControllerHost host,
                                     ApcConfiguration configuration) {
        super(definition, host, configuration);
        controlLayerClass = EncoderLayer.class;
    }

    @Override
    protected MidiProcessor createMidiProcessor(MidiIn midiIn, MidiOut midiOut) {
        return new MidiProcessorDirect(getHost(), midiIn, midiOut);
    }
    
    @Override
    protected void init(final Context diContext) {
        Transport transport = diContext.getService(Transport.class);
        final SingleLedButton playButton = hwElements.getPlayButton();
        transport.isPlaying().markInterested();
        playButton.bindPressed(mainLayer, () -> transport.isPlaying().toggle());
        playButton.bindLight(mainLayer, () -> transport.isPlaying().get() ? SingleLedState.ON : SingleLedState.OFF);
        playButton.bindPressed(shiftLayer, transport::restart);
        final SingleLedButton recButton = hwElements.getRecButton();
        transport.isClipLauncherOverdubEnabled().markInterested();
        recButton.bindPressed(mainLayer, () -> transport.isClipLauncherOverdubEnabled().toggle());
        recButton.bindLight(mainLayer,
                () -> transport.isClipLauncherOverdubEnabled().get() ? SingleLedState.ON : SingleLedState.OFF);
        final SingleLedButton stopAllButton = hwElements.getStopAllButton();
        ViewControl viewControl = diContext.getService(ViewControl.class);
        stopAllButton.bindPressed(mainLayer, viewControl.getRootTrack().stopAction());
    }


}
