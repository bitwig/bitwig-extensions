package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition.AbstractLaunchControlExtensionDefinition;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer.LcDawControlLayer;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer.LcMixerLayer;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer.SpecControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;

public class LaunchControlMk3Extension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    
    private final AbstractLaunchControlExtensionDefinition definition;
    private HardwareSurface surface;
    private Context diContext;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    public static void showCallLocation(final String message) {
        println("MSG: %s", message);
        for (final StackTraceElement element : Thread.currentThread().getStackTrace()) {
            final String s = element.toString();
            if (s.startsWith("com.bitwig.extensions") && !s.contains("showCallLocation")) {
                println("  | %s ", s.replace("com.bitwig.extensions.controllers.novation.launchcontrolxlmk3", ""));
            }
        }
    }
    
    public LaunchControlMk3Extension(final AbstractLaunchControlExtensionDefinition definition,
        final ControllerHost host) {
        super(definition, host);
        this.definition = definition;
    }
    
    public void init() {
        debugHost = getHost();
        diContext = new Context(this, Set.of(definition.isXlVersion() ? "XLModel" : "LCModel"));
        diContext.registerService(AbstractLaunchControlExtensionDefinition.class, definition);
        surface = diContext.getService(HardwareSurface.class);
        if (!definition.isXlVersion()) {
            final SpecControl specControl = diContext.create(SpecControl.class);
            final LcDawControlLayer dawControlLayer = diContext.getService(LcDawControlLayer.class);
            final LcMixerLayer mixerLayer = diContext.getService(LcMixerLayer.class);
            dawControlLayer.setSpecOverlay(specControl);
            mixerLayer.setSpecOverlay(specControl);
        }
        diContext.activate();
        final Layers layers = diContext.getService(Layers.class);
        for (final Layer l : layers.getLayers()) {
            LaunchControlMk3Extension.println(" > " + l.getName());
        }
    }
    
    @Override
    public void exit() {
        // Nothing right now
        diContext.deactivate();
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
