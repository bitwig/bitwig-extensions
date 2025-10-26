package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicIntegerValue;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public abstract class AbstractMixerLayer extends Layer {
    protected final LaunchControlMidiProcessor midiProcessor;
    protected final TransportHandler transportHandler;
    protected final Project project;
    protected final BooleanValueObject shiftState;
    protected final LaunchViewControl viewControl;
    protected final DisplayControl displayControl;
    protected int armHeld = 0;
    protected int soloHeld = 0;
    protected BaseMode mode = BaseMode.MIXER;
    protected final BasicIntegerValue selectedTrackIndex = new BasicIntegerValue();
    protected final RgbState[] trackColors = new RgbState[8];
    
    protected final BasicStringValue fixedVolumeLabel = new BasicStringValue("Volume");
    protected final BasicStringValue fixedPanLabel = new BasicStringValue("Panning");
    
    protected final Layer mixerLayer;
    
    
    public AbstractMixerLayer(final Layers layers, final LaunchControlMidiProcessor midiProcessor,
        final ControllerHost host, final LaunchViewControl viewControl, final LaunchControlXlHwElements hwElements,
        final DisplayControl displayControl, final TransportHandler transportHandler) {
        super(layers, "MIXER");
        mixerLayer = new Layer(layers, "MIXER_LAYER");
        this.project = host.getProject();
        this.transportHandler = transportHandler;
        this.midiProcessor = midiProcessor;
        project.hasArmedTracks().markInterested();
        project.hasSoloedTracks().markInterested();
        this.displayControl = displayControl;
        this.viewControl = viewControl;
        this.shiftState = hwElements.getShiftState();
        midiProcessor.addModeListener(this::handleModeChange);
    }
    
    protected abstract void applyMode();
    
    private void handleModeChange(final BaseMode baseMode) {
        this.mode = baseMode;
        applyMode();
    }
    
    protected void changeTrackColor(final int index, final int color) {
        if (color == 1) {
            trackColors[index] = RgbState.of(color);
        } else {
            trackColors[index] = RgbState.of(color).dim();
        }
    }
    
    protected void selectTrack(final Track track) {
        track.selectInMixer();
    }
    
    protected void toggleArm(final boolean pressed, final Track track) {
        if (shiftState.get()) {
            if (pressed) {
                track.arm().toggle();
            }
        } else if (pressed) {
            armHeld++;
            if (armHeld == 1) {
                final boolean armed = track.arm().get();
                project.unarmAll();
                if (!armed) {
                    track.arm().toggle();
                }
            } else {
                track.arm().toggle();
            }
        } else {
            if (armHeld > 0) {
                armHeld--;
            }
        }
    }
    
    protected void toggleSolo(final boolean pressed, final Track track) {
        if (shiftState.get()) {
            if (pressed) {
                track.solo().toggle();
            }
        } else if (pressed) {
            soloHeld++;
            if (soloHeld == 1) {
                track.solo().toggle(true);
            } else {
                track.solo().toggle();
            }
        } else {
            if (soloHeld > 0) {
                soloHeld--;
            }
        }
    }
    
    protected RgbState muteColor(final Track track) {
        if (track.exists().get()) {
            return track.mute().get() ? RgbState.ORANGE : RgbState.ORANGE_LO;
        }
        return RgbState.OFF;
    }
    
    protected RgbState selectColor(final Track track, final int index) {
        if (track.exists().get()) {
            return index == selectedTrackIndex.get() ? RgbState.WHITE : trackColors[index];
        }
        return RgbState.OFF;
    }
    
    protected RgbState armColor(final Track track) {
        if (track.exists().get()) {
            return track.arm().get() ? RgbState.RED : RgbState.RED_LO;
        }
        return RgbState.OFF;
    }
    
    protected RgbState soloColor(final Track track) {
        if (track.exists().get()) {
            return track.solo().get() ? RgbState.YELLOW : RgbState.YELLOW_LO;
        }
        return RgbState.OFF;
    }
    
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
}
