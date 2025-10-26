package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.AbsoluteEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightSendValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition.AbstractLaunchControlExtensionDefinition;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.BasicIntegerValue;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component(tag = "XLModel")
public class MixerLayer extends Layer {
    
    @Inject
    private DawControlLayer dawLayer;
    
    private final Layer mixerLayer;
    private final Layer selectLayer;
    private final Layer soloLayer;
    private final Layer armLayer;
    private final Layer muteLayer;
    
    private BaseMode mode = BaseMode.MIXER;
    private final RgbState[] trackColors = new RgbState[8];
    private final BasicIntegerValue selectedTrackIndex = new BasicIntegerValue();
    private Row1ButtonMode row1Mode = Row1ButtonMode.SOLO;
    private Row2ButtonMode row2Mode = Row2ButtonMode.SELECT;
    private final DisplayControl displayControl;
    private int armHeld = 0;
    private int soloHeld = 0;
    private final Project project;
    private final BooleanValueObject shiftState;
    private final LaunchViewControl viewControl;
    private final TransportHandler transportHandler;
    
    private enum Row1ButtonMode {
        SOLO,
        ARM
    }
    
    private enum Row2ButtonMode {
        SELECT,
        MUTE
    }
    
    public MixerLayer(final Layers layers, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final LaunchControlMidiProcessor midiProcessor, final ControllerHost host,
        final TransportHandler transportHandler, final AbstractLaunchControlExtensionDefinition definition) {
        super(layers, "MAIN");
        this.project = host.getProject();
        this.viewControl = viewControl;
        project.hasArmedTracks().markInterested();
        project.hasSoloedTracks().markInterested();
        this.displayControl = displayControl;
        this.transportHandler = transportHandler;
        this.shiftState = hwElements.getShiftState();
        midiProcessor.addModeListener(this::handleModeChange);
        mixerLayer = new Layer(layers, "MIXER_LAYER");
        selectLayer = new Layer(layers, "SELECT");
        soloLayer = new Layer(layers, "SOLO");
        armLayer = new Layer(layers, "ARM");
        muteLayer = new Layer(layers, "MUTE");
        
        final BasicStringValue fixedVolumeLabel = new BasicStringValue("Volume");
        final BasicStringValue fixedPanLabel = new BasicStringValue("Panning");
        
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 8; i++) {
            bindTrack(hwElements, trackBank, i, fixedPanLabel, fixedVolumeLabel);
        }
        
        bindNavigation(hwElements);
        transportHandler.bindTransport(this);
        final LaunchButton soloArmButton = hwElements.getButton(CcConstValues.SOLO_ARM_MODE);
        final LaunchButton muteSelectButton = hwElements.getButton(CcConstValues.MUTE_SELECT_MODE);
        
        final LaunchButton specModeButton = hwElements.getButton(CcConstValues.DAW_SPEC);
        specModeButton.bindLight(this, () -> RgbState.BLUE);
        //specModeButton.bindPressed(this, () -> LaunchControlXlMk3Extension.println(" > PRESS SPEC >"));
        
        soloArmButton.bindLight(this, () -> row1Mode == Row1ButtonMode.ARM ? RgbState.RED : RgbState.YELLOW);
        muteSelectButton.bindLight(this, () -> row2Mode == Row2ButtonMode.SELECT ? RgbState.WHITE : RgbState.ORANGE);
        soloArmButton.bindPressed(this, this::toggleSoloArmMode);
        muteSelectButton.bindPressed(this, this::toggleSelectMuteMode);
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton pageUpButton = hwElements.getButton(CcConstValues.PAGE_UP);
        final LaunchButton pageDownButton = hwElements.getButton(CcConstValues.PAGE_DOWN);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        transportHandler.bindTrackNavigation(mixerLayer);
        
