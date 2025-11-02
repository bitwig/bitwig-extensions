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
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightSendValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchAbsoluteEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
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
    private DawControlLayer dawLayer;
    
    private final Layer panLayer;
    private final Layer sendLayer;
    
    private final Layer selectLayer;
    private final Layer soloLayer;
    private final Layer muteLayer;
    private final Layer armLayer;
    
    private boolean panFocus = true;
    private boolean updateFxText = false;
    private String fxTextName = "";
    
    private enum ButtonMode {
        SELECT,
        MUTE,
        SOLO,
        ARM
    }
    
    private ButtonMode buttonMode = ButtonMode.SELECT;
    
    public LcMixerLayer(final Layers layers, final LaunchControlMidiProcessor midiProcessor, final ControllerHost host,
        final LaunchViewControl viewControl, final LaunchControlXlHwElements hwElements,
        final DisplayControl displayControl, final TransportHandler transportHandler) {
        super(layers, midiProcessor, host, viewControl, hwElements, displayControl, transportHandler);
        panLayer = new Layer(layers, "PAN");
        sendLayer = new Layer(layers, "SEND");
        selectLayer = new Layer(layers, "SELECT");
        soloLayer = new Layer(layers, "SOLO");
        muteLayer = new Layer(layers, "MUTE");
        armLayer = new Layer(layers, "ARM");
        
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 8; i++) {
            bindTrack(hwElements, trackBank, i);
        }
        bindNavigation(hwElements);
    }
    
    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton functionButton = hwElements.getButton(CcConstValues.MUTE_SELECT_MODE);
        functionButton.bindIsPressed(mixerLayer, this::buttonModePressed);
        functionButton.bindLight(mixerLayer, this::functionButtonColor);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        transportHandler.bindTrackNavigation(mixerLayer);
        mixerLayer.addBinding(
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getFixedDisplay()));
        
        final SendBank refBank = viewControl.getRefSendBank();
        refBank.getItemAt(0).name().addValueObserver(this::updateFxSendName);
        final LaunchButton pageUpButton = hwElements.getButton(CcConstValues.PAGE_DOWN);
        final LaunchButton pageDownButton = hwElements.getButton(CcConstValues.PAGE_UP);
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
            displayControl.show2Line("Row 1 Control", "Send : %s".formatted(fxTextName));
            updateFxText = false;
        }
    }
    
    private void navigatePage(final int dir) {
        if (panFocus) {
            if (dir > 0) {
                panFocus = false;
                displayControl.show2Line("Row 1 Control", "Send : %s".formatted(fxTextName));
                applyMode();
            } else {
                displayControl.show2Line("Row 1 Control", "Pan");
            }
        } else {
            if (dir < 0 && !viewControl.getRefSendBank().canScrollBackwards().get()) {
                panFocus = true;
                applyMode();
                displayControl.show2Line("Row 1 Control", "Pan");
            } else {
                updateFxText = true;
                viewControl.navigateSends(dir);
            }
        }
    }
    
    private void bindTrack(final LaunchControlXlHwElements hwElements, final TrackBank trackBank, final int index) {
        final Track track = trackBank.getItemAt(index);
        final SendBank refBank = viewControl.getRefSendBank();
        final Send send1 = refBank.getItemAt(0);
        send1.name().markInterested();
        refBank.canScrollBackwards().markInterested();
        refBank.canScrollForwards().markInterested();
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
        
        mixerLayer.addBinding(new LightValueBindings(track.volume(), row2Encoder.getLight(), GradientColor.WHITE));
        mixerLayer.addBinding(
            new AbsoluteEncoderBinding(track.volume(), row2Encoder, displayControl, track.name(), fixedVolumeLabel));
        
        sendLayer.addBinding(new LightSendValueBindings(send1, row1Encoder.getLight()));
        sendLayer.addBinding(
            new AbsoluteEncoderBinding(send1, row1Encoder, displayControl, track.name(), send1.name()));
        
        panLayer.addBinding(
            new LightValueBindings(track.pan(), hwElements.getRelativeEncoder(0, index).getLight(), GradientColor.PAN));
        panLayer.addBinding(
            new RelativeEncoderBinding(
                track.pan(), hwElements.getRelativeEncoder(0, index), displayControl, track.name(), fixedPanLabel));
        
        final LaunchButton button = hwElements.getRowButtons(0, index);
        button.bindLight(selectLayer, () -> selectColor(track, index));
        button.bindPressed(selectLayer, () -> selectTrack(track));
        button.bindLight(soloLayer, () -> soloColor(track));
        button.bindIsPressed(soloLayer, pressed -> toggleSolo(pressed, track));
        button.bindLight(armLayer, () -> armColor(track));
        button.bindIsPressed(armLayer, pressed -> toggleArm(pressed, track));
        button.bindLight(muteLayer, () -> muteColor(track));
        button.bindPressed(muteLayer, () -> track.mute().toggle());
    }
    
    @Activate
    public void init() {
        this.setIsActive(true);
        applyMode();
    }
    
    private void buttonModePressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        switch (this.buttonMode) {
            case SELECT -> {
                buttonMode = ButtonMode.SOLO;
                displayControl.show2Line("Button Function", "Solo");
            }
            case SOLO -> {
                buttonMode = ButtonMode.MUTE;
                displayControl.show2Line("Button Function", "Mute");
            }
            case MUTE -> {
                buttonMode = ButtonMode.ARM;
                displayControl.show2Line("Button Function", "Arm");
            }
            case ARM -> {
                buttonMode = ButtonMode.SELECT;
                displayControl.show2Line("Button Function", "Select");
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
        this.selectLayer.setIsActive(buttonMode == ButtonMode.SELECT);
        this.soloLayer.setIsActive(buttonMode == ButtonMode.SOLO);
        this.muteLayer.setIsActive(buttonMode == ButtonMode.MUTE);
        this.armLayer.setIsActive(buttonMode == ButtonMode.ARM);
    }
    
    
}