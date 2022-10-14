package com.bitwig.extensions.controllers.arturia.keystep;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KeyStepProExtension extends ControllerExtension {

    private Layers layers;
    private MidiIn midiIn;
    private MidiOut midiOut;
    private Layer mainLayer;
    private HardwareSurface surface;
    private ControllerHost host;

    protected KeyStepProExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        host = getHost();
        layers = new Layers(this);
        midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
        midiOut = host.getMidiOutPort(0);
        surface = host.createHardwareSurface();
        final Transport transport = host.createTransport();
        final NoteInput noteInput = midiIn.createNoteInput("MIDI", "8?????", "9?????", "D?????", "E?????", "B?????");
        noteInput.setShouldConsumeEvents(true);

        mainLayer = new Layer(layers, "Main");

        mainLayer.activate();
    }

    private void onMidi0(final ShortMidiMessage msg) {
        final int channel = msg.getChannel();
        final int sb = msg.getStatusByte() & (byte) 0xF0;
//        host.println(String.format("MIDI KSP <%d> %02x => %d %d", channel, sb, msg.getData1(), msg.getData2()));
    }

    @Override
    public void exit() {
        // no action for Keystep Pro
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }


}
