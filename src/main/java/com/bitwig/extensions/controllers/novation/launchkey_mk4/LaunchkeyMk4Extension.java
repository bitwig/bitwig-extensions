package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchkeyButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.MonoButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.definition.LaunchkeyMk4ExtensionDefinition;
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
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(String.format(format, args));
        }
    }
    
    public LaunchkeyMk4Extension(final LaunchkeyMk4ExtensionDefinition definition, final ControllerHost host,
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
        
        initControl(diContext);
        
        mainLayer.setIsActive(true);
        sessionLayer.setIsActive(true);
        midiProcessor.init();
    }
    
    public void initControl(final Context diContext) {
        final LaunchkeyHwElements hwElements = diContext.getService(LaunchkeyHwElements.class);
        final Transport transport = diContext.getService(Transport.class);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final MasterTrack masterTrack = host.createMasterTrack(2);
        final GlobalStates globalStates = diContext.getService(GlobalStates.class);
        
        final Application application = diContext.getService(Application.class);
        
        final LaunchkeyButton shiftButton = hwElements.getShiftButton();
        shiftButton.bindIsPressed(mainLayer, globalStates.getShiftState());
        
        globalStates.getShiftState().addValueObserver(shiftActive -> shiftLayer.setIsActive(shiftActive));
        
        final ViewControl viewControl = diContext.getService(ViewControl.class);
        final TrackBank trackBank = viewControl.getTrackBank();
        
        mainLayer.bind(hwElements.getMasterSlider(), masterTrack.volume());
        //masterTrack.volume().displayedValue().addValueObserver(v -> lcdDisplay.setValue(v, 88));
        //masterTrack.name().addValueObserver(name -> lcdDisplay.setParameter("Volume - " + name, 88));
        
        final HardwareSlider[] trackSliders = hwElements.getSliders();
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final HardwareSlider slider = trackSliders[i];
            mainLayer.bind(slider, track.volume().value());
        }
        
        transport.isPlaying().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerLoopEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        
        final MonoButton playButton = hwElements.getPlayButton();
        final MonoButton stopButton = hwElements.getStopButton();
        final MonoButton loopButton = hwElements.getLoopButton();
        final MonoButton recButton = hwElements.getRecButton();
        final MonoButton metroButton = hwElements.getMetroButton();
        final MonoButton undoButton = hwElements.getUndoButton();
        playButton.bind(mainLayer, transport.playAction());
        playButton.bindLight(mainLayer, transport.isPlaying());
        stopButton.bind(mainLayer, transport.stopAction());
        stopButton.bindLight(mainLayer, transport.isPlaying());
        loopButton.bindToggle(mainLayer, transport.isArrangerLoopEnabled());
        loopButton.bindLight(mainLayer, transport.isArrangerLoopEnabled());
        recButton.bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled());
        recButton.bindLight(mainLayer, transport.isClipLauncherOverdubEnabled());
        metroButton.bindToggle(mainLayer, transport.isMetronomeEnabled());
        metroButton.bindLight(mainLayer, transport.isMetronomeEnabled());
        
        undoButton.bind(mainLayer, application.undoAction());
        undoButton.bindLight(mainLayer, application.canUndo());
        undoButton.bind(shiftLayer, application.redoAction());
        undoButton.bindLight(shiftLayer, application.canRedo());
    }
    
    @Override
    public void exit() {
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            //setDawMode(false);
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
