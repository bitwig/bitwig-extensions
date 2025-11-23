package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.FocusSlot;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchpadDeviceConfig;
import com.bitwig.extensions.controllers.novation.commonsmk3.LpHwElements;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.OverviewLayer;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.DrumLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.NotePlayingLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.SceneLaunchLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.SessionLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.TrackControlLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.TrackModeLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers.DeviceSliderLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers.PanSliderLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers.SendsSliderLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers.SliderLayer;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers.VolumeSliderLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class LaunchpadProMk3ControllerExtension extends ControllerExtension implements ModeHandler {
    
    private Transport transport;
    
    // Main Grid Buttons counting from top to bottom
    
    private ModifierStates modifierStates;
    private Layer mainLayer;
    private Layer shiftLayer;
    private final HashMap<ControlMode, SliderLayer> controlMaps = new HashMap<>();
    
    private TrackModeLayer trackModeLayer;
    private OverviewLayer overviewLayer;
    private ViewCursorControl viewControl;
    private SysExHandler sysExHandler;
    private HardwareSurface surface;
    private LpBaseMode mainMode = LpBaseMode.SESSION;
    private int mainModePage = 0;
    private LpProHwElements hwElements;
    private DrumLayer drumPadLayer;
    private boolean drumModeActive = false;
    
    private static ControllerHost debugHost;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(String.format(format, args));
        }
    }
    
    protected LaunchpadProMk3ControllerExtension(final ControllerExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        Context.registerCounter(getHost());
        final Context diContext = new Context(this, ViewCursorControl.class.getPackage());
        diContext.registerService(ModeHandler.class, this);
        
        final ControllerHost host = diContext.getService(ControllerHost.class);
        surface = diContext.getService(HardwareSurface.class);
        transport = diContext.getService(Transport.class);
        
        final MidiIn midiIn = host.getMidiInPort(0);
        final MidiIn midiIn2 = host.getMidiInPort(1);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn2.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi1);
        
        final MidiOut midiOut = host.getMidiOutPort(0);
        final MidiProcessor midiProcessor = new MidiProcessor(
            host, midiIn, midiOut,
            new LaunchpadDeviceConfig("LaunchPadProMk3", 0x0E, 0xB4, 0xB5, false));
        diContext.registerService(MidiProcessor.class, midiProcessor);
        
        midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "D?????");
        
        viewControl = diContext.getService(ViewCursorControl.class);
        sysExHandler = diContext.create(SysExHandler.class);
        
        modifierStates = diContext.getService(ModifierStates.class);
        hwElements = diContext.create(LpProHwElements.class);
        diContext.registerService(LpHwElements.class, hwElements);
        
        midiIn.setSysexCallback(sysExHandler::handleSysEx);
        mainLayer = diContext.createLayer("MainLayer");
        shiftLayer = diContext.createLayer("GlobalShiftLayer");
        
        initTransportSection();
        assignModifiers(diContext.getService(Application.class));
        assignModeButtons();
        initViewControlListeners();
        sysExHandler.addPrintToClipDataListener(this::handlePrintToClipInvoked);
        
        final SessionLayer sessionLayer = diContext.create(SessionLayer.class);
        final SceneLaunchLayer sceneLaunchLayer = diContext.create(SceneLaunchLayer.class);
        drumPadLayer = diContext.create(DrumLayer.class);
        final NotePlayingLayer notePlayingLayer = diContext.create(NotePlayingLayer.class);
        overviewLayer = diContext.create(OverviewLayer.class);
        
        trackModeLayer = diContext.create(TrackModeLayer.class);
        final TrackControlLayer trackControlLayer = diContext.create(TrackControlLayer.class);
        initSliderLayers(diContext);
        
        mainLayer.activate();
        sessionLayer.activate();
        sceneLaunchLayer.activate();
        trackControlLayer.activate();
        trackModeLayer.activate();
        notePlayingLayer.activate();
        
        sysExHandler.addModeChangeListener(this::handleModeChanged);
        sysExHandler.deviceInquiry();
        diContext.activate();
        midiProcessor.start();
    }
    
    private void initSliderLayers(final Context diContext) {
        controlMaps.put(ControlMode.VOLUME, diContext.create(VolumeSliderLayer.class));
        controlMaps.put(ControlMode.PAN, diContext.create(PanSliderLayer.class));
        controlMaps.put(ControlMode.SENDS, diContext.create(SendsSliderLayer.class));
        controlMaps.put(ControlMode.DEVICE, diContext.create(DeviceSliderLayer.class));
    }
    
    private void initViewControlListeners() {
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        cursorTrack.canHoldNoteData()
            .addValueObserver(canHoldNoteData -> sysExHandler.enableClipPrint(canHoldNoteData));
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
            
            if (sysExHandler.getMode() == LpBaseMode.NOTE || sysExHandler.getMode() == LpBaseMode.CHORD) {
                changeDrumMode(hasDrumPads);
            }
        });
    }
    
    private void changeDrumMode(final boolean drumModeActive) {
        if (this.drumModeActive == drumModeActive) {
            return;
        }
        sysExHandler.changeNoteMode(drumModeActive);
        drumPadLayer.setIsActive(drumModeActive);
        this.drumModeActive = drumModeActive;
    }
    
    private void assignModifiers(final Application application) {
        final LabeledButton shiftButton = hwElements.getLabeledButton(LabelCcAssignments.SHIFT);
        shiftButton.bindPressed(
            mainLayer, pressed -> {
                shiftLayer.setIsActive(pressed);
                modifierStates.setShift(pressed);
            }, RgbState.BLUE, RgbState.WHITE);
        
        final LabeledButton clearButton = hwElements.getLabeledButton(LabelCcAssignments.CLEAR);
        clearButton.bindPressed(mainLayer, this::handleClear);
        clearButton.bindLightPressed(mainLayer, RgbState.BUTTON_INACTIVE, RgbState.BUTTON_ACTIVE);
        clearButton.bindLightPressed(shiftLayer, RgbState.SHIFT_INACTIVE, RgbState.SHIFT_ACTIVE);
        
        final LabeledButton duplicateButton = hwElements.getLabeledButton(LabelCcAssignments.DUPLICATE);
        duplicateButton.bindPressed(mainLayer, this::handleDuplicate);
        duplicateButton.bindLightPressed(mainLayer, RgbState.BUTTON_INACTIVE, RgbState.BUTTON_ACTIVE);
        duplicateButton.bindLightPressed(shiftLayer, RgbState.SHIFT_INACTIVE, RgbState.SHIFT_ACTIVE);
        
        final LabeledButton quantizeButton = hwElements.getLabeledButton(LabelCcAssignments.QUANTIZE);
        quantizeButton.bindPressed(mainLayer, this::handleQuantize, RgbState.BUTTON_ACTIVE, RgbState.BUTTON_INACTIVE);
        
        final SettableEnumValue recordQuantizeValue = application.recordQuantizationGrid();
        recordQuantizeValue.markInterested();
        quantizeButton.bindPressed(shiftLayer, pressed -> toggleRecordQuantize(recordQuantizeValue, pressed));
        quantizeButton.bindLight(
            shiftLayer,
            () -> recordQuantizeValue.get().equals("OFF") ? RgbState.RED_LO : RgbState.TURQUOISE);
    }
    
    private void handleDuplicate(final boolean pressed) {
        if (mainMode.isNoteHandler() && pressed) { // TODO consider long pressing
            viewControl.handleDuplication(modifierStates.isShift());
        }
        modifierStates.setDuplicate(pressed);
    }
    
    private void handleClear(final boolean pressed) {
        if (mainMode.isNoteHandler() && pressed) {
            viewControl.handleClear(modifierStates.isShift());
        }
        modifierStates.setClear(pressed);
    }
    
    private void handleQuantize(final boolean pressed) {
        if (mainMode.isNoteHandler() && pressed) {
            viewControl.handleQuantize(modifierStates.isShift());
        }
        modifierStates.setQuantize(pressed);
    }
    
    private void toggleRecordQuantize(final SettableEnumValue recordQuant, final Boolean pressed) {
        if (!pressed) {
            return;
        }
        final String current = recordQuant.get();
        if ("OFF".equals(current)) {
            recordQuant.set("1/16");
        } else {
            recordQuant.set("OFF");
        }
    }
    
    private void assignModeButtons() {
        final LabeledButton sessionButton = hwElements.getLabeledButton(LabelCcAssignments.SESSION);
        sessionButton.bindRelease(
            mainLayer, () -> {
                if (overviewLayer.isActive()) {
                    overviewLayer.setIsActive(false);
                }
            });
        sessionButton.bindPressed(
            mainLayer, () -> {
                if (mainMode == LpBaseMode.SESSION) {
                    overviewLayer.setIsActive(true);
                } else {
                    sysExHandler.changeMode(LpBaseMode.SESSION, 0);
                }
            });
        sessionButton.bindLight(
            mainLayer,
            () -> sysExHandler.getMode() == LpBaseMode.SESSION ? RgbState.BLUE : RgbState.DIM_WHITE);
        
        final LabeledButton noteButton = hwElements.getLabeledButton(LabelCcAssignments.NOTE);
        noteButton.bind(
            mainLayer, () -> {
                sysExHandler.changeMode(LpBaseMode.NOTE, 0);
            }, () -> sysExHandler.getMode() == LpBaseMode.NOTE ? RgbState.ORANGE : RgbState.DIM_WHITE);
        final LabeledButton chordButton = hwElements.getLabeledButton(LabelCcAssignments.CHORD);
        chordButton.bind(
            mainLayer, () -> {
                sysExHandler.changeMode(LpBaseMode.CHORD, 0);
            }, () -> sysExHandler.getMode() == LpBaseMode.CHORD ? RgbState.ORANGE : RgbState.DIM_WHITE);
    }
    
    private void initTransportSection() {
        transport.isPlaying().markInterested();
        transport.tempo().markInterested();
        transport.playPosition().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        
        final LabeledButton playButton = hwElements.getLabeledButton(LabelCcAssignments.PLAY);
        playButton.bind(mainLayer, this::togglePlay, this::getPlayColor);
        playButton.bind(
            shiftLayer, () -> transport.continuePlayback(),
            () -> transport.isPlaying().get() ? RgbState.TURQUOISE : RgbState.SHIFT_INACTIVE);
        
        final LabeledButton recButton = hwElements.getLabeledButton(LabelCcAssignments.REC);
        recButton.bind(mainLayer, this::toggleRecord, this::getRecordButtonColorRegular);
        recButton.bindPressed(shiftLayer, () -> transport.isClipLauncherOverdubEnabled().toggle());
        recButton.bindLight(
            shiftLayer, pressed -> transport.isClipLauncherOverdubEnabled().get() ? //
                (pressed ? RgbState.pulse(60) : RgbState.of(60)) : RgbState.DIM_WHITE);
    }
    
    private RgbState getPlayColor() {
        return transport.isPlaying().get() ? RgbState.GREEN : RgbState.DIM_WHITE;
    }
    
    private void togglePlay() {
        transport.isPlaying().toggle();
    }
    
    private RgbState getRecordButtonColorRegular() {
        final FocusSlot focusSlot = viewControl.getFocusSlot();
        if (focusSlot != null) {
            final ClipLauncherSlot slot = focusSlot.getSlot();
            if (slot.isRecordingQueued().get()) {
                return RgbState.flash(5, 0);
            }
            if (slot.isRecording().get() || slot.isRecordingQueued().get()) {
                return RgbState.pulse(5);
            }
        }
        if (transport.isClipLauncherOverdubEnabled().get()) {
            return RgbState.RED;
        } else {
            return RgbState.of(1);
        }
    }
    
    private void toggleRecord() {
        viewControl.globalRecordAction(transport);
    }
    
    private void handlePrintToClipInvoked(final PrintToClipData printToClipData) {
        viewControl.createNewClip();
        printToClipData.applyToClip(viewControl.getCursorClip());
    }
    
    private void onMidi0(final ShortMidiMessage msg) {
        // DebugOut.println("MIDI %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
    }
    
    private void onMidi1(final ShortMidiMessage msg) {
        //if (msg.getChannel() == 0 && msg.getStatusByte() == 144) {
        //    drumseqenceMode.notifyMidiEvent(msg.getData1(), msg.getData2());
        //}
        // DebugOut.println("MIDI 2 -> %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
    }
    
    private void shutDownController(final CompletableFuture<Boolean> shutdown) {
        sysExHandler.setDawMode(false);
        try {
            Thread.sleep(300);
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }
        shutdown.complete(true);
    }
    
    @Override
    public void exit() {
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> shutDownController(shutdown));
        try {
            shutdown.get();
        }
        catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    @Override
    public void toFaderMode(final ControlMode controlMode, final ControlMode previousMode) {
        final SliderLayer nextControlLayer = controlMaps.get(controlMode);
        final SliderLayer previousLayer = controlMaps.get(previousMode);
        if (previousLayer != null) {
            previousLayer.setIsActive(false);
            sysExHandler.enableFaderMode(previousMode, false);
        }
        if (nextControlLayer != null) {
            nextControlLayer.setIsActive(true);
            sysExHandler.enableFaderMode(controlMode, true);
        } else {
            sysExHandler.setLayout(LpBaseMode.SESSION, 0);
        }
    }
    
    private void handleModeChanged(final LpBaseMode mode, final int page) {
        final LpBaseMode prevMode = mainMode;
        final int prevPage = mainModePage;
        if (prevMode == LpBaseMode.FADER && mode != LpBaseMode.FADER) {
            final ControlMode previousCtrlMode = ControlMode.fromPageId(prevPage);
            final SliderLayer previousLayer = controlMaps.get(previousCtrlMode);
            previousLayer.setIsActive(false);
        }
        mainMode = mode;
        mainModePage = page;
        if (mode == LpBaseMode.FADER) {
            trackModeLayer.setIsActive(true);
            trackModeLayer.setControlMode(ControlMode.fromPageId(page));
        } else {
            trackModeLayer.setControlMode(ControlMode.NONE);
        }
        if (sysExHandler.getMode() == LpBaseMode.NOTE || sysExHandler.getMode() == LpBaseMode.CHORD) {
            changeDrumMode(viewControl.getPrimaryDevice().hasDrumPads().get());
        } else if (prevMode == LpBaseMode.NOTE && drumModeActive) {
            changeDrumMode(false);
        }
    }
    
}
