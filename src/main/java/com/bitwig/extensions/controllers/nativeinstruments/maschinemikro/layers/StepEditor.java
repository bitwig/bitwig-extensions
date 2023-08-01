package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.nativeinstruments.commons.StepViewPosition;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class StepEditor extends Layer {
    
    private final Clip clip;
    private final StepViewPosition positionHandler; // TODO Move this to commons
    private int focusNote = 60;
    private final NoteStep[] assignments = new NoteStep[16];
    private int playingStep = -1;
    
    public StepEditor(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
        MidiProcessor midiProcessor, ControllerHost host) {
        super(layers, "PAD_LAYER");
        List<RgbButton> padButtons = hwElements.getPadButtons();
        this.clip = host.createLauncherCursorClip(16, 1);
        this.positionHandler = new StepViewPosition(this.clip);
    
        for (int i = 0; i < 16; i++) {
            final int index = i;
            RgbButton button = padButtons.get(i);
            button.bindPressed(this, () -> this.pressStep( index));
            button.bindRelease(this, () -> this.releaseStep(index));
            button.bindLight(this, () -> getRgbState(index));
        }
        clip.addNoteStepObserver(this::handleNoteStep);
        clip.playingStep().addValueObserver(this::handlePlayingStep);
    
        this.clip.scrollToKey(focusNote);
    }
    
    private void handleNoteStep(final NoteStep noteStep) {
        // Which grid step contains Notes
        assignments[noteStep.x()] = noteStep;
    }
    
    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }
    
    
    private RgbColor getRgbState(final int index) {
        return RgbColor.RED;
    }
    
/*    private InternalHardwareLightState computeGridLedState(final int index) {
        if (assignments[index] == null || assignments[index].state() == NoteStep.State.Empty) {
            if (index == playingStep) {
                return RgbLed.WHITE_BRIGHT;
            }
            return RgbLed.OFF;
        } else if (isDrumEdit) {
            if (index == playingStep) {
                return RgbLed.of(padColor + 3);
            } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
                return RgbLed.of(padColor);
            }
            return RgbLed.of(padColor + 2);
        } else {
            if (index == playingStep) {
                return RgbLed.of(clipColor + 3);
            } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
                return RgbLed.of(clipColor);
            }
            return RgbLed.of(clipColor + 2);
        }
    }
    */
    
    private void releaseStep(final int index) {
    
    }
    
    private void pressStep(final int index) {
    
    }
}
