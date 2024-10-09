package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
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
        
        initControl(diContext);
        
        mainLayer.setIsActive(true);
        midiProcessor.init();
    }
    
    public void initControl(final Context diContext) {
        final LaunchkeyHwElements hwElements = diContext.getService(LaunchkeyHwElements.class);
        final Transport transport = diContext.getService(Transport.class);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final MasterTrack masterTrack = host.createMasterTrack(2);
        final GlobalStates globalStates = diContext.getService(GlobalStates.class);
        final LaunchkeyButton shiftButton = hwElements.getShiftButton();
        shiftButton.bindIsPressed(mainLayer, globalStates.getShiftState());
        mainLayer.bind(hwElements.getMasterSlider(), masterTrack.volume());
        //masterTrack.volume().displayedValue().addValueObserver(v -> lcdDisplay.setValue(v, 88));
        //masterTrack.name().addValueObserver(name -> lcdDisplay.setParameter("Volume - " + name, 88));
        transport.isPlaying().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerLoopEnabled().markInterested();
        
        final MonoButton playButton = hwElements.getPlayButton();
        final MonoButton stopButton = hwElements.getStopButton();
        final MonoButton loopButton = hwElements.getLoopButton();
        final MonoButton recButton = hwElements.getRecButton();
        playButton.bind(mainLayer, transport.playAction());
        playButton.bindLight(mainLayer, transport.isPlaying());
        stopButton.bind(mainLayer, transport.stopAction());
        stopButton.bindLight(mainLayer, transport.isPlaying());
        loopButton.bindToggle(mainLayer, transport.isArrangerLoopEnabled());
        loopButton.bindLight(mainLayer, transport.isArrangerLoopEnabled());
        recButton.bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled());
        recButton.bindLight(mainLayer, transport.isClipLauncherOverdubEnabled());
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
