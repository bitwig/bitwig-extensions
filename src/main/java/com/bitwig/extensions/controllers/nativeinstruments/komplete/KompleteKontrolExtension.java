package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SpecificPluginDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.TextCommand;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.ValueCommand;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.FocusMode;

public abstract class KompleteKontrolExtension extends ControllerExtension {
    static final int KOMPLETE_KONTROL_DEVICE_ID = 1315523403;
    static final String KOMPLETE_KONTROL_VST3_ID = "5653544E694B4B6B6F6D706C65746520";
    static final String KONTAKT_7_VST3_ID = "5653544E694B376B6F6E74616B742037";
    static final String KONTAKT_8_VST3_ID = "5653544E694B386B6F6E74616B742038";
    static final String MASCHINE_3_VST3_ID = "5653544E694D336D61736368696E6520";
    
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    
    protected HardwareSurface surface;
    protected TrackBank mixerTrackBank;
    protected Transport mTransport;
    final RelativeHardwareKnob[] volumeKnobs = new RelativeHardwareKnob[8];
    final RelativeHardwareKnob[] panKnobs = new RelativeHardwareKnob[8];
    private final List<RelativeHardwareControlBinding> knobBindings = new ArrayList<>();
    
    protected MidiProcessor midiProcessor;
    
    protected Layers layers;
    protected Layer mainLayer;
    protected Application application;
    
    protected NavigationState navigationState = new NavigationState();
    protected ClipSceneCursor clipSceneCursor;
    
    protected LayoutType currentLayoutType = LayoutType.LAUNCHER;
    protected Project project;
    protected ControlElements controlElements;
    
