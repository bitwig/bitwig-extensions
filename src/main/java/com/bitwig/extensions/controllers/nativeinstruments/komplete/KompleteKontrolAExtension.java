package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.TextCommand;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.ValueCommand;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolAExtension extends KompleteKontrolExtension {
    
    public KompleteKontrolAExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    protected boolean hasDeviceControl() {
        return super.hasDeviceControl();
    }
    
    protected boolean hasSwitchedNavigationMapping() {
        return true;
    }
    
    @Override
    public void init() {
        super.init();
        midiProcessor.intoDawMode(0x3);
        final ControllerHost host = getHost();
        
        surface.setPhysicalSize(300, 200);
        
        layers = new Layers(this);
        mainLayer = new Layer(layers, "Main");
        arrangeFocusLayer = new Layer(layers, "ArrangeFocus");
        sessionFocusLayer = new Layer(layers, "SessionFocus");
        navigationLayer = new Layer(layers, "NavigationLayer");
        
        
        final MidiIn midiIn2 = host.getMidiInPort(1);
        final NoteInput noteInput =
            midiIn2.createNoteInput(
                "MIDI", "80????", "90????", "D0????", "E0????", "B001??", "B040??", "B042??", "B1????");
        noteInput.setShouldConsumeEvents(true);
        
        initTrackBank();
        setUpTransport();
        initJogWheel();
        
        bindMacroControl(midiIn2);
        doHardwareLayout();
        midiProcessor.resetAllLEDs();
        activateStandardLayers();
    }
    
    @Override
    protected void initNavigation() {
        final Clip cursorClip = getHost().createLauncherCursorClip(8, 128);
        final Clip arrangerClip = getHost().createArrangerCursorClip(8, 128);
        
        arrangerClip.exists().markInterested();
        final Track rootTrack = viewControl.getProject().getRootTrackGroup();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final ClipSceneCursor clipSceneCursor = viewControl.getClipSceneCursor();
        final NavigationState navigationState = viewControl.getNavigationState();
        
        viewControl.getApplication().panelLayout().addValueObserver(v -> currentLayoutType = LayoutType.toType(v));
        controlElements.getLeftNavButton().bind(
            mainLayer, () -> clipSceneCursor.navigateLeft(currentLayoutType),
            () -> !navigationState.isSceneNavMode());
        controlElements.getRightNavButton()
            .bind(mainLayer, () -> clipSceneCursor.navigateRight(currentLayoutType), navigationState::canGoTrackRight);
        controlElements.getUpNavButton()
            .bind(mainLayer, () -> clipSceneCursor.navigateUp(currentLayoutType), navigationState::canScrollSceneUp);
        controlElements.getDownNavButton().bind(
            mainLayer, () -> clipSceneCursor.navigateDown(currentLayoutType),
            navigationState::canScrollSceneDown);
        
        cursorClip.exists().markInterested();
        final ModeButton quantizeButton = controlElements.getButton(CcAssignment.QUANTIZE);
        sessionFocusLayer.bindPressed(quantizeButton.getHwButton(), () -> cursorClip.quantize(1.0));
        sessionFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
            quantizeButton.getLed());
        
        arrangeFocusLayer.bindPressed(quantizeButton.getHwButton(), () -> arrangerClip.quantize(1.0));
        arrangeFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
            quantizeButton.getLed());
        
        cursorTrack.canHoldNoteData().markInterested();
        cursorClip.exists().markInterested();
        
        final ModeButton clearButton = controlElements.getButton(CcAssignment.CLEAR);
        sessionFocusLayer.bindPressed(clearButton.getHwButton(), () -> cursorClip.clearSteps());
        sessionFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
            clearButton.getLed());
        
        arrangeFocusLayer.bindPressed(clearButton.getHwButton(), () -> arrangerClip.clearSteps());
        arrangeFocusLayer.bind(
            () -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
            clearButton.getLed());
        
        clearButton.getLed().isOn()
            .setValueSupplier(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get());
        
        final ModeButton knobPressed = controlElements.getKnobPressed();
        final ModeButton knobShiftPressed = controlElements.getKnobShiftPressed();
        mainLayer.bindPressed(knobPressed.getHwButton(), () -> clipSceneCursor.launch());
        mainLayer.bindPressed(knobShiftPressed.getHwButton(), () -> handle4DShiftPressed(rootTrack, cursorTrack));
    }
    
    @Override
    protected void initJogWheel() {
        final RelativeHardwareKnob fourKnob = controlElements.getFourDKnob();
        mainLayer.bind(fourKnob, midiProcessor.createIncAction(this::handleTransportScroll));
    }
    
    @Override
    public void exit() {
        midiProcessor.exit();
    }
    
    @Override
    public void flush() {
        midiProcessor.doFlush();
    }
    
    @Override
    protected void setUpChannelControl(final int index, final Track channel) {
        final HardwareButton selectButton = midiProcessor.createButton("SELECT_BUTTON", 0x42, index);
        mainLayer.bindPressed(
            selectButton, () -> {
                if (!channel.exists().get()) {
                    viewControl.insertInstrument();
                } else {
                    channel.selectInMixer();
                }
            });
        channel.exists().markInterested();
        
        channel.addIsSelectedInMixerObserver(v -> {
            midiProcessor.sendValueCommand(ValueCommand.SELECT, index, v);
        });
        channel.mute().addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.MUTE, index, v));
        channel.solo().addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.SOLO, index, v));
        channel.arm().addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.ARM, index, v));
        channel.isMutedBySolo()
            .addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.MUTED_BY_SOLO, index, v));
        channel.name().addValueObserver(name -> midiProcessor.sendTextCommand(TextCommand.NAME, index, name));
        
        channel.volume().displayedValue()
            .addValueObserver(valueText -> midiProcessor.sendTextCommand(TextCommand.VOLUME, index, valueText));
        
        channel.pan().displayedValue()
            .addValueObserver(value -> midiProcessor.sendTextCommand(TextCommand.PAN, index, value));
        
        channel.pan().value().addValueObserver(value -> midiProcessor.sendPanValue(index, (int) (value * 127)));
        
        channel.trackType().addValueObserver(v -> {
            final TrackType type = TrackType.toType(v);
            midiProcessor.sendValueCommand(ValueCommand.AVAILABLE, index, type.getId());
        });
        controlElements.getVolumeKnobs().get(index).addBindingWithSensitivity(channel.volume(), 0.025);
        controlElements.getPanKnobs().get(index).addBindingWithSensitivity(channel.pan(), 0.025);
        
        channel.isActivated().markInterested();
        channel.canHoldAudioData().markInterested();
        channel.canHoldNoteData().markInterested();
    }
    
    @Override
    protected void setUpChannelDisplayFeedback(final int index, final Track channel) {
    }
    
    
}
