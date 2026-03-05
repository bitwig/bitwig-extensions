package com.bitwig.extensions.controllers.softube.console1;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.softube.console1.definition.Console1ExtensionDefinition;
import com.bitwig.extensions.framework.di.Context;

public class Console1Extension extends ControllerExtension {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");

    private HardwareSurface surface;
    private final Console1ExtensionDefinition definition;
    private ViewControl viewControl;
    private TrackControl trackSlotControl;
    private ConsoleMidiProcessor midiProcessor;

    //    public static void println(final String format, final Object... args) {
    //        if (debugHost != null) {
    //            final LocalDateTime now = LocalDateTime.now();
    //            debugHost.println(now.format(DF) + " > " + String.format(format, args));
    //        }
    //    }

    public Console1Extension(final Console1ExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
        this.definition = definition;
    }

    @Override
    public void init() {
        final Context diContext = new Context(this);
        midiProcessor = new ConsoleMidiProcessor(getHost(), definition.getNumMidiInPorts(), definition.getName());

        surface = diContext.getService(HardwareSurface.class);
        final Application application = diContext.getService(Application.class);

        viewControl = diContext.getService(ViewControl.class);

        trackSlotControl = new TrackControl(viewControl.getTrackBank(), getHost(), midiProcessor,
            diContext.getService(Transport.class));
        midiProcessor.setTrackSlotControl(trackSlotControl);
        application.hasActiveEngine().addValueObserver(active -> midiProcessor.handleEngineSwitch(active));

        midiProcessor.startHandshake();
    }

    @Override
    public void exit() {
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            trackSlotControl.resetAll();
            midiProcessor.invokeReset();
            try {
                Thread.sleep(100);
            }
            catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
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
