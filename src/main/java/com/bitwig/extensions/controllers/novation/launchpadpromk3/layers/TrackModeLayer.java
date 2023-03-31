package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.NovationColor;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;

public class TrackModeLayer extends Layer {
    public static final int MOMENTARY_TIME = 500;

    private TrackModeButtonMode buttonsMode = TrackModeButtonMode.SELECT;
    private TrackModeButtonMode returnToMode = null;

    private final SettableEnumValue postRecordingAction;
    private ControlMode controlMode = ControlMode.NONE;
    private TrackModeButtonMode stashedButtonMode = TrackModeButtonMode.SELECT;
    private ControlMode stashedControlMode = ControlMode.NONE;

    @Inject
    private ModeHandler modeHandler;

    public TrackModeLayer(final Layers layers, final ModifierStates modifiers, final Application application,
                          final Transport transport, final HwElements hwElements, final ViewCursorControl viewControl) {
        super(layers, "TRACK_MODE_LAYER");
        final Layer shiftLayer = new Layer(layers, "SHIFT_LAYER_TRACKMODE");
        application.canRedo().markInterested();
        application.canUndo().markInterested();
        transport.isMetronomeEnabled().markInterested();
        modifiers.getShiftActive().addValueObserver(shiftLayer::setIsActive);
        final SettableBeatTimeValue postRecordingTimeOffset = transport.getClipLauncherPostRecordingTimeOffset();
        postRecordingAction = transport.clipLauncherPostRecordingAction();
        postRecordingAction.markInterested();
        postRecordingTimeOffset.markInterested();

        final LabeledButton armButton = hwElements.getLabeledButton(LabelCcAssignments.RECORD_ARM_UNDO);
        armButton.bindPressReleaseAfter(this, () -> setButtonMode(TrackModeButtonMode.ARM), this::returnToPreviousMode,
                MOMENTARY_TIME);
        armButton.bindLight(this, () -> buttonsMode == TrackModeButtonMode.ARM ? RgbState.RED : RgbState.DIM_WHITE);
        armButton.bind(shiftLayer, application::undo,
                () -> application.canUndo().get() ? RgbState.WHITE : RgbState.OFF);

        final LabeledButton muteButton = hwElements.getLabeledButton(LabelCcAssignments.MUTE_REDO);
        muteButton.bindPressReleaseAfter(this, () -> setButtonMode(TrackModeButtonMode.MUTE),
                this::returnToPreviousMode, MOMENTARY_TIME);
        muteButton.bindLight(this,
                () -> buttonsMode == TrackModeButtonMode.MUTE ? NovationColor.AMBER.getMainColor() : RgbState.DIM_WHITE);

        muteButton.bind(shiftLayer, application::redo,
                () -> application.canRedo().get() ? RgbState.WHITE : RgbState.OFF);

        final LabeledButton soloButton = hwElements.getLabeledButton(LabelCcAssignments.SOLO_CLICK);
        soloButton.bindPressReleaseAfter(this, () -> setButtonMode(TrackModeButtonMode.SOLO),
                this::returnToPreviousMode, MOMENTARY_TIME);
        soloButton.bindLight(this,
                () -> buttonsMode == TrackModeButtonMode.SOLO ? NovationColor.YELLOW.getMainColor() : RgbState.DIM_WHITE);
        soloButton.bind(shiftLayer, () -> transport.isMetronomeEnabled().toggle(),
                () -> transport.isMetronomeEnabled().get() ? RgbState.TURQUOISE : RgbState.RED_LO);

        final LabeledButton sendButton = hwElements.getLabeledButton(LabelCcAssignments.SENDS_TAP);
        sendButton.bindPressed(shiftLayer, transport::tapTempo);
        sendButton.bindLightPressed(shiftLayer, RgbState.SHIFT_INACTIVE, RgbState.WHITE);


        final LabeledButton stopButton = hwElements.getLabeledButton(LabelCcAssignments.STOP_CLIP_SWING);
        stopButton.bindPressReleaseAfter(this, () -> setButtonMode(TrackModeButtonMode.STOP),
                this::returnToPreviousMode, MOMENTARY_TIME);
        stopButton.bindLight(this, () -> buttonsMode == TrackModeButtonMode.STOP ? RgbState.of(5) : RgbState.DIM_WHITE);
        stopButton.bindPressed(shiftLayer, () -> viewControl.getRootTrack().stop());
        stopButton.bindLightPressed(shiftLayer, RgbState.SHIFT_INACTIVE, RgbState.SHIFT_ACTIVE);

        final LabeledButton fixedLengthButton = hwElements.getLabeledButton(LabelCcAssignments.FIXED_LENGTH);

        fixedLengthButton.bindDelayedAction(this, this::enterFixedSetMode, this::releasedFixedLength, 300);
        fixedLengthButton.bindLight(this, pressed -> postRecordingAction.get()
                .equals("play_recorded") ? (pressed ? RgbState.ORANGE_PULSE : RgbState.ORANGE) : RgbState.DIM_WHITE);
        fixedLengthButton.disable(shiftLayer);

        initSliderModeButtons(hwElements, shiftLayer);
    }

