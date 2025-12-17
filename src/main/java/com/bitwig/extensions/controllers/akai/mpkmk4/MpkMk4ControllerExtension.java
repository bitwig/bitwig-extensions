package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class MpkMk4ControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private ControllerHost host;
    private final Variant variant;
    private HardwareSurface surface;
    private Layer mainLayer;
    private MpkMidiProcessor midiProcessor;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    public enum Variant {
        MINI
    }
    
    public MpkMk4ControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
        final Variant variant) {
        super(definition, host);
        this.variant = variant;
    }
    
    @Override
    public void init() {
        this.host = getHost();
        debugHost = host;
        final Context diContext = new Context(this);
        mainLayer = diContext.createLayer("MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        midiProcessor = diContext.getService(MpkMidiProcessor.class);
        midiProcessor.init();
    }
    
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    @Override
    public void exit() {
        midiProcessor.exit();
    }
    
}
