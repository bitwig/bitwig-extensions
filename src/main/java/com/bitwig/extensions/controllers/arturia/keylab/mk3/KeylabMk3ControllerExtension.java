package com.bitwig.extensions.controllers.arturia.keylab.mk3;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.controls.RgbButton;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.display.ScreenTarget;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;
import com.bitwig.extensions.framework.values.LayoutType;

public class KeylabMk3ControllerExtension extends ControllerExtension {
    
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private static ControllerHost debugHost;
    private Layer mainLayer;
    private HardwareSurface surface;
    private MidiProcessor midiProcessor;
    private FocusMode recordFocusMode = FocusMode.LAUNCHER;
    private ViewControl viewControl;
    private LayoutType panelLayout = LayoutType.ARRANGER;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    protected KeylabMk3ControllerExtension(final KeylabMk3ControllerExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        final ControllerHost host = getHost();
        //debugHost = host;
        final Context diContext = new Context(this);
        surface = diContext.getService(HardwareSurface.class);
        mainLayer = diContext.createLayer("MAIN_LAYER");
        midiProcessor = new MidiProcessor(host);
        diContext.registerService(MidiProcessor.class, midiProcessor);
        final AutoDetectionMidiPortNamesList ports =
            getExtensionDefinition().getAutoDetectionMidiPortNamesList(PlatformType.MAC);
        
        final ClipLaunchingLayer clipLauncher = diContext.getService(ClipLaunchingLayer.class);
        setUpPreferences(clipLauncher);
        final DrumPadLayer drumPadLayer = diContext.getService(DrumPadLayer.class);
        viewControl = diContext.getService(ViewControl.class);
        drumPadLayer.setIsActive(true);
        bindTransport(diContext);
        bindMixer(diContext);
        
        midiProcessor.init();
        mainLayer.activate();
        clipLauncher.activate();
        diContext.activate();
    }
    
    private void setUpPreferences(final ClipLaunchingLayer clipLauncher) {
        final DocumentState documentState = getHost().getDocumentState();
        final SettableEnumValue recordButtonAssignment = documentState.getEnumSetting("Record Button assignment",
            //
            "Transport", new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            recordFocusMode.getDescriptor());
        recordButtonAssignment.addValueObserver(value -> recordFocusMode = FocusMode.toMode(value));
        
        final Preferences preferences = getHost().getPreferences();
        final SettableEnumValue clipStopTiming = preferences.getEnumSetting("Long press to stop clip", //
            "Clip", new String[] {"Fast", "Medium", "Standard"}, "Medium");
        clipStopTiming.addValueObserver(clipLauncher::setClipStopTiming);
    }
    
    
    private void bindMixer(final Context diContext) {
        final KeylabHardwareElements hwElements = diContext.getService(KeylabHardwareElements.class);
        final ViewControl viewControl = diContext.getService(ViewControl.class);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        hwElements.getTouchSlider(8).bindParameter(mainLayer, cursorTrack.volume(), cursorTrack.name());
        hwElements.getEncoder(8).bindParameter(mainLayer, cursorTrack.pan(), cursorTrack.name());
    }
    
