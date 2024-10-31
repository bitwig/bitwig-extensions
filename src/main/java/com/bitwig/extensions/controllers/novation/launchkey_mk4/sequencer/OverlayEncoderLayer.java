package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.RelativeDisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchRelEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.DoubleNoteValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.NoteLengthValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.NoteValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class OverlayEncoderLayer extends Layer {
    
    private int currentTargetIndex;
    private final ClipState clipState;
    
    public OverlayEncoderLayer(final Layers layers, final LaunchRelEncoder[] incEncoders, final ClipState clipState,
        final DisplayControl display, final ValueSet gridValue) {
        super(layers, "NOTE_EDIT_LAYER");
        this.clipState = clipState;
        final NoteValueHandler velValue = new DoubleNoteValueHandler(NoteStep::velocity, NoteStep::setVelocity);
        addBinding(0, incEncoders[0], display, "Velocity", velValue);
        final NoteLengthValueHandler noteLengthValue =
            new NoteLengthValueHandler(NoteStep::duration, NoteStep::setDuration, gridValue);
        addBinding(1, incEncoders[1], display, "Steps", noteLengthValue);
        
        final NoteValueHandler rndValue = new DoubleNoteValueHandler(NoteStep::chance, NoteStep::setChance);
        addBinding(3, incEncoders[3], display, "Chance", rndValue);
        
        final NoteValueHandler timbreValue =
            new DoubleNoteValueHandler(NoteStep::timbre, NoteStep::setTimbre, -1, 1, 0.01);
        addBinding(6, incEncoders[6], display, "Timbre", timbreValue);
    }
    
    private void addBinding(final int index, final LaunchRelEncoder encoder, final DisplayControl display,
        final String paramName, final NoteValueHandler valueHandler) {
        final RelativeDisplayControl control =
            new RelativeDisplayControl(index, display, "Note Edit", paramName, valueHandler.getDisplayValue(),
                inc -> handleValueInc(inc, valueHandler),
                () -> encoder.setEncoderBehavior(LaunchRelEncoder.EncoderMode.NONACCELERATED));
        encoder.bindIncrementAction(this, control::handleInc);
        this.addBinding(control);
    }
    
    void handleValueInc(final int inc, final NoteValueHandler velocityValue) {
        final List<NoteStep> notes = clipState.getHeldNotes();
        velocityValue.setSteps(notes);
        velocityValue.doIncrement(inc);
    }
    
    public void setCurrentTargetIndex(final int currentTargetIndex) {
        this.currentTargetIndex = currentTargetIndex;
    }
}
