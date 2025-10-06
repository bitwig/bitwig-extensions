package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.HwElements;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.SysExHandler;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.ViewControl;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplay;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.MainScreenSection;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.values.FocusMode;
import com.bitwig.extensions.framework.values.LayoutType;

public class KeylabEssential3Extension extends ControllerExtension {
    
    private Layer mainLayer;
    
    private HardwareSurface surface;
    private ControllerHost host;
    private Transport transport;
    private SysExHandler sysExHandler;
    
    private Runnable nextPingAction = null;
    private FocusMode recordFocusMode = FocusMode.ARRANGER;
    private ClipLaunchingLayer clipLaunchingLayer;
    private LcdDisplay lcdDisplay;
    private MainScreenSection mainScreenSection;
    private DrumPadLayer drumPadLayer;
    private SysExHandler.PadMode padMode = SysExHandler.PadMode.PAD_CLIPS;
    private boolean buttonPadRequestInvoked = false;
    private LayoutType panelLayout = LayoutType.ARRANGER;
    
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private static ControllerHost debugHost;
    private ViewControl viewControl;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    protected KeylabEssential3Extension(final KeyLabEssential3ExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        host = getHost();
        debugHost = host;
        final Context diContext = new Context(this);
        surface = diContext.getService(HardwareSurface.class);
        final MidiIn midiIn = host.getMidiInPort(0);
        //midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        final MidiOut midiOut = host.getMidiOutPort(0);
        diContext.registerService(MidiIn.class, midiIn);
        diContext.registerService(MidiOut.class, midiOut);
        sysExHandler = diContext.getService(SysExHandler.class);
        lcdDisplay = diContext.getService(LcdDisplay.class);
        sysExHandler.addSysExEventListener(this::handleSysExEvent);
        
        final NoteInput noteInput = midiIn.createNoteInput(
            "MIDI", getInputMask(
                0x0A, new int[] {
                    0x1, 0x40, 0x72, 0x73, 0x1E, 0x1F, 0x56, 0x57, 0x49, 0x4B, 0x4F, 0x48, //
                    0x50, 0x51, 0x52, 0x53, 0x55, 0x5A, 0x4A, 0x47, 0x4C, 0x4D, 0x5D, 0x12, 0x13, 0x10, 0x11
                }));
        noteInput.setShouldConsumeEvents(true);
        
        mainLayer = diContext.createLayer("MAIN");
        clipLaunchingLayer = diContext.create(ClipLaunchingLayer.class);
        drumPadLayer = diContext.create(DrumPadLayer.class);
        viewControl = diContext.getService(ViewControl.class);
        diContext.create(SliderEncoderControl.class);
        mainScreenSection = diContext.getService(MainScreenSection.class);
        diContext.create(BrowserLayer.class);
        
        sysExHandler.addPadModeEventListener(this::handlePadModeChanged);
        
        setUpTransportControl(diContext);
        initEncoders(diContext);
        initPadBankHandling(diContext);
        
        sysExHandler.deviceInquiry();
        
        mainScreenSection.setPadInfo(drumPadLayer.getPadLocationInfo());
        mainLayer.activate();
        clipLaunchingLayer.activate();
        drumPadLayer.activate();
        
        setUpPreferences();
        diContext.activate();
        host.scheduleTask(this::handlePing, 100);
    }
    
    private void handleSysExEvent(final SysExHandler.SysexEventType sysexEventType) {
        switch (sysexEventType) {
            case DAW_MODE:
                println(" Into Daw Mode");
                clipLaunchingLayer.activateIndication(true);
                break;
            case ARTURIA_MODE:
                println(" Into Arturia Mode");
                break;
            case USER_MODE:
                println(" USER MODE");
                break;
            case INIT:
                lcdDisplay.logoText("Bitwig", "connected", KeylabIcon.BITWIG);
                sysExHandler.requestPadBank();
                sysExHandler.queueTimedEvent(new TimedDelayEvent(() -> mainScreenSection.updatePage(), 2000));
                break;
        }
    }
    
    private void handlePing() {
        lcdDisplay.ping();
        if (nextPingAction != null) {
            nextPingAction.run();
            nextPingAction = null;
        }
        host.scheduleTask(this::handlePing, 100);
    }
    