    private void initSliderModeButtons(final HwElements hwElements, final Layer shiftLayer) {
        final LabeledButton volumeButton = hwElements.getLabeledButton(LabelCcAssignments.VOLUME);
        bindSliderButton(volumeButton, ControlMode.VOLUME, RgbState.of(ControlMode.VOLUME.getColor()));
        volumeButton.disable(shiftLayer);

        final LabeledButton panButton = hwElements.getLabeledButton(LabelCcAssignments.PAN);
        bindSliderButton(panButton, ControlMode.PAN, RgbState.of(61));
        panButton.disable(shiftLayer);

        final LabeledButton sendButton = hwElements.getLabeledButton(LabelCcAssignments.SENDS_TAP);
        bindSliderButton(sendButton, ControlMode.SENDS, RgbState.of(ControlMode.SENDS.getColor()));

        final LabeledButton deviceButton = hwElements.getLabeledButton(LabelCcAssignments.DEVICE_TEMPO);
        bindSliderButton(deviceButton, ControlMode.DEVICE, RgbState.of(ControlMode.DEVICE.getColor()));
        deviceButton.disable(shiftLayer);
    }

    private void bindSliderButton(final LabeledButton button, final ControlMode mode, final RgbState color) {
        button.bindPressReleaseAfter(this, () -> toggleControlMode(mode), this::returnToPreviousMode, MOMENTARY_TIME);
        button.bindLight(this, () -> controlMode == mode ? color : RgbState.DIM_WHITE);
    }

    private void returnToPreviousMode(final boolean longPress) {
        if (longPress) {
            final TrackModeButtonMode previousMode = buttonsMode;
            buttonsMode = stashedButtonMode;
            modeHandler.toFaderMode(stashedControlMode, controlMode);
            setButtonsMode(stashedButtonMode, previousMode);
        } else {
            stashedButtonMode = buttonsMode;
            stashedControlMode = controlMode;
        }
    }

    private void toggleControlMode(final ControlMode controlMode) {
        stashedControlMode = this.controlMode;
        stashedButtonMode = buttonsMode;
        final ControlMode oldMode = this.controlMode;
        if (oldMode == controlMode) {
            this.controlMode = ControlMode.NONE;
        } else {
            this.controlMode = controlMode;
        }
        if (this.controlMode != ControlMode.NONE && buttonsMode != TrackModeButtonMode.SELECT) {
            setButtonMode(TrackModeButtonMode.SELECT);
            toggleButtonMode(TrackModeButtonMode.SELECT, buttonsMode);
        }
        modeHandler.toFaderMode(this.controlMode, stashedControlMode);
    }

    private void enterFixedSetMode() {
        returnToMode = buttonsMode;
        setButtonMode(TrackModeButtonMode.FIXED_LENGTH);
        postRecordingAction.set("play_recorded");
    }

    private void releasedFixedLength() {
        if (returnToMode != null) {
            setButtonMode(returnToMode);
            returnToMode = null;
        } else {
            if (postRecordingAction.get().equals("play_recorded")) {
                postRecordingAction.set("off");
            } else {
                postRecordingAction.set("play_recorded");
            }
        }
    }


    public void setControlMode(final ControlMode mode) {
        controlMode = mode;
    }

    void setButtonMode(final TrackModeButtonMode mode) {
        stashedButtonMode = buttonsMode;
        toggleButtonMode(mode, stashedButtonMode);
        if (buttonsMode != TrackModeButtonMode.SELECT) {
            toStandardMode();
        }
    }

    private void toggleButtonMode(final TrackModeButtonMode mode, final TrackModeButtonMode previousMode) {
        if (buttonsMode == mode) {
            if (buttonsMode != TrackModeButtonMode.SELECT) {
                setButtonsMode(TrackModeButtonMode.SELECT, previousMode);
            }
        } else {
            setButtonsMode(mode, previousMode);
        }
    }

    private void setButtonsMode(final TrackModeButtonMode mode, final TrackModeButtonMode previousMode) {
        buttonsMode = mode;
    }

    private void toStandardMode() {
        if (controlMode == ControlMode.NONE) {
            return;
        }
        stashedControlMode = controlMode;
        controlMode = ControlMode.NONE;
        modeHandler.toFaderMode(controlMode, stashedControlMode);
    }

    public TrackModeButtonMode getButtonsMode() {
        return buttonsMode;
    }


}
