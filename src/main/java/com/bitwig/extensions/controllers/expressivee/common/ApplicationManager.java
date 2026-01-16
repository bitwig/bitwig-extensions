package com.bitwig.extensions.controllers.expressivee.common;

import javax.naming.ldap.Control;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.HardwareActionBindable;

public class ApplicationManager extends Manager {

    public Application mApplication;
    private ControllerHost mHost;
    private MidiIn mMidiIn;

    private Clip mArrangerClip;
    private Clip mLauncherClip;

    private HardwareButton mButtonUndo;
    private HardwareButton mButtonRedo;
    private HardwareButton mButtonQuantize;

    public ApplicationManager(final ControllerHost host, HardwareSurface surface, MidiIn midiIn, MidiOut midiOut) {
        super(midiOut);
        mMidiIn = midiIn;
        mHost = host;

        mApplication = host.createApplication();

        mArrangerClip = host.createArrangerCursorClip(Constant.CLIP_GRID_WIDTH, Constant.CLIP_GRID_HEIGHT);
        mLauncherClip = host.createLauncherCursorClip(Constant.CLIP_GRID_WIDTH, Constant.CLIP_GRID_HEIGHT);

        mButtonUndo = surface.createHardwareButton("Undo");
        mButtonUndo.setLabel("Undo");
        mButtonUndo.setLabelPosition(RelativePosition.ABOVE);
        mButtonUndo.pressedAction()
                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1, Constant.UNDO_CC));

        mButtonRedo = surface.createHardwareButton("Redo");
        mButtonRedo.setLabel("Redo");
        mButtonRedo.setLabelPosition(RelativePosition.ABOVE);
        mButtonRedo.pressedAction()
                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1, Constant.REDO_CC));

        mButtonQuantize = surface.createHardwareButton("Quantize");
        mButtonQuantize.setLabel("Quantize");
        mButtonQuantize.setLabelPosition(RelativePosition.ABOVE);
        mButtonQuantize.pressedAction()
                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1, Constant.QUANTIZE_CC));
    }

    public void init() {
        mArrangerClip.exists().markInterested();
        mLauncherClip.exists().markInterested();

        mButtonUndo.pressedAction().addBinding(mApplication.undoAction());
        mButtonRedo.pressedAction().addBinding(mApplication.undoAction());

        final HardwareActionBindable quantizeAction = mHost.createAction(() -> {
            this.quantizeCurrentClip();
        }, () -> "Quantize");
        mButtonQuantize.pressedAction().addBinding(quantizeAction);
    }

    private void quantizeCurrentClip() {
        if (mArrangerClip.exists().get()) {
            mArrangerClip.quantize(Constant.QUANTIZE_FULL);
        }

        if (mLauncherClip.exists().get()) {
            mLauncherClip.quantize(Constant.QUANTIZE_FULL);
        }
    }
}
