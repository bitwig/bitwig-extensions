package com.bitwig.extensions.controllers.neuzeitinstruments;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.di.Context;

public class DropExtension extends ControllerExtension {
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    
    private HardwareSurface surface;
    private final DropExtensionDefinition definition;
    private Context diContext;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    protected DropExtension(final DropExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
        this.definition = definition;
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        diContext = new Context(this);
        surface = diContext.getService(HardwareSurface.class);
        diContext.activate();
    }
    
    @Override
    public void exit() {
        diContext.deactivate();
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
