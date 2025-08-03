package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareElement;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SpecificPluginDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.binding.KnobParameterBinding;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ControlElements;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ModeButton;
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
    private final List<RelativeHardwareControlBinding> knobBindings = new ArrayList<>();
    
    protected MidiProcessor midiProcessor;
    
    protected Layers layers;
    protected Layer mainLayer;
    
    protected ViewControl viewControl;
    
    protected LayoutType currentLayoutType = LayoutType.LAUNCHER;
    protected ControlElements controlElements;
    
    protected SettableEnumValue focusMode;
    protected Layer arrangeFocusLayer;
    protected Layer sessionFocusLayer;
    protected Layer navigationLayer;
    private CursorRemoteControlsPage genericRemotes;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private static ControllerHost debugHost;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DATE_FORMATTER) + " > " + String.format(format, args));
        }
    }
    
    protected KompleteKontrolExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        final ControllerHost host = getHost();
        debugHost = host;
        surface = host.createHardwareSurface();
        midiProcessor = new MidiProcessor(host, surface);
        viewControl = new ViewControl(host);
        controlElements = new ControlElements(surface, midiProcessor, hasSwitchedNavigationMapping());
        midiProcessor.addModeListener(this::changeMode);
    }
    
    protected boolean hasSwitchedNavigationMapping() {
        return false;
    }
    
    protected boolean hasDeviceControl() {
        return false;
    }
    
    protected void activateStandardLayers() {
        mainLayer.activate();
        navigationLayer.activate();
        updateFocusMode(focusMode.get());
    }
    
    protected abstract void initNavigation();
    
    protected void setUpChannelControl(final int index, final Track channel) {
        final HardwareButton selectButton = midiProcessor.createButton("SELECT_BUTTON", 0x42, index);
        if (hasDeviceControl()) {
            channel.color().addValueObserver((r, g, b) -> midiProcessor.sendColor(index, toColor(r, g, b)));
        }
        
        mainLayer.bindPressed(
            selectButton, () -> {
                if (!channel.exists().get()) {
                    viewControl.insertInstrument();
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
        
        final List<RelativeHardwareKnob> volumeKnobs = controlElements.getVolumeKnobs();
        final List<RelativeHardwareKnob> panKnobs = controlElements.getPanKnobs();
        if (hasDeviceControl()) {
            knobBindings.add(volumeKnobs.get(index)
                .addBindingWithSensitivity(channel.volume(), KnobParameterBinding.BASE_SENSITIVITY));
            knobBindings.add(
                panKnobs.get(index).addBindingWithSensitivity(channel.pan(), KnobParameterBinding.BASE_SENSITIVITY));
            controlElements.getShiftHeld().addValueObserver(this::applyShift);
        } else {
            volumeKnobs.get(index).addBindingWithSensitivity(channel.volume(), 0.02);
            panKnobs.get(index).addBindingWithSensitivity(channel.pan(), 0.02);
        }
        channel.isActivated().markInterested();
        channel.canHoldAudioData().markInterested();
        channel.canHoldNoteData().markInterested();
    }
    
    private void applyShift(final boolean shift) {
        final double sensitivity =
            shift ? KnobParameterBinding.FINE_SENSITIVITY : KnobParameterBinding.BASE_SENSITIVITY;
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
        midiProcessor.exit();
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    protected void initJogWheel() {
        final RelativeHardwareKnob mixer4DKnob = controlElements.getFourDKnobMixer();
        mainLayer.bind(mixer4DKnob, midiProcessor.createIncAction(this::handleTransportScroll));
        final RelativeHardwareKnob fourKnob = controlElements.getFourDKnob();
        mainLayer.bind(fourKnob, midiProcessor.createIncAction(this::navigateRemotes));
    }
    
    protected void handleTransportScroll(final int inc) {
        if (inc > 0) {
            viewControl.getTransport().fastForward();
        } else {
            viewControl.getTransport().rewind();
        }
    }
    
    protected void bindMacroControl(final MidiIn midiIn) {
        final PinnableCursorDevice device = viewControl.getCursorDevice();
        genericRemotes = device.createCursorRemoteControlsPage(8);
        for (int i = 0; i < 8; i++) {
            final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("MACRO_" + i);
            knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 14 + i));
            final RemoteControl parameter = genericRemotes.getParameter(i);
            parameter.setIndication(true);
            mainLayer.bind(knob, parameter);
        }
    }
    
    public void navigateRemotes(final int inc) {
        if (genericRemotes == null) {
            return;
        }
        if (inc > 0) {
            genericRemotes.selectNextPage(false);
        } else {
            genericRemotes.selectPreviousPage(false);
        }
    }
    
    protected void handle4DShiftPressed(final Track rootTrack, final CursorTrack cursorTrack) {
        if (viewControl.getNavigationState().isSceneNavMode()) {
            rootTrack.stop();
        } else {
            cursorTrack.stop();
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
        initNavigation();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final TrackBank mixerTrackBank = viewControl.getMixerTrackBank();
        createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
        createKontaktDeviceKompleteKontrol(cursorDevice);
        createKontakt8DeviceKompleteKontrol(cursorDevice);
        createMaschineDeviceKompleteKontrol(cursorDevice);
        
        mainLayer.bindPressed(controlElements.getMuteSelectedButton(), cursorTrack.mute().toggleAction());
        mainLayer.bindPressed(controlElements.getSoloSelectedButton(), cursorTrack.solo().toggleAction());
        
        controlElements.getTrackNavLeftButton()
            .bind(navigationLayer, () -> mixerTrackBank.scrollBy(-8), mixerTrackBank.canScrollChannelsUp());
        controlElements.getTrackRightNavButton()
            .bind(navigationLayer, () -> mixerTrackBank.scrollBy(8), mixerTrackBank.canScrollChannelsDown());
        
        for (int i = 0; i < 8; i++) {
            setUpChannelControl(i, mixerTrackBank.getItemAt(i));
        }
    }
    
    public void setUpTransport() {
        final Transport transport = viewControl.getTransport();
        final DocumentState documentState = getHost().getDocumentState();
        focusMode = documentState.getEnumSetting(
            "Focus", //
            "Recording/Automation",
            new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            FocusMode.ARRANGER.getDescriptor());
        final ModeButton recButton = controlElements.getButton(CcAssignment.REC);
        final ModeButton autoButton = controlElements.getButton(CcAssignment.AUTO);
        final ModeButton countInButton = controlElements.getButton(CcAssignment.COUNT_IN);
        
        arrangeFocusLayer.bindToggle(recButton.getHwButton(), transport.isArrangerRecordEnabled());
        arrangeFocusLayer.bindToggle(autoButton.getHwButton(), transport.isArrangerAutomationWriteEnabled());
        arrangeFocusLayer.bindToggle(countInButton.getHwButton(), transport.isArrangerOverdubEnabled());
        
        sessionFocusLayer.bindToggle(recButton.getHwButton(), transport.isClipLauncherOverdubEnabled());
        sessionFocusLayer.bindToggle(autoButton.getHwButton(), transport.isClipLauncherAutomationWriteEnabled());
        sessionFocusLayer.bindToggle(countInButton.getHwButton(), transport.isClipLauncherOverdubEnabled());
        
        focusMode.addValueObserver(this::updateFocusMode);
        focusMode.markInterested();
        
        final ModeButton playButton = controlElements.getButton(CcAssignment.PLAY);
        mainLayer.bindToggle(playButton.getHwButton(), transport.isPlaying());
        mainLayer.bind(transport.isPlaying(), playButton.getLed());
        final ModeButton restartButton = controlElements.getButton(CcAssignment.RESTART);
        mainLayer.bindPressed(restartButton.getHwButton(), transport::launchFromPlayStartPosition);
        final ModeButton stopButton = controlElements.getButton(CcAssignment.STOP);
        mainLayer.bindPressed(stopButton.getHwButton(), transport.stopAction());
        
        final ModeButton loopButton = controlElements.getButton(CcAssignment.LOOP);
        mainLayer.bindToggle(loopButton.getHwButton(), transport.isArrangerLoopEnabled());
        
        final ModeButton metroButton = controlElements.getButton(CcAssignment.METRO);
        mainLayer.bindToggle(metroButton.getHwButton(), transport.isMetronomeEnabled());
        
        final ModeButton tapTempoButton = controlElements.getButton(CcAssignment.TAP_TEMPO);
        mainLayer.bindPressed(tapTempoButton.getHwButton(), transport::tapTempo);
        tapTempoButton.bindLightToPressed();
        
        final ModeButton undoButton = controlElements.getButton(CcAssignment.UNDO);
        final Application application = viewControl.getApplication();
        mainLayer.bindPressed(undoButton.getHwButton(), application::undo);
        undoButton.getLed().isOn().setValue(true); // As long as there is no canUndo
        
        final ModeButton redoButton = controlElements.getButton(CcAssignment.REDO);
        mainLayer.bindPressed(redoButton.getHwButton(), application::redo);
        redoButton.getLed().isOn().setValue(true);
    }
    
    protected void updateFocusMode(final String newValue) {
        final FocusMode newMode = FocusMode.toMode(newValue);
        sessionFocusLayer.setIsActive(newMode == FocusMode.LAUNCHER);
        arrangeFocusLayer.setIsActive(newMode == FocusMode.ARRANGER);
    }
    
    protected void doHardwareLayout() {
        final int colOff = 12;
        final int colTopOf = 14;
        final int controlTop = 7;
        for (int i = 0; i < 8; i++) {
            configureElement("SELECT_BUTTON_%d".formatted(i), "S%d".formatted(i + 1), 62 + i * colOff, 1, 9.5, 4);
            final HardwareElement volKnob = surface.hardwareElementWithId("VOLUME_KNOB_%d".formatted(i));
            volKnob.setLabel("Level %d".formatted(i + 1));
            volKnob.setBounds(62 + i * colOff, controlTop, 10, 10);
            volKnob.setLabelPosition(RelativePosition.BELOW);
            final HardwareElement panKnob = surface.hardwareElementWithId("PAN_KNOB_%d".formatted(i));
            panKnob.setLabel("Pan %d".formatted(i + 1));
            panKnob.setBounds(62 + i * colOff, controlTop + colTopOf, 10, 10);
            panKnob.setLabelPosition(RelativePosition.BELOW);
            if (!hasDeviceControl()) {
                configureElement(
                    "MACRO_%d".formatted(i), "Macro %d".formatted(i + 1), 62 + i * colOff,
                    controlTop + colTopOf * 2, 10).setLabelPosition(RelativePosition.BELOW);
            }
        }
        
        configureElement("LEFT_NAV_BUTTON", "<<", 160, 20.0, 6.0);
        configureElement("RIGHT_NAV_BUTTON", ">>", 174, 20.0, 6.0);
        configureElement("UP_NAV_BUTTON", "up", 167, 13.0, 6.0);
        configureElement("DOWN_NAV_BUTTON", "dn", 167, 27, 6.0);
        
        configureElement("KNOB4D_PRESSED", "4D+P", 167, 20.0, 6.0);
        configureElement("KNOB4D_PRESSED_SHIFT", "4D+S+P", 181, 20, 6.0);
        
        configureElement("4D_WHEEL_PLUGIN_MODE", "4D Plugin", 188, 20.0, 10.0).setLabelPosition(RelativePosition.BELOW);
        configureElement("4D_WHEEL_MIX_MODE", "4d Mix", 188, 34, 10.0).setLabelPosition(RelativePosition.BELOW);
        
        
        final Color labelColor = Color.fromRGB255(0, 240, 156);
        configureElement("TRACK_LEFT_NAV_BUTTON", "<<", 40, 20, 6).setLabelColor(labelColor);
        configureElement("TRACK_RIGHT_NAV_BUTTON", ">>", 48, 20, 6).setLabelColor(labelColor);
        configureElement("MUTE_SELECTED_BUTTON", "M", 40, 10, 6).setLabelColor(Color.fromHex("ffff00"));
        configureElement("SOLO_SELECTED_BUTTON", "S", 48, 10, 6).setLabelColor(Color.fromHex("e8eb34"));
        
        final int bw = 10;
        final int bh = 6;
        final int top = 10;
        final int left = 1;
        final int bhHlf = bh / 2;
        
        configureElement("PLAY_BUTTON", ">", left, top + bh + 1, bw, bhHlf);
        configureElement("RESTART_BUTTON", "Restart", left, top + bh + 1 + bhHlf, bw, bhHlf);
        configureElement("REC_BUTTON", "Rec", left + (bw + 1), top + bh + 1, bw, bhHlf);
        configureElement("COUNTIN_BUTTON", "Count-in", left + (bw + 1), top + bh + 1 + bhHlf, bw, bhHlf).setLabelColor(
            labelColor);
        
        configureElement("STOP_BUTTON", "Stop", left + (bw + 1) * 2, top + bh + 1, bw, bh);
        configureElement("LOOP_BUTTON", "LOOP", left, top, bw, bh);
        configureElement("AUTO_BUTTON", "Auto", left + (bw + 1), top, bw, bh);
        configureElement("METRO_BUTTON", "Metro", left + (bw + 1) * 2, top, bw, bhHlf);
        configureElement("TAP_BUTTON", "Tap", left + (bw + 1) * 2, top + bhHlf, bw, bhHlf);
        
        configureElement("QUANTIZE_BUTTON", "Quant", left, top + (bh + 1) * 3, bw, bh).setLabelColor(labelColor);
        configureElement("CLEAR_BUTTON", "Clear", left + (bw + 1), top + (bh + 1) * 3, bw, bh).setLabelColor(
            labelColor);
        configureElement("UNDO_BUTTON", "Undo", left + (bw + 1) * 3, top + (bh + 1) * 3, bw, bhHlf).setLabelColor(
            labelColor);
        configureElement(
            "REDO_BUTTON", "Redo", left + (bw + 1) * 3, top + (bh + 1) * 3 + bhHlf, bw, bhHlf).setLabelColor(
            labelColor);
    }
    
    private HardwareElement configureElement(final String id, final String label, final double x, final double y,
        final double w, final double h) {
        final HardwareElement element = surface.hardwareElementWithId(id);
        
        element.setBounds(x, y, w, h);
        element.setLabel(label);
        return element;
    }
    
    private HardwareElement configureElement(final String id, final String label, final double x, final double y,
        final double size) {
        return configureElement(id, label, x, y, size, size);
    }
    
}
