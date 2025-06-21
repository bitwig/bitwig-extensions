package com.bitwig.extensions.controllers.reloop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DetailEditor;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.reloop.display.ScreenManager;
import com.bitwig.extensions.controllers.reloop.display.ScreenMode;
import com.bitwig.extensions.controllers.reloop.display.ScreenParameterBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.LayoutType;

public class KeypadProControllerExtension extends ControllerExtension {
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private HardwareSurface surface;
    private Layer mainLayer;
    private SessionLayer sessionLayer;
    private DrumPadLayer drumPadLayer;
    private DeviceSelectionLayer deviceSelectionLayer;
    
    
    private Layer deviceControlLayer;
    private MidiProcessor midiProcessor;
    private boolean sessionLayerActive = false;
    private LayoutType panelLayout = LayoutType.LAUNCHER;
    private boolean performHeld;
    private boolean pageNavOccurred;
    private long performDownTime;
    private BitwigControl viewControl;
    private ScreenManager screenManager;
    private String[] pageNames = new String[0];
    private int pageIndex;
    private final GlobalStates globalState = new GlobalStates();
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    protected KeypadProControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        midiProcessor = new MidiProcessor(getHost(), this::handleActivation, globalState);
        final Context diContext = new Context(this);
        
        //        final SettableRangedValue delayValue =
        //            getHost().getPreferences().getNumberSetting("Delay", "MIDI", 1000, 5000, 1, "", 1000);
        
        diContext.registerService(MidiProcessor.class, midiProcessor);
        surface = diContext.getService(HardwareSurface.class);
        
        mainLayer = diContext.createLayer("MAIN_LAYER");
        deviceControlLayer = diContext.createLayer("DEVICE_CONTROL");
        diContext.registerService(GlobalStates.class, globalState);
        
        final MidiProcessor midiProcessor = diContext.getService(MidiProcessor.class);
        
