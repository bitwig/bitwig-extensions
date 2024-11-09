package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchkeyButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.definition.AbstractLaunchkeyMk4ExtensionDefinition;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.SequencerLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class LaunchkeyMk4Extension extends ControllerExtension {
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Layer mainLayer;
    private Layer shiftLayer;
    private final boolean hasFaders;
    private final boolean isMini;
    private MidiProcessor midiProcessor;
    private SessionLayer sessionLayer;
    private Layer currentMainModeLayer;
    private Layer stashedModeLayer;
    private DrumPadLayer padLayer;
    private SequencerLayer sequencerLayer;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(String.format(format, args));
        }
    }
    
    public LaunchkeyMk4Extension(final AbstractLaunchkeyMk4ExtensionDefinition definition, final ControllerHost host,
        final boolean hasFaders, final boolean miniVersion) {
        super(definition, host);
        this.hasFaders = hasFaders;
        this.isMini = miniVersion;
    }
    
    @Override
    public void init() {
        final Context diContext = new Context(this);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        debugHost = host;
        surface = diContext.getService(HardwareSurface.class);
        midiProcessor = new MidiProcessor(host, isMini);
        diContext.registerService(MidiProcessor.class, midiProcessor);
        
        mainLayer = diContext.createLayer("MAIN");
        shiftLayer = diContext.createLayer("SHIFT");
        sessionLayer = diContext.getService(SessionLayer.class);
        padLayer = diContext.getService(DrumPadLayer.class);
        sequencerLayer = diContext.getService(SequencerLayer.class);
        final DisplayControl display = diContext.getService(DisplayControl.class);
        display.initTemps();
        initControl(diContext);
        initGenerateModeHandling(diContext);
        midiProcessor.init();
        
        display.initTemps();
        mainLayer.setIsActive(true);
        sessionLayer.setIsActive(true);
        diContext.activate();
    }
    
    public void initGenerateModeHandling(final Context diContext) {
        final SessionLayer sessionLayer = diContext.getService(SessionLayer.class);
        currentMainModeLayer = null;
        final DisplayControl display = diContext.getService(DisplayControl.class);
        
        midiProcessor.addModeListener((type, id) -> this.handleModeChange(type, id, display));
    }
    
    private void handleModeChange(final ModeType type, final int id, final DisplayControl display) {
        if (type != ModeType.PAD) {
            return;
        }
        if (id == 2) {
            switchToDawControl(display);
        } else if (id == 15) {
            if (currentMainModeLayer != padLayer) {
                switchToLayer(padLayer);
            }
        } else if (currentMainModeLayer != null) {
            switchToCustomPadMode();
        }
    }
    
    private void switchToDawControl(final DisplayControl display) {
        if (stashedModeLayer != null) {
            if (stashedModeLayer == sequencerLayer) {
                switchToSequencer(display);
            } else {
                switchToSession(display);
            }
        } else if (currentMainModeLayer == sessionLayer) {
            switchToSequencer(display);
        } else {
            switchToSession(display);
        }
    }
    
    private void switchToSequencer(final DisplayControl display) {
        switchToLayer(sequencerLayer);
        display.setText(0x22, 0, "Sequencer 2/2");
        display.showDisplay(0x22);
        stashedModeLayer = null;
    }
    
    private void switchToSession(final DisplayControl display) {
        display.setText(0x22, 0, "Launcher 1/2");
        display.showDisplay(0x22);
        switchToLayer(sessionLayer);
        stashedModeLayer = null;
    }
    
    private void switchToCustomPadMode() {
        currentMainModeLayer.setIsActive(false);
        if (currentMainModeLayer == sessionLayer) {
            stashedModeLayer = sessionLayer;
        } else if (currentMainModeLayer == sequencerLayer) {
            stashedModeLayer = sequencerLayer;
            LaunchkeyMk4Extension.println(" STASH SEQ");
        } else {
            stashedModeLayer = null;
        }
        currentMainModeLayer = null;
    }
    
    private void switchToLayer(final Layer layer) {
        if (currentMainModeLayer != null) {
            currentMainModeLayer.setIsActive(false);
        }
        currentMainModeLayer = layer;
        currentMainModeLayer.setIsActive(true);
    }
    
    public void initControl(final Context diContext) {
        final LaunchkeyHwElements hwElements = diContext.getService(LaunchkeyHwElements.class);
        final Transport transport = diContext.getService(Transport.class);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final MasterTrack masterTrack = host.createMasterTrack(2);
        final GlobalStates globalStates = diContext.getService(GlobalStates.class);
        
        globalStates.setMiniVersion(isMini);
        
        final Application application = diContext.getService(Application.class);
        
        final LaunchkeyButton shiftButton = hwElements.getShiftButton();
        shiftButton.bindIsPressed(mainLayer, globalStates.getShiftState());
        
        globalStates.getShiftState().addValueObserver(shiftActive -> shiftLayer.setIsActive(shiftActive));
        
        mainLayer.bind(hwElements.getMasterSlider(), masterTrack.volume());
        
        transport.isPlaying().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerLoopEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        
        hwElements.getButton(CcAssignments.PLAY).bindPressed(mainLayer, () -> handleRestartPlay(transport));
        hwElements.getButton(CcAssignments.PLAY).bindPressed(mainLayer, () -> transport.play());
        hwElements.getButton(CcAssignments.PLAY).bindLightOnOff(mainLayer, transport.isPlaying());
        
        hwElements.getButton(CcAssignments.PLAY).bind(shiftLayer, transport.continuePlaybackAction());
        hwElements.getButton(CcAssignments.PLAY).bindLightOnOff(shiftLayer, transport.isPlaying());
        
        hwElements.getButton(CcAssignments.STOP).bind(mainLayer, transport.stopAction());
        hwElements.getButton(CcAssignments.STOP).bindLightOnOff(mainLayer, transport.isPlaying());
        hwElements.getButton(CcAssignments.LOOP).bindToggle(mainLayer, transport.isArrangerLoopEnabled());
        hwElements.getButton(CcAssignments.LOOP).bindLightOnOff(mainLayer, transport.isArrangerLoopEnabled());
        hwElements.getButton(CcAssignments.REC).bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled());
        hwElements.getButton(CcAssignments.REC).bindLightOnOff(mainLayer, transport.isClipLauncherOverdubEnabled());
        hwElements.getButton(CcAssignments.METRO).bindToggle(mainLayer, transport.isMetronomeEnabled());
        hwElements.getButton(CcAssignments.METRO).bindLightOnOff(mainLayer, transport.isMetronomeEnabled());
        
        
        final RgbButton undoButton = hwElements.getButton(CcAssignments.UNDO);
        undoButton.bind(mainLayer, application.undoAction());
        undoButton.bindLightOnOff(mainLayer, application.canUndo());
        undoButton.bind(shiftLayer, application.redoAction());
        undoButton.bindLightOnOff(shiftLayer, application.canRedo());
    }
    
    
    private void handleRestartPlay(final Transport transport) {
        if (!transport.isPlaying().get()) {
            transport.stop();
            transport.play();
        } else {
            transport.play();
        }
    }
    
    @Override
    public void exit() {
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            midiProcessor.exitDawMode();
            pause(100);
            shutdown.complete(true);
        });
        try {
            shutdown.get();
        }
        catch (final InterruptedException | ExecutionException e) {
            println(" >> Exit Daw MOde");
        }
    }
    
    private void pause(final long timeMs) {
        try {
            Thread.sleep(100);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    
}
