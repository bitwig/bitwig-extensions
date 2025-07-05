package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.TextCommand;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.ValueCommand;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolAExtension extends KompleteKontrolExtension {
    
    public KompleteKontrolAExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host, false);
    }
    
    @Override
    public void init() {
        super.init();
        midiProcessor.intoDawMode(0x3);
        final ControllerHost host = getHost();
        
        surface.setPhysicalSize(200, 100);
        
        layers = new Layers(this);
        mainLayer = new Layer(layers, "Main");
        arrangeFocusLayer = new Layer(layers, "ArrangeFocus");
        sessionFocusLayer = new Layer(layers, "SessionFocus");
        navigationLayer = new Layer(layers, "NavigationLayer");
        
        project = host.getProject();
        mTransport = host.createTransport();
        
        setUpSliders();
        final MidiIn midiIn2 = host.getMidiInPort(1);
        final NoteInput noteInput =
            midiIn2.createNoteInput(
                "MIDI", "80????", "90????", "D0????", "E0????", "B001??", "B040??", "B042??",
                "B1????");
        noteInput.setShouldConsumeEvents(true);
        
        initTrackBank();
        setUpTransport();
        initJogWheel();
        
        final PinnableCursorDevice cursorDevice = clipSceneCursor.getCursorTrack().createCursorDevice();
        bindMacroControl(cursorDevice, midiIn2);
        doHardwareLayout();
        midiProcessor.resetAllLEDs();
        mainLayer.activate();
        navigationLayer.activate();
    }
    
    @Override
    protected void setUpSliders() {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("VOLUME_KNOB" + i);
            volumeKnobs[i] = knob;
            knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x50 + i, 128));
            knob.setStepSize(1 / 1024.0);
            
            final RelativeHardwareKnob panKnob = surface.createRelativeHardwareKnob("PAN_KNOB" + i);
            panKnobs[i] = panKnob;
            panKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x58 + i, 128));
            panKnob.setStepSize(1 / 1024.0);
        }
    }
    
    @Override
    protected void initNavigation() {
        final Clip cursorClip = getHost().createLauncherCursorClip(8, 128);
        final Clip arrangerClip = getHost().createArrangerCursorClip(8, 128);
        
        arrangerClip.exists().markInterested();
        final Track rootTrack = project.getRootTrackGroup();
        final CursorTrack cursorTrack = clipSceneCursor.getCursorTrack();
        
        application.panelLayout().addValueObserver(v -> currentLayoutType = LayoutType.toType(v));
        final MidiIn midiIn = midiProcessor.getMidiIn();
        final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
        leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));
        final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
        rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));
        final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
        upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));
        final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
        downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));
        
        mainLayer.bindPressed(leftNavButton, () -> clipSceneCursor.doNavigateRight(currentLayoutType));
        mainLayer.bindPressed(rightNavButton, () -> clipSceneCursor.doNavigateLeft(currentLayoutType));
        mainLayer.bindPressed(upNavButton, () -> clipSceneCursor.doNavigateUp(currentLayoutType));
        mainLayer.bindPressed(downNavButton, () -> clipSceneCursor.doNavigateDown(currentLayoutType));
        
        cursorClip.exists().markInterested();
        final ModeButton quantizeButton = new ModeButton(midiProcessor, "QUANTIZE_BUTTON", CcAssignment.QUANTIZE);
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
        
        final ModeButton clearButton = new ModeButton(midiProcessor, "CLEAR_BUTTON", CcAssignment.CLEAR);
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
        
        final ModeButton knobPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED", CcAssignment.PRESS_4D_KNOB);
        mainLayer.bindPressed(knobPressed.getHwButton(), () -> clipSceneCursor.launch());
        final ModeButton knobShiftPressed =
            new ModeButton(midiProcessor, "KNOB4D_PRESSED_SHIFT", CcAssignment.PRESS_4D_KNOB_SHIFT);
        mainLayer.bindPressed(
            knobShiftPressed.getHwButton(), () -> {
                if (navigationState.isSceneNavMode()) {
                    rootTrack.stop();
                } else {
                    cursorTrack.stop();
                }
            });
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
                    application.createInstrumentTrack(-1);
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
        volumeKnobs[index].addBindingWithSensitivity(channel.volume(), 0.025);
        panKnobs[index].addBindingWithSensitivity(channel.pan(), 0.025);
        
        channel.isActivated().markInterested();
        channel.canHoldAudioData().markInterested();
        channel.canHoldNoteData().markInterested();
    }
    
    @Override
    protected void setUpChannelDisplayFeedback(final int index, final Track channel) {
    }
    
    private void doHardwareLayout() {
        surface.hardwareElementWithId("VOLUME_KNOB0").setBounds(62.75, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB0").setBounds(62.25, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB1").setBounds(75.75, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB1").setBounds(76.25, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB2").setBounds(88.25, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB2").setBounds(89.25, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB3").setBounds(101.5, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB3").setBounds(104.5, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB4").setBounds(115.25, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB4").setBounds(115.5, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB5").setBounds(127.25, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB5").setBounds(127.5, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB6").setBounds(138.0, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB6").setBounds(138.75, 19.0, 10.0, 10.0);
        surface.hardwareElementWithId("VOLUME_KNOB7").setBounds(148.0, 7.5, 10.0, 10.0);
        surface.hardwareElementWithId("PAN_KNOB7").setBounds(149.25, 19.0, 10.0, 10.0);
        
        surface.hardwareElementWithId("LEFT_NAV_BUTTON").setBounds(160.75, 20.0, 6.0, 6.0);
        surface.hardwareElementWithId("RIGHT_NAV_BUTTON").setBounds(174.25, 20.0, 5.5, 6.0);
        surface.hardwareElementWithId("UP_NAV_BUTTON").setBounds(167.25, 14.25, 6.0, 5.0);
        surface.hardwareElementWithId("DOWN_NAV_BUTTON").setBounds(167.25, 26.5, 6.5, 6.0);
        surface.hardwareElementWithId("KNOB4D_PRESSED").setBounds(167.25, 20.0, 6.5, 3.0);
        surface.hardwareElementWithId("KNOB4D_PRESSED_SHIFT").setBounds(168.0, 23.75, 4.75, 2.0);
        
        surface.hardwareElementWithId("QUANTIZE_BUTTON").setBounds(26.25, 13.5, 7.25, 2.0);
        surface.hardwareElementWithId("CLEAR_BUTTON").setBounds(35.25, 27.75, 7.5, 2.0);
        surface.hardwareElementWithId("TRACK_LEFT_NAV_BUTTON").setBounds(48.75, 25.75, 2.25, 3.0);
        surface.hardwareElementWithId("TRACK_RIGHT_NAV_BUTTON").setBounds(52.75, 25.5, 3.25, 3.5);
        surface.hardwareElementWithId("MUTE_SELECTED_BUTTON").setBounds(48.75, 29.0, 2.5, 2.75);
        surface.hardwareElementWithId("SOLO_SELECTED_BUTTON").setBounds(52.75, 29.25, 3.25, 2.5);
        surface.hardwareElementWithId("SELECT_BUTTON_0").setBounds(60.75, 17.25, 9.5, 4.25);
        surface.hardwareElementWithId("SELECT_BUTTON_1").setBounds(72.25, 17.25, 10.0, 4.0);
        surface.hardwareElementWithId("SELECT_BUTTON_2").setBounds(84.25, 17.25, 10.0, 4.0);
        surface.hardwareElementWithId("SELECT_BUTTON_3").setBounds(96.25, 17.25, 10.0, 4.0);
        surface.hardwareElementWithId("SELECT_BUTTON_4").setBounds(108.25, 17.25, 10.0, 4.0);
        surface.hardwareElementWithId("SELECT_BUTTON_5").setBounds(120.25, 17.25, 10.0, 4.0);
        surface.hardwareElementWithId("SELECT_BUTTON_6").setBounds(132.25, 17.25, 10.0, 4.0);
        surface.hardwareElementWithId("SELECT_BUTTON_7").setBounds(144.25, 17.25, 9.25, 4.0);
        surface.hardwareElementWithId("REC_BUTTON").setBounds(26.75, 25.0, 6.75, 4.0);
        surface.hardwareElementWithId("AUTO_BUTTON").setBounds(26.0, 15.75, 7.75, 2.0);
        surface.hardwareElementWithId("COUNTIN_BUTTON").setBounds(7.75, 57.75, 25.5, 10.0);
        surface.hardwareElementWithId("PLAY_BUTTON").setBounds(18.0, 25.0, 7.0, 2.0);
        surface.hardwareElementWithId("RESTART_BUTTON").setBounds(18.0, 27.0, 7.0, 2.25);
        surface.hardwareElementWithId("STOP_BUTTON").setBounds(35.25, 24.75, 7.0, 2.5);
        surface.hardwareElementWithId("LOOP_BUTTON").setBounds(18.0, 20.75, 7.0, 2.75);
        surface.hardwareElementWithId("METRO_BUTTON").setBounds(26.75, 20.75, 7.0, 3.0);
        surface.hardwareElementWithId("TAP_BUTTON").setBounds(35.25, 20.75, 7.0, 3.25);
        surface.hardwareElementWithId("UNDO_BUTTON").setBounds(18.0, 13.5, 7.0, 2.0);
        surface.hardwareElementWithId("REDO_BUTTON").setBounds(18.0, 15.75, 7.0, 1.75);
        surface.hardwareElementWithId("4D_WHEEL_PLUGIN_MODE").setBounds(180.0, 13.5, 10.0, 10.0);
        surface.hardwareElementWithId("4D_WHEEL_MIX_MODE").setBounds(180.25, 24.5, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_0").setBounds(61.75, 31.5, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_1").setBounds(73.75, 31.5, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_2").setBounds(85.75, 31.5, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_3").setBounds(97.75, 31.5, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_4").setBounds(112.25, 30.75, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_5").setBounds(124.25, 30.75, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_6").setBounds(136.25, 30.75, 10.0, 10.0);
        surface.hardwareElementWithId("MACRO_7").setBounds(148.25, 30.75, 10.0, 10.0);
    }
}