    private String[] getInputMask(final int excludeChannel, final int[] miniLabPassThroughCcs) {
        final List<String> masks = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            if (i != excludeChannel) {
                masks.add(String.format("8%01x????", i));
                masks.add(String.format("9%01x????", i));
            }
        }
        masks.add("A?????"); // Poly Aftertouch
        masks.add("D?????"); // Channel Aftertouch
        masks.add("E?????"); // Pitchbend
        masks.add("B1????"); // CCs Channel 2
        //masks.add("B0????");
        for (final int miniLabPassThroughCc : miniLabPassThroughCcs) {
            masks.add(String.format("B0%02x??", miniLabPassThroughCc));
        }
        return masks.toArray(String[]::new);
    }
    
    private void setUpPreferences() {
        final Preferences preferences = getHost().getPreferences(); // THIS
        final SettableEnumValue recordButtonAssignment = preferences.getEnumSetting(
            "Record Button assignment", //
            "Transport", new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            recordFocusMode.getDescriptor());
        recordButtonAssignment.addValueObserver(value -> recordFocusMode = FocusMode.toMode(value));
        final SettableEnumValue clipStopTiming = preferences.getEnumSetting(
            "Long press to stop clip", //
            "Clip", new String[] {"Fast", "Medium", "Standard"}, "Medium");
        clipStopTiming.addValueObserver(clipLaunchingLayer::setClipStopTiming);
    }
    
    private void initEncoders(final Context diContext) {
        final HwElements hwElements = diContext.getService(HwElements.class);
        hwElements.bindEncoder(mainLayer, hwElements.getMainEncoder(), this::mainEncoderAction);
        mainLayer.bindPressed(hwElements.getEncoderPress(), this::handleEncoderPressed);
    }
    
    private void handlePadModeChanged(final SysExHandler.PadMode padMode) {
        this.padMode = buttonPadRequestInvoked ? padMode.inverted() : padMode;
        mainScreenSection.notifyPadMode(this.padMode);
        buttonPadRequestInvoked = false;
    }
    
    private void initPadBankHandling(final Context diContext) {
        final HwElements hwElements = diContext.getService(HwElements.class);
        final HardwareButton bankButton = hwElements.getBankButton();
        mainLayer.bindPressed(
            bankButton, () -> {
                sysExHandler.requestPadBank();
                buttonPadRequestInvoked = true;
            });
    }
    
    private void handleEncoderPressed() {
        if (padMode == SysExHandler.PadMode.PAD_CLIPS) {
            clipLaunchingLayer.launchScene();
        }
    }
    
    private void setUpTransportControl(final Context diContext) {
        final HwElements hwElements = diContext.getService(HwElements.class);
        transport = diContext.getService(Transport.class);
        final Application application = diContext.getService(Application.class);
        transport.isArrangerRecordEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        application.panelLayout().addValueObserver(layout -> this.panelLayout = LayoutType.toType(layout));
        
        final RgbButton loopButton = hwElements.getButton(CCAssignment.LOOP);
        loopButton.bindToggle(
            mainLayer, transport.isArrangerLoopEnabled(), RgbLightState.ORANGE,
            RgbLightState.ORANGE_DIMMED);
        final RgbButton playButton = hwElements.getButton(CCAssignment.PLAY);
        transport.isPlaying().markInterested();
        playButton.bindPressed(mainLayer, this::handlePlayPressed);
        playButton.bindLight(
            mainLayer,
            () -> transport.isPlaying().get() ? RgbLightState.GREEN : RgbLightState.GREEN_DIMMED);
        
        final RgbButton stopButton = hwElements.getButton(CCAssignment.STOP);
        stopButton.bindPressed(mainLayer, () -> transport.stop());
        stopButton.bindLight(
            mainLayer,
            () -> transport.isPlaying().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
        
        final RgbButton recordButton = hwElements.getButton(CCAssignment.REC);
        recordButton.bindPressed(mainLayer, this::handleRecordPressed);
        recordButton.bindLight(mainLayer, this::getRecordingLightState);
        
        final RgbButton tapButton = hwElements.getButton(CCAssignment.TAP);
        tapButton.bind(mainLayer, transport.tapTempoAction(), RgbLightState.WHITE, RgbLightState.WHITE_DIMMED);
        final RgbButton metroButton = hwElements.getButton(CCAssignment.METRO);
        metroButton.bindToggle(
            mainLayer, transport.isMetronomeEnabled(), RgbLightState.WHITE,
            RgbLightState.WHITE_DIMMED);
        
        final RgbButton fastForwardButton = hwElements.getButton(CCAssignment.FFWD);
        final RgbButton rewindButton = hwElements.getButton(CCAssignment.RWD);
        fastForwardButton.bindRepeatHold(mainLayer, () -> transport.fastForward(), 400, 100);
        fastForwardButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        
        rewindButton.bindRepeatHold(mainLayer, () -> transport.rewind(), 400, 100);
        rewindButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        
        application.canUndo().markInterested();
        application.canRedo().markInterested();
        final RgbButton undoButton = hwElements.getButton(CCAssignment.UNDO);
        undoButton.bindPressed(mainLayer, application::undo);
        undoButton.bindLight(
            mainLayer,
            () -> application.canUndo().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
        
        final RgbButton redoButton = hwElements.getButton(CCAssignment.REDO);
        redoButton.bindPressed(mainLayer, application::redo);
        redoButton.bindLight(
            mainLayer,
            () -> application.canRedo().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
        
        // TODO Punch Button does nothing
        final RgbButton quantizeButton = hwElements.getButton(CCAssignment.QUANTIZE);
        quantizeButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        quantizeButton.bindPressed(mainLayer, this::invokeQuantize);
        // quantizeButton.bind(mainLayer, this::customAction, RgbLightState.WHITE, RgbLightState.WHITE_DIMMED);
        final Action saveAction = application.getAction("Save");
        final RgbButton saveButton = hwElements.getButton(CCAssignment.SAVE);
        saveButton.bindPressed(
            mainLayer, () -> {
                saveAction.invoke();
                lcdDisplay.sendPopup("", "Project Saved", KeylabIcon.COMPUTER);
            });
        saveButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
    }
    
    
    private RgbLightState getRecordingLightState() {
        if (recordFocusMode == FocusMode.ARRANGER) {
            return transport.isArrangerRecordEnabled().get() ? RgbLightState.RED : RgbLightState.RED_DIMMED;
        } else {
            return transport.isClipLauncherOverdubEnabled().get() ? RgbLightState.RED : RgbLightState.RED_DIMMED;
        }
    }
    
    private void handlePlayPressed() {
        transport.play();
    }
    
    private void handleRecordPressed() {
        if (recordFocusMode == FocusMode.ARRANGER) {
            transport.isArrangerRecordEnabled().toggle();
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private void mainEncoderAction(final int dir) {
        if (padMode == SysExHandler.PadMode.PAD_CLIPS) {
            clipLaunchingLayer.navigateScenes(dir);
        } else {
            drumPadLayer.navigate(-dir);
        }
    }
    
    private void invokeQuantize() {
        if (panelLayout == LayoutType.ARRANGER) {
            final Clip clip = viewControl.getArrangerClip();
            if (clip.exists().get()) {
                viewControl.invokeArrangerQuantize();
                lcdDisplay.sendPopup("Arrangement", "Clip Quantized", KeylabIcon.NONE);
            } else {
                lcdDisplay.sendPopup("Arrangement", "Quantization: No Clip", KeylabIcon.NONE);
            }
        } else {
            final Clip clip = viewControl.getCursorClip();
            if (clip.exists().get()) {
                viewControl.invokeLauncherQuantize();
                lcdDisplay.sendPopup("Launcher", "Clip Quantized", KeylabIcon.NONE);
            } else {
                lcdDisplay.sendPopup("Launcher", "Quantization: No Clip", KeylabIcon.NONE);
            }
        }
    }
    
    @Override
    public void exit() {
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            sysExHandler.disconnectState();
            try {
                Thread.sleep(100);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
            shutdown.complete(true);
        });
        try {
            shutdown.get();
        }
        catch (final InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    
}
