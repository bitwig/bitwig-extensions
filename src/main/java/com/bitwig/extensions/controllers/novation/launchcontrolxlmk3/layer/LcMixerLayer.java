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
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMk3Extension;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.AbsoluteEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.DisplayId;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightSendValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component(tag = "LCModel")
public class LcMixerLayer extends AbstractMixerLayer {
    
    @Inject
    private LcDawControlLayer dawLayer;
    
    private final Layer panLayer;
    private final Layer sendLayer;
    
    private boolean panFocus = true;
    private boolean updateFxText = false;
    private String fxTextName = "";
    private SpecControl specControl;
    
    private enum ButtonMode {
        SELECT,
        MUTE,
        SOLO,
        ARM
    }
    
    private ButtonMode buttonMode = ButtonMode.SELECT;
    
    public LcMixerLayer(final Layers layers, final LaunchControlMidiProcessor midiProcessor, final ControllerHost host,
        final LaunchViewControl viewControl, final LaunchControlXlHwElements hwElements,
        final DisplayControl displayControl, final TransportHandler transportHandler, final ButtonLayers buttonLayers) {
        super(layers, midiProcessor, host, viewControl, hwElements, displayControl, transportHandler, buttonLayers);
        LaunchControlMk3Extension.println(" LC MIXER LAYER");
        panLayer = new Layer(layers, "PAN");
        sendLayer = new Layer(layers, "SEND");
        
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 8; i++) {
            bindTrack(hwElements, trackBank, i);
        }
        bindNavigation(hwElements);
        final LaunchButton lcButton = hwElements.getButtons(CcConstValues.DAW_SPEC);
        lcButton.bindIsPressed(this, this::handleSpecButton);
    }
    
    @Activate
    public void init() {
        this.setIsActive(true);
        applyMode();
    }
    
    public void setSpecOverlay(final SpecControl specControl) {
        this.specControl = specControl;
    }
    
    private void handleSpecButton(final Boolean pressed) {
        specControl.setActive(pressed);
        midiProcessor.setToRelative(1, pressed);
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton trackLeftButton = hwElements.getButtons(CcConstValues.TRACK_LEFT);
        final LaunchButton trackRightButton = hwElements.getButtons(CcConstValues.TRACK_RIGHT);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        
        trackLeftButton.bindLight(mixerLayer, () -> viewControl.canNavLeft() ? RgbState.WHITE : RgbState.OFF);
        trackRightButton.bindLight(
            mixerLayer, () -> viewControl.canNavRight(shiftState.get()) ? RgbState.WHITE : RgbState.OFF);
        
        trackRightButton.bindRepeatHold(mixerLayer, this::navTrackRight);
        trackLeftButton.bindRepeatHold(mixerLayer, this::navTrackLeft);
        
        final LaunchButton functionButton = hwElements.getButtons(CcConstValues.MUTE_SELECT_MODE);
        functionButton.bindIsPressed(this, this::buttonModePressed);
        functionButton.bindLight(this, this::functionButtonColor);
        final SegmentDisplayBinding trackDisplayBinding =
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getFixedDisplay());
        mixerLayer.addBinding(trackDisplayBinding);
        
        final SendBank refBank = viewControl.getRefSendBank();
        refBank.canScrollBackwards().markInterested();
        refBank.canScrollForwards().markInterested();
        refBank.getItemAt(0).name().addValueObserver(this::updateFxSendName);
        final LaunchButton pageUpButton = hwElements.getButtons(CcConstValues.PAGE_DOWN);
        final LaunchButton pageDownButton = hwElements.getButtons(CcConstValues.PAGE_UP);
        pageUpButton.bindLight(
            mixerLayer, () -> panFocus || refBank.canScrollForwards().get() ? RgbState.WHITE : RgbState.OFF);
        pageDownButton.bindLight(
            mixerLayer, () -> !panFocus || refBank.canScrollBackwards().get() ? RgbState.WHITE : RgbState.OFF);
        pageUpButton.bindRepeatHold(mixerLayer, () -> navigatePage(1));
        pageDownButton.bindRepeatHold(mixerLayer, () -> navigatePage(-1));
    }
    
    private void updateFxSendName(final String name) {
        this.fxTextName = name;
        if (updateFxText) {
            displayControl.show2LineTemporary("Row 1 Control", "Send : %s".formatted(fxTextName));
            updateFxText = false;
        }
    }
    
    private void navigatePage(final int dir) {
        if (panFocus) {
            if (dir > 0) {
                panFocus = false;
                applyMode();
                displayControl.show2LineTemporary("Row 1 Control", "Send : %s".formatted(fxTextName));
            } else {
                displayControl.show2LineTemporary("Row 1 Control", "Pan");
            }
        } else {
            if (dir < 0 && !viewControl.getRefSendBank().canScrollBackwards().get()) {
                panFocus = true;
                applyMode();
                displayControl.show2LineTemporary("Row 1 Control", "Pan");
            } else {
                updateFxText = true;
                viewControl.navigateSends(dir);
            }
        }
    }
    
    private void bindTrack(final LaunchControlXlHwElements hwElements, final TrackBank trackBank, final int index) {
        final Track track = trackBank.getItemAt(index);
        final Send send1 = track.sendBank().getItemAt(0);
        send1.name().markInterested();
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
        
        //fixedVolumeLabel
        final ParameterDisplayBinding volumeDisplayBinding =
            new ParameterDisplayBinding(
                new DisplayId(row2Encoder.getTargetId(), displayControl), track.name(),
                track.volume());
        mixerLayer.addBinding(volumeDisplayBinding);
        mixerLayer.addBinding(new LightValueBindings(track.volume(), row2Encoder.getLight(), GradientColor.WHITE));
        mixerLayer.addBinding(new AbsoluteEncoderBinding(track.volume(), row2Encoder));
        
        final ParameterDisplayBinding send1DisplayBinding =
            new ParameterDisplayBinding(new DisplayId(row1Encoder.getTargetId(), displayControl), track.name(), send1);
        sendLayer.addBinding(send1DisplayBinding);
        sendLayer.addBinding(new LightSendValueBindings(send1, row1Encoder.getLight()));
        sendLayer.addBinding(new AbsoluteEncoderBinding(send1, row1Encoder));
        
        final LaunchRelativeEncoder relativeRow2Encoder = hwElements.getRelativeEncoder(0, index);
        final ParameterDisplayBinding panDisplayBinding =
            new ParameterDisplayBinding(
                new DisplayId(relativeRow2Encoder.getTargetId(), displayControl), track.name(), track.pan());
        panLayer.addBinding(panDisplayBinding);
        panLayer.addBinding(new LightValueBindings(track.pan(), relativeRow2Encoder.getLight(), GradientColor.PAN));
        panLayer.addBinding(new RelativeEncoderBinding(track.pan(), relativeRow2Encoder));
        
        final LaunchButton button = hwElements.getRowButtons(0, index);
        button.bindLight(this.buttonLayers.getSelectLayer(), () -> selectColor(track, index));
        button.bindPressed(this.buttonLayers.getSelectLayer(), () -> selectTrack(track));
        button.bindLight(this.buttonLayers.getSoloLayer(), () -> soloColor(track));
        button.bindIsPressed(this.buttonLayers.getSoloLayer(), pressed -> toggleSolo(pressed, track));
        button.bindLight(this.buttonLayers.getArmLayer(), () -> armColor(track));
        button.bindIsPressed(this.buttonLayers.getArmLayer(), pressed -> toggleArm(pressed, track));
        button.bindLight(this.buttonLayers.getMuteLayer(), () -> muteColor(track));
        button.bindPressed(this.buttonLayers.getMuteLayer(), () -> track.mute().toggle());
        transportHandler.assignTransportButtons(hwElements.getButtons(1), this.buttonLayers.getMuteLayer());
    }
    
    private void buttonModePressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        switch (this.buttonMode) {
            case SELECT -> {
                buttonMode = ButtonMode.SOLO;
                displayControl.show2LineTemporary("Button Function", "Solo");
            }
            case SOLO -> {
                buttonMode = ButtonMode.MUTE;
                displayControl.show2LineTemporary("Button Function", "Mute");
            }
            case MUTE -> {
                buttonMode = ButtonMode.ARM;
                displayControl.show2LineTemporary("Button Function", "Arm");
            }
            case ARM -> {
                buttonMode = ButtonMode.SELECT;
                displayControl.show2LineTemporary("Button Function", "Select");
            }
        }
        applyButtonMode();
    }
    
    private RgbState functionButtonColor() {
        return switch (this.buttonMode) {
            case SELECT -> RgbState.WHITE;
            case SOLO -> RgbState.YELLOW;
            case ARM -> RgbState.RED;
            case MUTE -> RgbState.ORANGE;
        };
    }
    
    @Override
    protected void applyMode() {
        if (mode == BaseMode.MIXER) {
            midiProcessor.setToRelative(0, panFocus);
            midiProcessor.setToRelative(1, false);
        } else {
            midiProcessor.setToRelative(0, true);
            midiProcessor.setToRelative(1, true);
        }
        
        this.mixerLayer.setIsActive(mode == BaseMode.MIXER);
        this.dawLayer.setIsActive(mode == BaseMode.DAW);
        if (mode == BaseMode.MIXER) {
            this.panLayer.setIsActive(panFocus);
            this.sendLayer.setIsActive(!panFocus);
        } else {
            this.panLayer.setIsActive(false);
            this.sendLayer.setIsActive(false);
        }
        applyButtonMode();
    }
    
    private void applyButtonMode() {
        this.buttonLayers.getSelectLayer().setIsActive(buttonMode == ButtonMode.SELECT);
        this.buttonLayers.getSoloLayer().setIsActive(buttonMode == ButtonMode.SOLO);
        this.buttonLayers.getMuteLayer().setIsActive(buttonMode == ButtonMode.MUTE);
        this.buttonLayers.getArmLayer().setIsActive(buttonMode == ButtonMode.ARM);
    }
    
    private void activateButtonModes(final boolean active) {
        if (active) {
            applyButtonMode();
        } else {
            this.buttonLayers.getSelectLayer().setIsActive(active);
            this.buttonLayers.getMuteLayer().setIsActive(active);
            this.buttonLayers.getSoloLayer().setIsActive(active);
            this.buttonLayers.getArmLayer().setIsActive(active);
        }
    }
    
    public void navTrackRight() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(8);
        } else {
            viewControl.navigateCursorBy(1);
        }
        displayControl.cancelTemporary();
    }
    
    public void navTrackLeft() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(-8);
        } else {
            viewControl.navigateCursorBy(-1);
        }
        displayControl.cancelTemporary();
    }
    
    
}