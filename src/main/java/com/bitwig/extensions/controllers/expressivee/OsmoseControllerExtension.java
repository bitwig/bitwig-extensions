package com.bitwig.extensions.controllers.expressivee;

import java.time.format.DateTimeFormatter;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.di.Context;

public class OsmoseControllerExtension extends ControllerExtension {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private HardwareSurface surface;
    
    protected OsmoseControllerExtension(final OsmoseControllerExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host);
    }
    
    public void init() {
        final Context diContext = new Context(this);
        surface = diContext.getService(HardwareSurface.class);
        final NoteInput noteInput1 =
            getHost().getMidiInPort(0).createNoteInput("External MIDI Port (Port 1)", "??????");
        final NoteInput noteInput2 = getHost().getMidiInPort(1).createNoteInput("Haken Port (Port 2)", "??????");
        // Pitchbend range for Port 1 (External MIDI Port) uses the MPE standard of 48 semitones. Sensitivity menu of
        // External MIDI Mode allows setting a fraction of this range for per-key bend, typically 1/48 or 2/48.
        noteInput1.setUseExpressiveMidi(true, 0, 48);
        // Pitchbend range for Port 2 (Haken Port) uses 96 semitone range as typically used by the EaganMatrix.
        noteInput2.setUseExpressiveMidi(true, 0, 96);
    }
    
    @Override
    public void exit() {
        // Nothing right now
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