    private void bindTransport(final Context context) {
        final KeylabHardwareElements hwElements = context.getService(KeylabHardwareElements.class);
        final Transport transport = context.getService(Transport.class);
        final Application application = context.getService(Application.class);
        prepareTransport(transport);
        
        application.panelLayout().addValueObserver(layout -> this.panelLayout = LayoutType.toType(layout));
        final RgbButton playButton = hwElements.getButton(CcAssignment.PLAY);
        playButton.bindPressed(mainLayer, transport::play);
        playButton.bindLight(mainLayer,
            () -> transport.isPlaying().get() ? RgbLightState.GREEN : RgbLightState.GREEN_DIMMED);
        
        final RgbButton recordButton = hwElements.getButton(CcAssignment.RECORD);
        recordButton.bindPressed(mainLayer, () -> handleRecordPressed(transport));
        recordButton.bindLight(mainLayer, () -> getRecordingLightState(transport));
        
        final RgbButton stopButton = hwElements.getButton(CcAssignment.STOP);
        stopButton.bindPressed(mainLayer, () -> transport.stop());
        stopButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        final RgbButton fastForwardButton = hwElements.getButton(CcAssignment.FAST_FWD);
        final RgbButton rewindButton = hwElements.getButton(CcAssignment.REWIND);
        fastForwardButton.bindRepeatHold(mainLayer, () -> transport.fastForward(), 400, 100);
        fastForwardButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        
        rewindButton.bindRepeatHold(mainLayer, () -> transport.rewind(), 400, 100);
        rewindButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        
        final RgbButton loopButton = hwElements.getButton(CcAssignment.LOOP);
        loopButton.bindToggle(mainLayer, transport.isArrangerLoopEnabled(), RgbLightState.ORANGE,
            RgbLightState.ORANGE_DIMMED);
        
        final RgbButton tapButton = hwElements.getButton(CcAssignment.TAP);
        tapButton.bind(mainLayer, transport.tapTempoAction(), RgbLightState.WHITE, RgbLightState.WHITE_DIMMED);
        final RgbButton metroButton = hwElements.getButton(CcAssignment.METRO);
        metroButton.bindToggle(mainLayer, transport.isMetronomeEnabled(), RgbLightState.WHITE,
            RgbLightState.WHITE_DIMMED);
        
        application.canUndo().markInterested();
        application.canRedo().markInterested();
        final RgbButton undoButton = hwElements.getButton(CcAssignment.UNDO);
        undoButton.bindPressed(mainLayer, application::undo);
        undoButton.bindLight(mainLayer,
            () -> application.canUndo().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
        
        final RgbButton redoButton = hwElements.getButton(CcAssignment.REDO);
        redoButton.bindPressed(mainLayer, application::redo);
        redoButton.bindLight(mainLayer,
            () -> application.canRedo().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
        
        final RgbButton quantizeButton = hwElements.getButton(CcAssignment.QUANTIZE);
        quantizeButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        quantizeButton.bindPressed(mainLayer, () -> invokeQuantize());
        final RgbButton saveButton = hwElements.getButton(CcAssignment.SAVE);
        final Action saveAction = application.getAction("Save");
        saveButton.bindLight(mainLayer, RgbLightState.WHITE_DIMMED, RgbLightState.WHITE);
        saveButton.bindPressed(mainLayer, () -> {
            saveAction.invoke();
        });
    }
    
    private void invokeQuantize() {
        if (panelLayout == LayoutType.ARRANGER) {
            final Clip clip = viewControl.getArrangerClip();
            if (clip.exists().get()) {
                viewControl.invokeArrangerQuantize();
                midiProcessor.screenLine2(ScreenTarget.POP_SCREEN_2_LINES, "Arrangement", RgbColor.AQUA,
                    "Clip Quantized", RgbColor.WHITE, null);
            } else {
                midiProcessor.screenLine2(ScreenTarget.POP_SCREEN_2_LINES, "Arrangement", RgbColor.AQUA,
                    "Quantization: No Clip", RgbColor.WHITE, null);
            }
        } else {
            final Clip clip = viewControl.getCursorClip();
            if (clip.exists().get()) {
                viewControl.invokeLauncherQuantize();
                midiProcessor.screenLine2(ScreenTarget.POP_SCREEN_2_LINES, "Launcher", RgbColor.AQUA, "Clip Quantized",
                    RgbColor.WHITE, null);
            } else {
                midiProcessor.screenLine2(ScreenTarget.POP_SCREEN_2_LINES, "Launcher", RgbColor.AQUA,
                    "Quantization: No Clip", RgbColor.WHITE, null);
            }
        }
    }
    
    private static void prepareTransport(final Transport transport) {
        transport.isArrangerRecordEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isPlaying().markInterested();
    }
    
    private void handleRecordPressed(final Transport transport) {
        if (recordFocusMode == FocusMode.ARRANGER) {
            transport.isArrangerRecordEnabled().toggle();
        } else {
            transport.isClipLauncherOverdubEnabled().toggle();
        }
    }
    
    private RgbLightState getRecordingLightState(final Transport transport) {
        if (recordFocusMode == FocusMode.ARRANGER) {
            return transport.isArrangerRecordEnabled().get() ? RgbLightState.RED : RgbLightState.RED_DIMMED;
        } else {
            return transport.isClipLauncherOverdubEnabled().get() ? RgbLightState.RED : RgbLightState.RED_DIMMED;
        }
    }
    
    
    @Override
    public void exit() {
        midiProcessor.exit();
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
