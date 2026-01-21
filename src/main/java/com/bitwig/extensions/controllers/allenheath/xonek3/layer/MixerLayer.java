package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.allenheath.xonek3.DeviceHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.ViewControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3GlobalStates;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneEncoder;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MixerLayer extends Layer {
    private final TrackState[] trackState;
    private static final int[] BRIGHTNESS_GRADS = {1, 3, 4, 8, 20};
    private static final XoneRgbColor.BrightnessScale WHITE_SCALE = XoneRgbColor.WHITE.scaleOf(BRIGHTNESS_GRADS);
    private final Layer shiftLayer;
    private final XoneK3GlobalStates globalStates;
    
    private static class TrackState {
        private XoneRgbColor color;
        private XoneRgbColor halfColor;
        private XoneRgbColor dimColor;
        private boolean isPlaying = false;
        private boolean isInstrument = false;
        private int vuState = 0;
        private boolean isSelected = false;
        private final Track track;
        
        public TrackState(final Track track) {
            track.addIsSelectedInMixerObserver(selected -> this.isSelected = selected);
            this.track = track;
            track.color().addValueObserver((r, g, b) -> {
                color = XoneRgbColor.of(r, g, b, XoneRgbColor.FULL_BRIGHT);
                halfColor = color.bright(XoneRgbColor.HALF_BRIGHT);
                dimColor = color.bright(XoneRgbColor.DIM);
            });
            track.playingNotes().addValueObserver(playingNotes -> {
                isPlaying = playingNotes.length > 0;
            });
            track.trackType().addValueObserver(trackType -> {
                isInstrument = "Instrument".equals(trackType);
            });
            track.addVuMeterObserver(5, -1, true, vu -> this.vuState = vu);
        }
        
        public boolean exists() {
            return track.exists().get();
        }
        
        public XoneRgbColor activityState() {
            if (isSelected) {
                if (isInstrument) {
                    return isPlaying ? XoneRgbColor.WHITE : XoneRgbColor.WHITE_HALF;
                }
                return XoneRgbColor.WHITE;
            }
            if (isInstrument) {
                return isPlaying ? color : halfColor;
            }
            return switch (vuState) {
                case 0 -> dimColor;
                case 1, 2 -> halfColor;
                case 3 -> color;
                default -> XoneRgbColor.RED;
            };
        }
        
        public XoneRgbColor selectColor() {
            if (isSelected) {
                return XoneRgbColor.WHITE;
            }
            return halfColor;
        }
    }
    
    public MixerLayer(final Layers layers, final ViewControl viewControl, final XoneHwElements hwElements,
        final XoneK3GlobalStates globalStates) {
        super(layers, "MIXER");
        this.shiftLayer = new Layer(layers, "MIXER_SHIFT_LAYER");
        this.globalStates = globalStates;
        globalStates.getShiftHeld().addValueObserver(this::handleShift);
        final TrackBank trackBank = viewControl.getTrackBank();
        this.trackState = new TrackState[trackBank.getSizeOfBank()];
        trackBank.setShouldShowClipLauncherFeedback(true);
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            bindTrack(track, index, hwElements.getDeviceElements(index / 4));
        }
    }
    
    private void handleShift(final boolean shiftHeld) {
        if (isActive()) {
            shiftLayer.setIsActive(shiftHeld);
        }
    }
    
    private void bindTrack(final Track track, final int index, final DeviceHwElements hwElements) {
        final List<AbsoluteHardwareControl> sliders = hwElements.getSliders();
        final List<AbsoluteHardwareControl> knobs = hwElements.getKnobs();
        final List<XoneRgbButton> knobButtons = hwElements.getKnobButtons();
        final List<XoneEncoder> encoders = hwElements.getEncoders();
        final int controlIndex = index % 4;
        
        trackState[index] = new TrackState(track);
        this.bind(knobs.get(controlIndex), track.sendBank().getItemAt(0).value());
        this.bind(knobs.get(controlIndex + 4), track.sendBank().getItemAt(1).value());
        this.bind(knobs.get(controlIndex + 8), track.pan().value());
        this.bind(sliders.get(controlIndex), track.volume().value());
        for (int i = 0; i < 3; i++) {
            shiftLayer.bind(knobs.get(controlIndex + i * 4), track.sendBank().getItemAt(i + 2).value());
        }
        
        final XoneRgbButton muteButton = knobButtons.get(controlIndex + 8);
        final XoneRgbButton soloButton = knobButtons.get(controlIndex + 4);
        final XoneRgbButton armButton = knobButtons.get(controlIndex);
        armButton.bindLight(this, () -> armColorState(track));
        armButton.bindPressed(this, () -> track.arm().toggle());
        
        soloButton.bindLight(this, () -> soloColorState(track));
        soloButton.bindPressed(this, () -> track.solo().toggleUsingPreferences(true));
        
        muteButton.bindLight(this, () -> muteColorState(track));
        muteButton.bindPressed(this, () -> track.mute().toggle());
        
        // TODO GROUP TRACKs + Bink things
        armButton.bindLight(shiftLayer, () -> groupColorState(track));
        armButton.bindPressed(shiftLayer, () -> handleGroupTrack(track));
        
        final TrackState trackState = this.trackState[index];
        soloButton.bindLight(shiftLayer, () -> stopColorState(trackState));
        soloButton.bindPressed(shiftLayer, () -> track.stop());
        
        muteButton.bindLight(shiftLayer, () -> selectColorState(trackState));
        muteButton.bindPressed(shiftLayer, () -> track.selectInMixer());
        
        final XoneRgbButton encoderButton = encoders.get(controlIndex).getPushButton();
        encoderButton.bindLight(this, trackState::activityState);
    }
    
    private void handleGroupTrack(final Track track) {
        if (track.isGroup().get()) {
            track.isGroupExpanded().toggle();
        }
    }
    
    private InternalHardwareLightState groupColorState(final Track track) {
        if (!track.exists().get()) {
            return XoneRgbColor.OFF;
        }
        if (track.isGroupExpanded().get()) {
            return XoneRgbColor.AQUA;
        }
        if (track.isGroup().get()) {
            return XoneRgbColor.WHITE;
        }
        return XoneRgbColor.WHITE_LO;
    }
    
    private static XoneRgbColor armColorState(final Track track) {
        return track.exists().get() ? (track.arm().get() ? XoneRgbColor.RED : XoneRgbColor.RED_DIM) : XoneRgbColor.OFF;
    }
    
    private static XoneRgbColor soloColorState(final Track track) {
        return track.exists().get()
            ? (track.solo().get() ? XoneRgbColor.YELLOW : XoneRgbColor.YELLOW_DIM)
            : XoneRgbColor.OFF;
    }
    
    private static XoneRgbColor muteColorState(final Track track) {
        return track.exists().get()
            ? (track.mute().get() ? XoneRgbColor.ORANGE : XoneRgbColor.ORANGE_DIM)
            : XoneRgbColor.OFF;
    }
    
    private XoneRgbColor stopColorState(final TrackState track) {
        if (!track.exists()) {
            return XoneRgbColor.OFF;
        }
        if (track.track.isQueuedForStop().get()) {
            return globalStates.blinkMid(XoneRgbColor.BLUE);
        }
        if (track.track.isStopped().get()) {
            return XoneRgbColor.BLUE_DIM;
        }
        return XoneRgbColor.BLUE;
    }
    
    private static XoneRgbColor selectColorState(final TrackState track) {
        if (track.exists()) {
            return track.selectColor();
        }
        return XoneRgbColor.OFF;
    }
    
    
}
