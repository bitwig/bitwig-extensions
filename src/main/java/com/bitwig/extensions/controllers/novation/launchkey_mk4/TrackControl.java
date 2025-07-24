package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.bitwig.extensions.framework.values.TrackType;

@Component
public class TrackControl {
    
    private final Layer armLayer;
    private final Layer selectLayer;
    private Mode mode = Mode.ARM;
    private Layer currentLayer;
    private final Layer controlLayer;
    //private int armHeld = 0;
    private final IntValueObject selectedTrackIndex = new IntValueObject(-1, -1, 100);
    private final RgbState[] trackColors = new RgbState[8];
    private final TrackType[] trackTypes = new TrackType[8];
    private final Project project;
    private final DisplayControl displayControl;
    
    private enum Mode {
        ARM,
        SELECT
    }
    
    public TrackControl(final Layers layers, final ViewControl viewControl, final LaunchkeyHwElements hwElements,
        final ControllerHost host, final DisplayControl displayControl) {
        this.project = host.getProject();
        this.displayControl = displayControl;
        this.armLayer = new Layer(layers, "ARM_TRACK");
        this.selectLayer = new Layer(layers, "SELECT_TRACK");
        this.controlLayer = new Layer(layers, "MAIN_LAYER");
        final TrackBank trackBank = viewControl.getTrackBank();
        final RgbButton modeButton = hwElements.getButton(CcAssignments.TRACK_MODE);
        modeButton.bindLight(controlLayer, () -> this.mode == Mode.ARM ? RgbState.RED : RgbState.WHITE);
        modeButton.bindPressed(controlLayer, this::toggleMode);
        final RgbButton[] buttons = hwElements.getTrackButtons();
        
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            final RgbButton button = buttons[i];
            track.addIsSelectedInMixerObserver(select -> {
                if (select) {
                    this.selectedTrackIndex.set(index);
                }
            });
            track.trackType().addValueObserver(type -> this.trackTypes[index] = TrackType.toType(type));
            track.arm().markInterested();
            track.exists().markInterested();
            track.color()
                .addValueObserver((r, g, b) -> trackColors[index] = RgbState.of(ColorLookup.toColor(r, g, b)).dim());
            button.bindLight(armLayer, () -> armColor(index, track));
            button.bindPressed(armLayer, () -> track.arm().toggle());
            button.bindLight(selectLayer, () -> selectColor(track, index));
            button.bindPressed(selectLayer, () -> selectTrack(track));
        }
        
        this.currentLayer = armLayer;
    }
    
    private void selectTrack(final Track track) {
        track.selectInMixer();
    }
    
    private RgbState armColor(final int index, final Track track) {
        if (track.exists().get()) {
            if (trackTypes[index].canBeArmed()) {
                return track.arm().get() ? RgbState.RED : RgbState.RED_LO;
            }
            return RgbState.DIM_WHITE;
        }
        return RgbState.OFF;
    }
    
    private RgbState selectColor(final Track track, final int index) {
        if (track.exists().get()) {
            return index == selectedTrackIndex.get() ? RgbState.WHITE : trackColors[index];
        }
        return RgbState.OFF;
    }
    
    private void toggleMode() {
        this.currentLayer.setIsActive(false);
        if (this.mode == Mode.ARM) {
            this.currentLayer = selectLayer;
            this.mode = Mode.SELECT;
            displayControl.show2Line("Fader Mode", "Arm");
        } else {
            this.currentLayer = armLayer;
            this.mode = Mode.ARM;
            displayControl.show2Line("Fader Mode", "Select");
        }
        this.currentLayer.setIsActive(true);
    }
    
    @Activate
    public void activate() {
        this.currentLayer.setIsActive(true);
        this.controlLayer.setIsActive(true);
    }
}
