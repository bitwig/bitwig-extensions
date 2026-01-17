package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class RemotesControlHandler {
    public static final int BASIC_ENCODER_MODE_NO = 3;
    
    private final PinnableCursorDevice cursorDevice;
    private EncoderLayer remotesControlLayer;
    private final RemotesControlLayer deviceControlLayer;
    private final RemotesControlLayer trackControlLayer;
    private final RemotesControlLayer projectControlLayer;
    private LayerId focusDeviceControl = LayerId.DEVICE_REMOTES;
    private LayerId encoderLayerMode = LayerId.DEVICE_REMOTES;
    private final BasicStringValue encoderModeValue = new BasicStringValue();
    private final MixEncoderLayer mixerLayer;
    private final CursorTrackMixLayer cursorTrackMixerLayer;
    private int selectedEncoderMode = 0;
    
    public RemotesControlHandler(final Layers layers, final MpkHwElements hwElements,
        final MpkMidiProcessor midiProcessor, final MpkViewControl viewControl) {
        cursorDevice = viewControl.getCursorDevice();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        encoderModeValue.set(encoderModeToString());
        
        deviceControlLayer = new RemotesControlLayer(
            "DEVICE", layers, cursorDevice.name(), cursorDevice.createCursorRemoteControlsPage(8), hwElements,
            midiProcessor);
        trackControlLayer = new RemotesControlLayer(
            "TRACK", layers, new BasicStringValue("Track Remotes"), cursorTrack.createCursorRemoteControlsPage(8),
            hwElements, midiProcessor);
        projectControlLayer = new RemotesControlLayer(
            "PROJECT", layers, new BasicStringValue("Project Remotes"),
            viewControl.getRootTrack().createCursorRemoteControlsPage(8), hwElements, midiProcessor);
        cursorTrackMixerLayer = new CursorTrackMixLayer(layers, hwElements, midiProcessor, viewControl);
        mixerLayer = new MixEncoderLayer(layers, hwElements, midiProcessor, viewControl);
        remotesControlLayer = deviceControlLayer;
        remotesControlLayer.getDisplayControl() //
            .ifPresent(control -> control.setActive(true));
    }
    
    private String encoderModeToString() {
        return switch (encoderLayerMode) {
            case TRACK_REMOTES, PROJECT_REMOTES, DEVICE_REMOTES -> "Remotes";
            case MIX_CONTROL -> "Mixer";
            case TRACK_CONTROL -> "Track";
            default -> "";
        };
    }
    
    public CursorTrackMixLayer getCursorTrackMixerLayer() {
        return cursorTrackMixerLayer;
    }
    
    public MixEncoderLayer getMixerLayer() {
        return mixerLayer;
    }
    
    public RemotesControlLayer getDeviceControlLayer() {
        return deviceControlLayer;
    }
    
    public RemotesControlLayer getTrackControlLayer() {
        return trackControlLayer;
    }
    
    public RemotesControlLayer getProjectControlLayer() {
        return projectControlLayer;
    }
    
    public void handleShiftEncoderTurn(final int inc) {
        if (encoderLayerMode == LayerId.MIX_CONTROL || encoderLayerMode == LayerId.TRACK_CONTROL) {
            if (inc > 0) {
                this.cursorDevice.selectNext();
            } else {
                this.cursorDevice.selectPrevious();
            }
            getFocussedDeviceControl().updateTemporary();
        } else {
            selectDevice(inc);
        }
    }
    
    public void selectDevice(final int inc) {
        if (encoderLayerMode == LayerId.DEVICE_REMOTES) {
            if (inc > 0) {
                this.cursorDevice.selectNext();
            } else {
                if (this.cursorDevice.hasPrevious().get()) {
                    this.cursorDevice.selectPrevious();
                } else {
                    setEncoderLayerMode(LayerId.TRACK_REMOTES);
                }
            }
        } else if (encoderLayerMode == LayerId.TRACK_REMOTES) {
            if (inc > 0) {
                setEncoderLayerMode(LayerId.DEVICE_REMOTES);
            } else {
                setEncoderLayerMode(LayerId.PROJECT_REMOTES);
            }
        } else if (encoderLayerMode == LayerId.PROJECT_REMOTES) {
            if (inc > 0) {
                setEncoderLayerMode(LayerId.TRACK_REMOTES);
            }
        }
    }
    
    public void incrementEncoderMode(final int inc, final boolean roundRobin) {
        final int previousMode = selectedEncoderMode;
        int nextMode = selectedEncoderMode + inc;
        if (roundRobin) {
            nextMode = nextMode % BASIC_ENCODER_MODE_NO;
        } else {
            nextMode = Math.max(0, Math.min(BASIC_ENCODER_MODE_NO - 1, nextMode));
        }
        if (nextMode != previousMode) {
            selectedEncoderMode = nextMode;
            if (selectedEncoderMode == 0) {
                backToDeviceControl();
            } else if (selectedEncoderMode == 1) {
                setEncoderLayerMode(LayerId.TRACK_CONTROL);
            } else if (selectedEncoderMode == 2) {
                setEncoderLayerMode(LayerId.MIX_CONTROL);
            }
        }
    }
    
    public void backToDeviceControl() {
        setEncoderLayerMode(focusDeviceControl);
    }
    
    public BasicStringValue getEncoderModeValue() {
        return encoderModeValue;
    }
    
    private EncoderLayer get(final LayerId layerId) {
        return switch (layerId) {
            case PROJECT_REMOTES -> projectControlLayer;
            case TRACK_REMOTES -> trackControlLayer;
            case TRACK_CONTROL -> cursorTrackMixerLayer;
            case MIX_CONTROL -> mixerLayer;
            default -> deviceControlLayer;
        };
    }
    
    private RemotesControlLayer getFocussedDeviceControl() {
        return switch (focusDeviceControl) {
            case PROJECT_REMOTES -> projectControlLayer;
            case TRACK_REMOTES -> trackControlLayer;
            default -> deviceControlLayer;
        };
    }
    
    public boolean canNavigateLeft() {
        return this.remotesControlLayer.canScrollLeft();
    }
    
    public boolean canNavigateRight() {
        return this.remotesControlLayer.canScrollRight();
    }
    
    public void navigateLeft() {
        this.remotesControlLayer.navigateLeft();
    }
    
    public void navigateRight() {
        this.remotesControlLayer.navigateRight();
    }
    
    public void setEncoderLayerMode(final LayerId id) {
        if (id == encoderLayerMode) {
            return;
        }
        this.encoderLayerMode = id;
        encoderModeValue.set(encoderModeToString());
        this.remotesControlLayer.setIsActive(false);
        this.remotesControlLayer = get(id);
        this.remotesControlLayer.setIsActive(true);
        if (this.encoderLayerMode.isControlsDevice()) {
            focusDeviceControl = this.encoderLayerMode;
        }
    }
}