        mixerLayer.addBinding(
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getFixedDisplay()));
        
        final SendBank refBank = viewControl.getRefSendBank();
        final Send send1 = refBank.getItemAt(0);
        final Send send2 = refBank.getItemAt(1);
        send1.name().markInterested();
        send2.name().markInterested();
        refBank.canScrollBackwards().markInterested();
        refBank.canScrollForwards().markInterested();
        
        pageUpButton.bindLight(mixerLayer, () -> refBank.canScrollBackwards().get() ? RgbState.WHITE : RgbState.OFF);
        pageDownButton.bindLight(mixerLayer, () -> refBank.canScrollForwards().get() ? RgbState.WHITE : RgbState.OFF);
        pageUpButton.bindRepeatHold(mixerLayer, () -> viewControl.navigateSends(-1));
        pageDownButton.bindRepeatHold(mixerLayer, () -> viewControl.navigateSends(1));
        refBank.scrollPosition().addValueObserver(
            pos -> displayControl.show2Line("Sends", "%s - %s".formatted(send1.name().get(), send2.name().get())));
    }
    
    
    private void bindTrack(final LaunchControlXlHwElements hwElements, final TrackBank trackBank, final int index,
        final BasicStringValue fixedPanLabel, final BasicStringValue fixedVolumeLabel) {
        final Track track = trackBank.getItemAt(index);
        final Send send1 = track.sendBank().getItemAt(0);
        final Send send2 = track.sendBank().getItemAt(1);
        track.color().addValueObserver((r, g, b) -> changeTrackColor(index, ColorLookup.toColor(r, g, b)));
        track.addIsSelectedInMixerObserver(select -> {
            if (select) {
                this.selectedTrackIndex.set(index);
            }
        });
        track.arm().markInterested();
        track.exists().markInterested();
        track.mute().markInterested();
        track.solo().markInterested();
        
        final LaunchAbsoluteEncoder row1Encoder = hwElements.getAbsoluteEncoder(0, index);
        final LaunchAbsoluteEncoder row2Encoder = hwElements.getAbsoluteEncoder(1, index);
        final LaunchRelativeEncoder row3Encoder = hwElements.getRelativeEncoder(2, index);
        
        mixerLayer.addBinding(
            new AbsoluteEncoderBinding(send1, row1Encoder, displayControl, track.name(), send1.name()));
        mixerLayer.addBinding(new LightSendValueBindings(send1, row1Encoder.getLight()));
        mixerLayer.addBinding(new LightSendValueBindings(send2, row2Encoder.getLight()));
        mixerLayer.addBinding(new LightValueBindings(track.pan(), row3Encoder.getLight(), GradientColor.PAN));
        mixerLayer.addBinding(
            new AbsoluteEncoderBinding(
                send2, hwElements.getAbsoluteEncoder(1, index), displayControl, track.name(),
                send2.name()));
        mixerLayer.addBinding(
            new RelativeEncoderBinding(track.pan(), row3Encoder, displayControl, track.name(), fixedPanLabel));
        this.addBinding(
            new SliderBinding(
                index, track.volume(), hwElements.getSlider(index), displayControl, track.name(),
                fixedVolumeLabel));
        final LaunchButton row2Button = hwElements.getRowButtons(1, index);
        final LaunchButton row1Button = hwElements.getRowButtons(0, index);
        row1Button.bindLight(armLayer, () -> armColor(track));
        row1Button.bindIsPressed(armLayer, pressed -> toggleArm(pressed, track));
        row1Button.bindLight(soloLayer, () -> soloColor(track));
        row1Button.bindIsPressed(soloLayer, pressed -> toggleSolo(pressed, track));
        
        row2Button.bindLight(selectLayer, () -> selectColor(track, index));
        row2Button.bindPressed(selectLayer, () -> selectTrack(track));
        row2Button.bindLight(muteLayer, () -> muteColor(track));
        row2Button.bindPressed(muteLayer, () -> track.mute().toggle());
    }
    
    private void toggleSoloArmMode() {
        if (this.row1Mode == Row1ButtonMode.ARM) {
            this.row1Mode = Row1ButtonMode.SOLO;
            displayControl.show2Line("Arm/Solo", "Solo");
        } else {
            this.row1Mode = Row1ButtonMode.ARM;
            displayControl.show2Line("Arm/Solo", "Arm");
        }
        applyArmSoloMode();
    }
    
    private void toggleSelectMuteMode() {
        if (this.row2Mode == Row2ButtonMode.SELECT) {
            this.row2Mode = Row2ButtonMode.MUTE;
            displayControl.show2Line("Mute/Select", "Mute");
        } else {
            this.row2Mode = Row2ButtonMode.SELECT;
            displayControl.show2Line("Mute/Select", "Select");
        }
        applySelectMuteMode();
    }
    
    private void changeTrackColor(final int index, final int color) {
        if (color == 1) {
            trackColors[index] = RgbState.of(color);
        } else {
            trackColors[index] = RgbState.of(color).dim();
        }
    }
    
    private void selectTrack(final Track track) {
        track.selectInMixer();
    }
    
    private void toggleArm(final boolean pressed, final Track track) {
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
    
    private void toggleSolo(final boolean pressed, final Track track) {
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
    
    private RgbState muteColor(final Track track) {
        if (track.exists().get()) {
            return track.mute().get() ? RgbState.ORANGE : RgbState.ORANGE_LO;
        }
        return RgbState.OFF;
    }
    
    private RgbState selectColor(final Track track, final int index) {
        if (track.exists().get()) {
            return index == selectedTrackIndex.get() ? RgbState.WHITE : trackColors[index];
        }
        return RgbState.OFF;
    }
    
    private RgbState armColor(final Track track) {
        if (track.exists().get()) {
            return track.arm().get() ? RgbState.RED : RgbState.RED_LO;
        }
        return RgbState.OFF;
    }
    
    private RgbState soloColor(final Track track) {
        if (track.exists().get()) {
            return track.solo().get() ? RgbState.YELLOW : RgbState.YELLOW_LO;
        }
        return RgbState.OFF;
    }
    
    
    private void handleModeChange(final BaseMode baseMode) {
        this.mode = baseMode;
        applyMode();
    }
    
    @Activate
    public void init() {
        this.setIsActive(true);
        applyMode();
    }
    
    private void applyMode() {
        this.mixerLayer.setIsActive(mode == BaseMode.MIXER);
        this.dawLayer.setIsActive(mode == BaseMode.DAW);
        applySelectMuteMode();
        applyArmSoloMode();
        selectLayer.setIsActive(true);
    }
    
    private void applyArmSoloMode() {
        this.armLayer.setIsActive(row1Mode == Row1ButtonMode.ARM);
        this.soloLayer.setIsActive(row1Mode == Row1ButtonMode.SOLO);
    }
    
    private void applySelectMuteMode() {
        this.selectLayer.setIsActive(row2Mode == Row2ButtonMode.SELECT);
        this.muteLayer.setIsActive(row2Mode == Row2ButtonMode.MUTE);
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
