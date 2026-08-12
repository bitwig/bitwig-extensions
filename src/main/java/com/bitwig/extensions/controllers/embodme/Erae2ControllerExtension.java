package com.bitwig.extensions.controllers.embodme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.values.LayoutType;
import com.bitwig.extensions.framework.view.OverviewGrid;

public class Erae2ControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private HardwareSurface surface;
    private ControllerHost host;
    private EraeMidiProcessor midiProcessor;
    private ClipLayer activeLayer;
    private ClipLayer smallClipLayer;
    private ClipLayer largeClipLayer;
    private Application application;
    private LayoutType panelLayout;
    private OverviewGrid overviewGrid;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    protected Erae2ControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        this.host = getHost();
        this.surface = host.createHardwareSurface();
        this.application = host.createApplication();
        final TrackBank maxTrackBank = host.createTrackBank(64, 1, 64, false);
        overviewGrid = new OverviewGrid(maxTrackBank);
        overviewGrid.initMaxBank();
        
        midiProcessor = new EraeMidiProcessor(host, this);
        application.panelLayout().addValueObserver(this::handleLayoutChanged);
        initLayers();
        midiProcessor.init();
    }
    
    private void handleLayoutChanged(final String layout) {
        this.panelLayout = LayoutType.toType(layout);
        Erae2ControllerExtension.println(" LAYOUT = " + this.panelLayout);
    }
    
    private void initLayers() {
        smallClipLayer = new ClipLayer(host, 9, 5, midiProcessor, false, overviewGrid);
        largeClipLayer = new ClipLayer(host, 12, 9, midiProcessor, false, overviewGrid);
        activeLayer = smallClipLayer;
        activeLayer.setActive(true);
    }
    
    public void handleMode(final int width, final int height) {
        Erae2ControllerExtension.println(" Window  width=%d height=%d   %s", width, height, panelLayout);
        activeLayer.setActive(false);
        if (width == 9) {
            activeLayer = smallClipLayer;
        } else if (width == 12) {
            activeLayer = largeClipLayer;
        }
        activeLayer.setCurrentHeightMode(height);
        activeLayer.setActive(true);
        activeLayer.updateState();
    }
    
    public void handleGridButton(final int row, final int col, final boolean pressed) {
        if (pressed) {
            activeLayer.handleGridButton(row, col, true);
        }
    }
    
    public void handleSlider(final int sliderIndex, final int value) {
        activeLayer.handleSliderValue(sliderIndex, value);
    }
    
    public void handleIndexButton(final int index, final boolean pressed) {
        if (pressed) {
            activeLayer.handleGridButton(index, pressed);
        }
    }
    
    @Override
    public void exit() {
        
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
