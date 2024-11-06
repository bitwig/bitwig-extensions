package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.List;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings.RelativeDisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchRelEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.DoubleNoteValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.IntNoteValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.NoteLengthFineValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.NoteLengthValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.NoteValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value.StdDoubleNoteValueHandler;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class OverlayEncoderLayer extends Layer {

    private final ClipState clipState;

    public OverlayEncoderLayer(final Layers layers, final LaunchRelEncoder[] incEncoders, final ClipState clipState,
        final DisplayControl display, final ValueSet gridValue) {
        super(layers, "NOTE_EDIT_LAYER");
        this.clipState = clipState;
        final DoubleNoteValueHandler velValue =
            new StdDoubleNoteValueHandler(NoteStep::velocity, NoteStep::setVelocity);
        int index = 0;
        addBinding(index, incEncoders[index++], display, "Velocity", velValue);
        final NoteLengthValueHandler noteLengthValue =
            new NoteLengthValueHandler(NoteStep::duration, NoteStep::setDuration, gridValue);
        addBinding(index, incEncoders[index++], display, "Steps", noteLengthValue);
        final NoteLengthFineValueHandler noteLengthFineValue =
            new NoteLengthFineValueHandler(NoteStep::duration, NoteStep::setDuration, gridValue);
        addBinding(index, incEncoders[index++], display, "Steps Fine", noteLengthFineValue);
        final DoubleNoteValueHandler rndValue = new StdDoubleNoteValueHandler(NoteStep::chance, NoteStep::setChance);
        addBinding(index, incEncoders[index++], display, "Chance", rndValue);
        final IntNoteValueHandler repeatValue =
            new IntNoteValueHandler(NoteStep::repeatCount, NoteStep::setRepeatCount, 1, 64);
        addBinding(index, incEncoders[index++], display, "Repeat", repeatValue);
        final DoubleNoteValueHandler repeatCurve =
            new StdDoubleNoteValueHandler(NoteStep::repeatCurve, NoteStep::setRepeatCurve, -1, 1, 0.01);
        addBinding(index, incEncoders[index++], display, "Repeat Curve", repeatCurve);
        final DoubleNoteValueHandler timbreValue =
            new StdDoubleNoteValueHandler(NoteStep::timbre, NoteStep::setTimbre, -1, 1, 0.01);
        addBinding(index, incEncoders[index++], display, "Timbre", timbreValue);
        final DoubleNoteValueHandler afterTouch =
            new StdDoubleNoteValueHandler(NoteStep::pressure, NoteStep::setPressure, 0, 1, 0.01);
        addBinding(index, incEncoders[index], display, "Aftertouch", afterTouch);
    }

    private void addBinding(final int index, final LaunchRelEncoder encoder, final DisplayControl display,
        final String paramName, final NoteValueHandler valueHandler) {
        final RelativeDisplayControl control =
            new RelativeDisplayControl(index, display, "Note Edit", paramName, valueHandler.getDisplayValue(),
                inc -> handleValueInc(inc, valueHandler),
                () -> encoder.setEncoderBehavior(LaunchRelEncoder.EncoderMode.NONACCELERATED)
            );
        encoder.bindIncrementAction(this, control::handleInc);
        this.addBinding(control);
    }

    void handleValueInc(final int inc, final NoteValueHandler velocityValue) {
        final List<NoteStep> notes = clipState.getHeldNotes();
        velocityValue.setSteps(notes);
        velocityValue.doIncrement(inc);
    }

}
