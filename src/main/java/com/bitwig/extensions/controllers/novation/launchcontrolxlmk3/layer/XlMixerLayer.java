package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
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
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.DisplayId;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightSendValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component(tag = "XLModel")
public class XlMixerLayer extends AbstractMixerLayer {
    
    @Inject
    private XlDawControlLayer dawLayer;
    
    
    private Row1ButtonMode row1Mode = Row1ButtonMode.SOLO;
    private Row2ButtonMode row2Mode = Row2ButtonMode.SELECT;
    
    private enum Row1ButtonMode {
        SOLO,
        ARM
    }
    
    private enum Row2ButtonMode {
        SELECT,
        MUTE
    }
    
    public XlMixerLayer(final Layers layers, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final LaunchControlMidiProcessor midiProcessor, final ControllerHost host,
        final TransportHandler transportHandler, final ButtonLayers buttonLayers) {
        super(layers, midiProcessor, host, viewControl, hwElements, displayControl, transportHandler, buttonLayers);
        
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 8; i++) {
            bindTrack(hwElements, trackBank, i);
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
    
    @Activate
    public void init() {
        this.setIsActive(true);
        applyMode();
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
        refBank.scrollPosition().addValueObserver(pos -> displayControl.show2LineTemporary(
            "Sends",
            "%s - %s".formatted(send1.name().get(), send2.name().get())));
    }
    
    
    private void bindTrack(final LaunchControlXlHwElements hwElements, final TrackBank trackBank, final int index) {
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
        
        final ParameterDisplayBinding send1DisplayBinding =
            new ParameterDisplayBinding(new DisplayId(row1Encoder.getTargetId(), displayControl), track.name(), send1);
        mixerLayer.addBinding(send1DisplayBinding);
        mixerLayer.addBinding(new AbsoluteEncoderBinding(send1, row1Encoder));
        mixerLayer.addBinding(new LightSendValueBindings(send1, row1Encoder.getLight()));
        
        final ParameterDisplayBinding send2DisplayBinding =
            new ParameterDisplayBinding(new DisplayId(row2Encoder.getTargetId(), displayControl), track.name(), send2);
        mixerLayer.addBinding(send2DisplayBinding);
        mixerLayer.addBinding(new LightSendValueBindings(send2, row2Encoder.getLight()));
        mixerLayer.addBinding(new AbsoluteEncoderBinding(send2, hwElements.getAbsoluteEncoder(1, index)));
        
        // fixedPanLabel
        final ParameterDisplayBinding panDisplayBinding =
            new ParameterDisplayBinding(
                new DisplayId(row3Encoder.getTargetId(), displayControl), track.name(),
                track.pan());
        mixerLayer.addBinding(panDisplayBinding);
        mixerLayer.addBinding(new LightValueBindings(track.pan(), row3Encoder.getLight(), GradientColor.PAN));
        mixerLayer.addBinding(new RelativeEncoderBinding(track.pan(), row3Encoder));
        
        // fixedVolumeLabel
        final ParameterDisplayBinding volumeDisplayBinding =
            new ParameterDisplayBinding(
                new DisplayId(row2Encoder.getTargetId(), displayControl), track.name(),
                track.volume());
        mixerLayer.addBinding(volumeDisplayBinding);
        this.addBinding(new SliderBinding(track.volume(), hwElements.getSlider(index)));
        
        final LaunchButton row2Button = hwElements.getRowButtons(1, index);
        final LaunchButton row1Button = hwElements.getRowButtons(0, index);
        row1Button.bindLight(buttonLayers.getArmLayer(), () -> armColor(track));
        row1Button.bindIsPressed(buttonLayers.getArmLayer(), pressed -> toggleArm(pressed, track));
        row1Button.bindLight(buttonLayers.getSoloLayer(), () -> soloColor(track));
        row1Button.bindIsPressed(buttonLayers.getSoloLayer(), pressed -> toggleSolo(pressed, track));
        
        row2Button.bindLight(buttonLayers.getSelectLayer(), () -> selectColor(track, index));
        row2Button.bindPressed(buttonLayers.getSelectLayer(), () -> selectTrack(track));
        row2Button.bindLight(buttonLayers.getMuteLayer(), () -> muteColor(track));
        row2Button.bindPressed(buttonLayers.getMuteLayer(), () -> track.mute().toggle());
    }
    
    private void toggleSoloArmMode() {
        if (this.row1Mode == Row1ButtonMode.ARM) {
            this.row1Mode = Row1ButtonMode.SOLO;
            displayControl.show2LineTemporary("Arm/Solo", "Solo");
        } else {
            this.row1Mode = Row1ButtonMode.ARM;
            displayControl.show2LineTemporary("Arm/Solo", "Arm");
        }
        applyArmSoloMode();
    }
    
    private void toggleSelectMuteMode() {
        if (this.row2Mode == Row2ButtonMode.SELECT) {
            this.row2Mode = Row2ButtonMode.MUTE;
            displayControl.show2LineTemporary("Mute/Select", "Mute");
        } else {
            this.row2Mode = Row2ButtonMode.SELECT;
            displayControl.show2LineTemporary("Mute/Select", "Select");
        }
        applySelectMuteMode();
    }
    
    protected void applyMode() {
        if (mode == BaseMode.MIXER) {
            midiProcessor.setToRelative(0, false);
            midiProcessor.setToRelative(1, false);
            midiProcessor.setToRelative(2, true);
        } else {
            midiProcessor.setToRelative(0, true);
            midiProcessor.setToRelative(1, true);
            midiProcessor.setToRelative(2, true);
        }
        this.mixerLayer.setIsActive(mode == BaseMode.MIXER);
        this.dawLayer.setIsActive(mode == BaseMode.DAW);
        
        
        applySelectMuteMode();
        applyArmSoloMode();
        
        this.buttonLayers.getSelectLayer().setIsActive(true);
        
    }
    
    private void applyArmSoloMode() {
        this.buttonLayers.getSoloLayer().setIsActive(row1Mode == Row1ButtonMode.SOLO);
        this.buttonLayers.getArmLayer().setIsActive(row1Mode == Row1ButtonMode.ARM);
    }
    
    private void applySelectMuteMode() {
        this.buttonLayers.getSelectLayer().setIsActive(row2Mode == Row2ButtonMode.SELECT);
        this.buttonLayers.getMuteLayer().setIsActive(row2Mode == Row2ButtonMode.MUTE);
    }
    
}