        sessionLayer = diContext.getService(SessionLayer.class);
        drumPadLayer = diContext.getService(DrumPadLayer.class);
        deviceSelectionLayer = diContext.getService(DeviceSelectionLayer.class);
        viewControl = diContext.getService(BitwigControl.class);
        screenManager = diContext.getService(ScreenManager.class);
        initTransport(diContext);
        initChannelControls(diContext);
        initSessionView(diContext);
        initScreen();
        midiProcessor.init(1000);
    }
    
    private void initScreen() {
        final TrackBank trackBank = viewControl.getTrackBank();
        viewControl.getCursorTrack().name().addValueObserver(name -> screenManager.updateTrackName(name));
        final Scene scene = trackBank.sceneBank().getScene(0);
        scene.name().addValueObserver(name -> screenManager.updateSceneName(name));
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        cursorDevice.name().addValueObserver(name -> screenManager.updateDeviceName(name));
        final CursorRemoteControlsPage remotes = viewControl.getRemotes();
        remotes.pageNames().addValueObserver(pageNames -> {
            this.pageNames = pageNames;
            updatePageNames();
        });
        remotes.selectedPageIndex().addValueObserver(pageIndex -> {
            this.pageIndex = pageIndex;
            updatePageNames();
        });
    }
    
    private void updatePageNames() {
        if (deviceControlLayer.isActive()) {
            if (this.pageIndex == -1) {
                screenManager.updatePageName("P: -no device- ");
            } else if (this.pageNames.length == 0) {
                screenManager.updatePageName("P: -no remotes- ");
            } else if (this.pageIndex < this.pageNames.length) {
                screenManager.updatePageName("P:" + this.pageNames[this.pageIndex]);
            }
        } else {
            screenManager.updatePageName("");
        }
    }
    
    private void handleActivation(final int stage) {
        if (stage == 0) {
            mainLayer.setIsActive(true);
            sessionLayer.setIsActive(sessionLayerActive);
            drumPadLayer.setIsActive(!sessionLayerActive);
            //screenManager.updateCurrent();
            screenManager.start();
        } else if (stage == 1) {
            screenManager.updateCurrent();
        }
    }
    
    private void initTransport(final Context diContext) {
        final Transport transport = diContext.getService(Transport.class);
        final Application application = diContext.getService(Application.class);
        final BitwigControl viewControl = diContext.getService(BitwigControl.class);
        final HwElements hwElements = diContext.getService(HwElements.class);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final DetailEditor detailEditor = host.createDetailEditor();
        final Arranger arranger = host.createArranger();
        
        setUpTransport(transport);
        
        final HardwareSlider masterSlider = surface.createHardwareSlider("MASTER");
        masterSlider.setAdjustValueMatcher(midiProcessor.getMidiIn().createAbsoluteCCValueMatcher(0, 0x7));
        final Track rootTrack = viewControl.getRootTrack();
        mainLayer.bind(masterSlider, rootTrack.volume());
        mainLayer.addBinding(
            new ScreenParameterBinding(masterSlider, rootTrack.volume(), new BasicStringValue("Master Volume"),
                ScreenMode.MIXER_PARAMETER, screenManager));
        
        hwElements.get(Assignment.PLAY).bindPressRelease(mainLayer, transport::play);
        hwElements.get(Assignment.PLAY).bindLight(mainLayer, transport.isPlaying());
        
        final LedButton stopButton = hwElements.get(Assignment.STOP);
        stopButton.bindTogglePressedState(mainLayer, transport::stop, transport.isPlaying());
        stopButton.bindLightOnPressed(mainLayer);
        
        final LedButton recordButton = hwElements.get(Assignment.RECORD);
        recordButton.bindToggle(mainLayer, transport.isArrangerRecordEnabled());
        recordButton.bindLight(mainLayer, transport.isArrangerRecordEnabled());
        
        final LedButton gridReverseButton = hwElements.get(Assignment.GRID_BACK);
        gridReverseButton.bindRepeatHold(mainLayer, transport::rewind);
        gridReverseButton.bindLightOnPressed(mainLayer);
        final LedButton gridForwardButton = hwElements.get(Assignment.GRID_FORWARD);
        gridForwardButton.bindRepeatHold(mainLayer, transport::fastForward);
        gridForwardButton.bindLightOnPressed(mainLayer);
        
        hwElements.get(Assignment.REC_OVERDUB)
            .bindPressed(mainLayer, transport.isClipLauncherOverdubEnabled().toggleAction());
        hwElements.get(Assignment.REC_OVERDUB).bindLight(mainLayer, transport.isClipLauncherOverdubEnabled());
        
        final LedButton zoomOutButton = hwElements.get(Assignment.ZOOM_IN);
        zoomOutButton.bindRepeatHold(mainLayer, () -> handleZoomOut(arranger, detailEditor));
        zoomOutButton.bindLightOnPressed(mainLayer);
        
        final LedButton zoomInButton = hwElements.get(Assignment.ZOOM_OUT);
        zoomInButton.bindRepeatHold(mainLayer, () -> handleZoomIn(arranger, detailEditor));
        zoomInButton.bindLightOnPressed(mainLayer);
        
        hwElements.get(Assignment.AUTO).bindToggle(mainLayer, transport.isArrangerAutomationWriteEnabled());
        hwElements.get(Assignment.AUTO).bindLight(mainLayer, transport.isArrangerAutomationWriteEnabled());
        
        application.canUndo().markInterested();
        hwElements.get(Assignment.UNDO).bindPressed(mainLayer, application::undo);
        hwElements.get(Assignment.UNDO).bindLight(mainLayer, () -> application.canUndo().get());
        
        hwElements.get(Assignment.STOP_ALL).bindPressed(mainLayer, () -> rootTrack.stop());
        
        final TrackBank trackBank = viewControl.getTrackBank();
        hwElements.get(Assignment.SCENE_LAUNCH)
            .bindPressed(mainLayer, () -> trackBank.sceneBank().getScene(0).launch());
        
        hwElements.get(Assignment.METRO).bindToggle(mainLayer, transport.isMetronomeEnabled());
        hwElements.get(Assignment.METRO).bindLight(mainLayer, transport.isMetronomeEnabled());
        
        hwElements.get(Assignment.LOOP).bindToggle(mainLayer, transport.isArrangerLoopEnabled());
        hwElements.get(Assignment.LOOP).bindLight(mainLayer, transport.isArrangerLoopEnabled());
        
        hwElements.get(Assignment.TAP).bindPressed(mainLayer, transport::tapTempo);
        
        hwElements.get(Assignment.MARKER).bindPressed(mainLayer, transport::addCueMarkerAtPlaybackPosition);
        hwElements.get(Assignment.MARKER).bindLightOnPressed(mainLayer);
        hwElements.get(Assignment.PREV_MARKER).bindPressed(mainLayer, transport::jumpToPreviousCueMarker);
        hwElements.get(Assignment.NEXT_MARKER).bindPressed(mainLayer, transport::jumpToNextCueMarker);
        application.panelLayout().addValueObserver(value -> panelLayout = LayoutType.toType(value));
        hwElements.get(Assignment.VIEW).bindPressed(mainLayer, () -> viewAction(application));
        hwElements.get(Assignment.VIEW).bindLightOnPressed(mainLayer);
        
        hwElements.get(Assignment.CST_SHIFT).bindPressed(mainLayer, () -> viewControl.getCursorClip().quantize(1.0));
        hwElements.get(Assignment.CST_SHIFT).bindLightOnPressed(mainLayer);
        
        hwElements.get(Assignment.PREV_MARKER).bindLightOnPressed(mainLayer);
        hwElements.get(Assignment.NEXT_MARKER).bindLightOnPressed(mainLayer);
        hwElements.get(Assignment.BACK).bindIsPressed(mainLayer, this::handlePerformButtonPressed);
        hwElements.get(Assignment.BACK).bindLight(mainLayer, () -> deviceControlLayer.isActive());
        hwElements.get(Assignment.SHIFT_RESET).bindPressed(mainLayer, viewControl::clearAllSteps);
        final HardwareButton encoderButton = hwElements.getEncoderButton();
        mainLayer.bindPressed(encoderButton, this::handleEncoderPressed);
        final StepEncoder mainEncoder = hwElements.getMainEncoder();
        mainEncoder.bindEncoder(mainLayer, this::handleMainEncoderTurn);
        hwElements.getShiftEncoder().bindEncoder(mainLayer, this::handleMainEncoderTurn);
        
        mainLayer.bindPressed(hwElements.getEncoderButton(), () -> drumPadLayer.setEncoderPressed(true));
        mainLayer.bindReleased(hwElements.getEncoderButton(), () -> drumPadLayer.setEncoderPressed(false));
        
        transport.tempo().value()
            .addValueObserver(tempo -> midiProcessor.updateTempo((int) Math.round(transport.tempo().getRaw())));
    }
    
    private void handleMainEncoderTurn(final int dir) {
        if (globalState.getShiftState().get()) {
            return;
        }
        if (performHeld && deviceControlLayer.isActive()) {
            if (globalState.getShiftState().get()) {
                viewControl.navigateDevices(dir);
            } else {
                viewControl.navigateRemotes(-dir);
            }
            pageNavOccurred = true;
        } else {
            if (sessionLayer.isActive()) {
                sessionLayer.navigateScenes(dir);
            } else if (drumPadLayer.isActive()) {
                drumPadLayer.navigatePadScale(-dir);
            }
        }
    }
    
    private void handlePerformButtonPressed(final boolean pressed) {
        performHeld = pressed;
        if (pressed) {
            performDownTime = System.currentTimeMillis();
        }
        if (!pressed && !pageNavOccurred && (System.currentTimeMillis() - performDownTime) < 1000) {
            deviceControlLayer.toggleIsActive();
            updatePageNames();
        }
        
        enableDeviceSelection(performHeld && deviceControlLayer.isActive());
        pageNavOccurred = false;
    }
    
    private void enableDeviceSelection(final boolean enable) {
        if (!enable && !deviceSelectionLayer.isActive()) {
            return;
        }
        if (sessionLayerActive) {
            sessionLayer.setIsActive(!enable);
        } else {
            drumPadLayer.setIsActive(!enable);
        }
        deviceSelectionLayer.setIsActive(enable);
    }
    
    private void handleZoomIn(final Arranger arranger, final DetailEditor editor) {
        if (panelLayout == LayoutType.ARRANGER) {
            arranger.zoomIn();
        } else {
            editor.zoomIn();
        }
    }
    
    private void handleZoomOut(final Arranger arranger, final DetailEditor editor) {
        if (panelLayout == LayoutType.ARRANGER) {
            arranger.zoomOut();
        } else {
            editor.zoomOut();
        }
    }
    
    private void handleEncoderPressed() {
        sessionLayer.launchScene();
    }
    
    private void viewAction(final Application application) {
        switch (panelLayout) {
            case LAUNCHER -> application.setPanelLayout(LayoutType.EDIT.getName());
            case EDIT -> application.setPanelLayout(LayoutType.ARRANGER.getName());
            case ARRANGER -> application.setPanelLayout(LayoutType.LAUNCHER.getName());
        }
    }
    
    private void initChannelControls(final Context diContext) {
        final BitwigControl viewControl = diContext.getService(BitwigControl.class);
        final HwElements hwElements = diContext.getService(HwElements.class);
        final ScreenManager screenManager = diContext.getService(ScreenManager.class);
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final ChannelControls control = hwElements.getChannelControl(i);
            final Track track = trackBank.getItemAt(i);
            control.bind(mainLayer, track, viewControl, screenManager);
        }
        
        final StringValue deviceName = viewControl.getCursorDevice().name();
        final CursorRemoteControlsPage remotes = viewControl.getRemotes();
        for (int i = 0; i < 32; i++) {
            final ChannelControls control = hwElements.getChannelControl(i);
            final RemoteControl remote = remotes.getParameter(i % 8);
            control.bindEncoder(deviceControlLayer, remote, deviceName, screenManager);
        }
    }
    
    private void initSessionView(final Context diContext) {
        final HwElements hwElements = diContext.getService(HwElements.class);
        
        final HardwareButton shiftButton = hwElements.getShiftButton();
        mainLayer.bindPressed(shiftButton, () -> globalState.getShiftState().set(true));
        mainLayer.bindReleased(shiftButton, () -> globalState.getShiftState().set(false));
        
        final LedButton cstButton = hwElements.get(Assignment.CST1);
        cstButton.bindLight(mainLayer, () -> sessionLayerActive);
        cstButton.bindPressed(mainLayer, () -> {
            sessionLayerActive = !sessionLayerActive;
            activateLayers();
        });
        //        final HardwareButton ccButton = hwElements.getCcButton();
        //        mainLayer.bindReleased(ccButton, () -> {
        //            println(" => NORMAL ");
        //            globalState.getCcState().set(false);
        //        });
        //        mainLayer.bindPressed(ccButton, () -> {
        //            println(" => CC STATE ");
        //            globalState.getCcState().set(true);
        //        });
        globalState.getCcState().addValueObserver(ccState -> {
            if (ccState) {
                sessionLayer.setIsActive(false);
                drumPadLayer.setIsActive(false);
            } else {
                activateLayers();
            }
        });
    }
    
    private void activateLayers() {
        if (globalState.getCcState().get()) {
            return;
        }
        if (sessionLayerActive) {
            sessionLayer.setIsActive(true);
            drumPadLayer.setIsActive(false);
        } else {
            drumPadLayer.setIsActive(true);
            sessionLayer.setIsActive(false);
        }
    }
    
    private void setUpTransport(final Transport transport) {
        transport.isPlaying().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isArrangerAutomationWriteEnabled().markInterested();
        transport.isArrangerLoopEnabled().markInterested();
        transport.isPlaying().addValueObserver(playing -> sessionLayer.setPlaying(playing));
        transport.isClipLauncherOverdubEnabled()
            .addValueObserver(overdub -> sessionLayer.setClipLauncherOverdub(overdub));
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