    protected Layer arrangeFocusLayer;
    protected Layer sessionFocusLayer;
    protected Layer navigationLayer;
    protected final boolean hasDeviceControl;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private static ControllerHost debugHost;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DATE_FORMATTER) + " > " + String.format(format, args));
        }
    }
    
    protected KompleteKontrolExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
        final boolean hasDeviceControl) {
        super(definition, host);
        this.hasDeviceControl = hasDeviceControl;
    }
    
    @Override
    public void init() {
        final ControllerHost host = getHost();
        debugHost = host;
        application = host.createApplication();
        surface = host.createHardwareSurface();
        midiProcessor = new MidiProcessor(host, surface);
        controlElements = new ControlElements(surface, midiProcessor);
        clipSceneCursor = new ClipSceneCursor(host, navigationState);
        midiProcessor.addModeListener(this::changeMode);
    }
    
    protected abstract void initNavigation();
    
    protected void setUpChannelControl(final int index, final Track channel) {
        final HardwareButton selectButton = midiProcessor.createButton("SELECT_BUTTON", 0x42, index);
        if (hasDeviceControl) {
            channel.color().addValueObserver((r, g, b) -> midiProcessor.sendColor(index, toColor(r, g, b)));
        }
        
        mainLayer.bindPressed(
            selectButton, () -> {
                if (!channel.exists().get()) {
                    application.createInstrumentTrack(-1);
                } else {
                    channel.selectInMixer();
                }
            });
        final HardwareButton muteButton = midiProcessor.createButton("MUTE_BUTTON", 0x43, index);
        mainLayer.bindPressed(muteButton, () -> channel.mute().toggle());
        final HardwareButton soloButton = midiProcessor.createButton("SOLO_BUTTON", 0x44, index);
        mainLayer.bindPressed(soloButton, () -> channel.solo().toggle());
        final HardwareButton armButton = midiProcessor.createButton("ARM_BUTTON", 0x45, index);
        mainLayer.bindPressed(armButton, () -> channel.arm().toggle());
        
        final HardwareButton shiftButton = controlElements.getShiftButton();
        
        mainLayer.bindPressed(shiftButton, () -> controlElements.getShiftHeld().set(true));
        mainLayer.bindReleased(shiftButton, () -> controlElements.getShiftHeld().set(false));
        channel.exists().markInterested();
        
        channel.addIsSelectedInMixerObserver(v -> midiProcessor.sendValueCommand(ValueCommand.SELECT, index, v));
        channel.mute().addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.MUTE, index, v));
        channel.solo().addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.SOLO, index, v));
        channel.arm().addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.ARM, index, v));
        channel.isMutedBySolo()
            .addValueObserver(v -> midiProcessor.sendValueCommand(ValueCommand.MUTED_BY_SOLO, index, v));
        
        channel.name().addValueObserver(name -> midiProcessor.sendTextCommand(TextCommand.NAME, index, name));
        
        channel.volume().displayedValue()
            .addValueObserver(valueText -> midiProcessor.sendTextCommand(TextCommand.VOLUME, index, valueText));
        
        setUpChannelDisplayFeedback(index, channel);
        channel.trackType().addValueObserver(v -> {
            final TrackType type = TrackType.toType(v);
            midiProcessor.sendValueCommand(ValueCommand.AVAILABLE, index, type.getId());
        });
        
        if (hasDeviceControl) {
            knobBindings.add(volumeKnobs[index].addBindingWithSensitivity(channel.volume(), 0.5));
            knobBindings.add(panKnobs[index].addBindingWithSensitivity(channel.pan(), 0.5));
            controlElements.getShiftHeld().addValueObserver(shift -> applyShift(shift));
        } else {
            volumeKnobs[index].addBindingWithSensitivity(channel.volume(), 0.02);
            panKnobs[index].addBindingWithSensitivity(channel.pan(), 0.02);
        }
        
        channel.isActivated().markInterested();
        channel.canHoldAudioData().markInterested();
        channel.canHoldNoteData().markInterested();
    }
    
    private void applyShift(final boolean shift) {
        final double sensitivity = shift ? 0.1 : 0.5;
        knobBindings.forEach(binding -> binding.setSensitivity(sensitivity));
    }
    
    private String toColor(final float r, final float g, final float b) {
        final int red = Math.round(r * 255);
        final int green = Math.round(g * 255);
        final int blue = Math.round(b * 255);
        return "#%02X%02X%02X".formatted(red, green, blue);
    }
    
    protected abstract void setUpChannelDisplayFeedback(final int index, final Track channel);
    
    @Override
    public void exit() {
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    public Layers getLayers() {
        return layers;
    }
    
    public HardwareSurface getSurface() {
        return surface;
    }
    
    protected void initJogWheel() {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        final RelativeHardwareKnob fourDKnob = surface.createRelativeHardwareKnob("4D_WHEEL_PLUGIN_MODE");
        fourDKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x34, 128));
        fourDKnob.setStepSize(1 / 128.0);
        
        final HardwareActionBindable incAction = getHost().createAction(() -> mTransport.fastForward(), () -> "+");
        final HardwareActionBindable decAction = getHost().createAction(() -> mTransport.rewind(), () -> "-");
        fourDKnob.addBinding(getHost().createRelativeHardwareControlStepTarget(incAction, decAction));
        
        final RelativeHardwareKnob fourDKnobMixer = surface.createRelativeHardwareKnob("4D_WHEEL_MIX_MODE");
        fourDKnobMixer.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x64, 4096));
        fourDKnobMixer.setStepSize(1 / 128.0);
        
        final HardwareActionBindable incMixAction = getHost().createAction(
            () -> {
            }, () -> "+");
        final HardwareActionBindable decMixAction = getHost().createAction(
            () -> {
            }, () -> "-");
        fourDKnobMixer.addBinding(getHost().createRelativeHardwareControlStepTarget(incMixAction, decMixAction));
    }
    
    protected void setUpSliders() {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("VOLUME_KNOB" + i);
            volumeKnobs[i] = knob;
            knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x50 + i, 128));
            knob.setStepSize(1 / 128.0);
            
            final RelativeHardwareKnob panKnob = surface.createRelativeHardwareKnob("PAN_KNOB" + i);
            panKnobs[i] = panKnob;
            panKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x58 + i, 128));
            panKnob.setStepSize(1 / 128.0);
        }
    }
    
    protected void bindMacroControl(final PinnableCursorDevice device, final MidiIn midiIn) {
        final CursorRemoteControlsPage remote = device.createCursorRemoteControlsPage(8);
        for (int i = 0; i < 8; i++) {
            final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("MACRO_" + i);
            knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 14 + i));
            final RemoteControl parameter = remote.getParameter(i);
            parameter.setIndication(true);
            mainLayer.bind(knob, parameter);
        }
    }
    
    protected void createKompleteKontrolDeviceKompleteKontrol(final PinnableCursorDevice cursorDevice) {
        final SpecificPluginDevice kompleteKontrolVst3Id =
            cursorDevice.createSpecificVst3Device(KOMPLETE_KONTROL_VST3_ID);
        final Parameter kompleteKontrolVst3InstId = kompleteKontrolVst3Id.createParameter(0);
        kompleteKontrolVst3InstId.name().addValueObserver(midiProcessor::updateKompleteKontrolInstance);
        
        final SpecificPluginDevice kompleteKontrolPluginVst2 =
            cursorDevice.createSpecificVst2Device(KOMPLETE_KONTROL_DEVICE_ID);
        final Parameter kompleteKontrolVst2InstId = kompleteKontrolPluginVst2.createParameter(0);
        kompleteKontrolVst2InstId.markInterested();
        kompleteKontrolVst2InstId.name().markInterested();
        kompleteKontrolVst2InstId.exists().markInterested();
        kompleteKontrolVst2InstId.name().addValueObserver(midiProcessor::updateKompleteKontrolInstance);
    }
    
    protected void createKontaktDeviceKompleteKontrol(final PinnableCursorDevice cursorDevice) {
        final SpecificPluginDevice kontaktVst3Id = cursorDevice.createSpecificVst3Device(KONTAKT_7_VST3_ID);
        final Parameter instId = kontaktVst3Id.createParameter(2048);
        instId.name().addValueObserver(midiProcessor::updateKompleteKontrolInstance);
    }
    
    protected void createKontakt8DeviceKompleteKontrol(final PinnableCursorDevice cursorDevice) {
        final SpecificPluginDevice kontaktVst3Id = cursorDevice.createSpecificVst3Device(KONTAKT_8_VST3_ID);
        final Parameter instId = kontaktVst3Id.createParameter(2048);
        instId.name().addValueObserver(midiProcessor::updateKompleteKontrolInstance);
    }
    
    protected void createMaschineDeviceKompleteKontrol(final PinnableCursorDevice cursorDevice) {
        final SpecificPluginDevice kontaktVst3Id = cursorDevice.createSpecificVst3Device(MASCHINE_3_VST3_ID);
        final Parameter instId = kontaktVst3Id.createParameter(128);
        instId.name().addValueObserver(midiProcessor::updateKompleteKontrolInstance);
    }
    
    private void changeMode(final int mode) {
        if (mode == 0) {
            navigationLayer.activate();
            controlElements.updateLights();
        } else {
            navigationLayer.deactivate();
        }
    }
    
    protected void initTrackBank() {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        initNavigation();
        
        final CursorTrack cursorTrack = clipSceneCursor.getCursorTrack();
        final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
        createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
        createKontaktDeviceKompleteKontrol(cursorDevice);
        createKontakt8DeviceKompleteKontrol(cursorDevice);
        createMaschineDeviceKompleteKontrol(cursorDevice);
        mixerTrackBank = getHost().createTrackBank(8, 0, 1);
        mixerTrackBank.setSkipDisabledItems(true);
        mixerTrackBank.canScrollChannelsDown().markInterested();
        mixerTrackBank.canScrollChannelsUp().markInterested();
        mixerTrackBank.followCursorTrack(cursorTrack);
        mixerTrackBank.setChannelScrollStepSize(8);
        
        mainLayer.bindPressed(controlElements.getMuteSelectedButton(), cursorTrack.mute().toggleAction());
        mainLayer.bindPressed(controlElements.getSoloSelectedButton(), cursorTrack.solo().toggleAction());
        
        navigationLayer.bindPressed(controlElements.getTrackNavLeftButton(), () -> mixerTrackBank.scrollBy(-8));
        navigationLayer.bindPressed(controlElements.getTrackRightNavButton(), () -> mixerTrackBank.scrollBy(8));
        
        navigationLayer.bind(mixerTrackBank.canScrollChannelsUp(), controlElements.getTrackNavRightButtonLight());
        navigationLayer.bind(mixerTrackBank.canScrollChannelsDown(), controlElements.getTrackNavLeftButtonLight());
        
        for (int i = 0; i < 8; i++) {
            setUpChannelControl(i, mixerTrackBank.getItemAt(i));
        }
    }
    
    public void setUpTransport() {
        final DocumentState documentState = getHost().getDocumentState();
        final SettableEnumValue focusMode = documentState.getEnumSetting(
            "Focus", //
            "Recording/Automation",
            new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            FocusMode.ARRANGER.getDescriptor());
        final ModeButton recButton = new ModeButton(midiProcessor, "REC_BUTTON", CcAssignment.REC);
        final ModeButton autoButton = new ModeButton(midiProcessor, "AUTO_BUTTON", CcAssignment.AUTO);
        final ModeButton countInButton = new ModeButton(midiProcessor, "COUNTIN_BUTTON", CcAssignment.COUNT_IN);
        focusMode.markInterested();
        
        arrangeFocusLayer.bindToggle(recButton.getHwButton(), mTransport.isArrangerRecordEnabled());
        arrangeFocusLayer.bindToggle(autoButton.getHwButton(), mTransport.isArrangerAutomationWriteEnabled());
        arrangeFocusLayer.bindToggle(countInButton.getHwButton(), mTransport.isArrangerOverdubEnabled());
        
        sessionFocusLayer.bindToggle(recButton.getHwButton(), mTransport.isClipLauncherOverdubEnabled());
        sessionFocusLayer.bindToggle(autoButton.getHwButton(), mTransport.isClipLauncherAutomationWriteEnabled());
        sessionFocusLayer.bindToggle(countInButton.getHwButton(), mTransport.isClipLauncherOverdubEnabled());
        
        focusMode.addValueObserver(newValue -> {
            final FocusMode newMode = FocusMode.toMode(newValue);
            switch (newMode) {
                case ARRANGER:
                    sessionFocusLayer.deactivate();
                    arrangeFocusLayer.activate();
                    break;
                case LAUNCHER:
                    arrangeFocusLayer.deactivate();
                    sessionFocusLayer.activate();
                    break;
                default:
                    break;
            }
        });
        
        final ModeButton playButton = new ModeButton(midiProcessor, "PLAY_BUTTON", CcAssignment.PLAY);
        mainLayer.bindToggle(playButton.getHwButton(), mTransport.isPlaying());
        mainLayer.bind(mTransport.isPlaying(), playButton.getLed());
        final ModeButton restartButton = new ModeButton(midiProcessor, "RESTART_BUTTON", CcAssignment.RESTART);
        mainLayer.bindPressed(restartButton.getHwButton(), () -> mTransport.launchFromPlayStartPosition());
        final ModeButton stopButton = new ModeButton(midiProcessor, "STOP_BUTTON", CcAssignment.STOP);
        mainLayer.bindPressed(stopButton.getHwButton(), mTransport.stopAction());
        
        final ModeButton loopButton = new ModeButton(midiProcessor, "LOOP_BUTTON", CcAssignment.LOOP);
        mainLayer.bindToggle(loopButton.getHwButton(), mTransport.isArrangerLoopEnabled());
        
        final ModeButton metroButton = new ModeButton(midiProcessor, "METRO_BUTTON", CcAssignment.METRO);
        mainLayer.bindToggle(metroButton.getHwButton(), mTransport.isMetronomeEnabled());
        final ModeButton tapTempoButton = new ModeButton(midiProcessor, "TAP_BUTTON", CcAssignment.TAP_TEMPO);
        mainLayer.bindPressed(tapTempoButton.getHwButton(), mTransport::tapTempo);
        tapTempoButton.bindLightToPressed();
        
        final ModeButton undoButton = new ModeButton(midiProcessor, "UNDO_BUTTON", CcAssignment.UNDO);
        mainLayer.bindPressed(undoButton.getHwButton(), () -> application.undo());
        undoButton.getLed().isOn().setValue(true); // As long as there is no canUndo
        
        final ModeButton redoButton = new ModeButton(midiProcessor, "REDO_BUTTON", CcAssignment.REDO);
        mainLayer.bindPressed(redoButton.getHwButton(), () -> application.redo());
        redoButton.getLed().isOn().setValue(true);
    }
    
    
}
