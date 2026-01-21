package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneEncoder;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.controllers.allenheath.xonek3.layer.GridMode;
import com.bitwig.extensions.controllers.allenheath.xonek3.layer.LayerCollection;
import com.bitwig.extensions.controllers.allenheath.xonek3.layer.LayerId;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.EnumeratorValue;

public class XoneK3ControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private HardwareSurface surface;
    private Context diContext;
    
    private final EnumeratorValue<LayerMode> mixerMode = new EnumeratorValue<>(LayerMode.values());
    private final EnumeratorValue<GridMode> gridMode = new EnumeratorValue<>(GridMode.values());
    
    private LayerCollection layerCollection;
    private final XoneK3GlobalStates globalStates;
    private ViewControl viewControl;
    private Parameter tempo;
    
    private enum LayerMode {
        MIXER(XoneRgbColor.WHITE),
        TRACK_INDIVIDUAL(XoneRgbColor.WHITE), // XoneRgbColor.PURPLE
        DJ_EQ(XoneRgbColor.WHITE),  // XoneRgbColor.MAGENTA
        DEVICE(XoneRgbColor.ORANGE_REMOTES), // XoneRgbColor.BLUE
        TRACK_REMOTES(XoneRgbColor.ORANGE_REMOTES), // XoneRgbColor.ORANGE
        PROJECT_REMOTES(XoneRgbColor.ORANGE_REMOTES); // XoneRgbColor.YELLOW
        
        private final XoneRgbColor color;
        private final XoneRgbColor dimColor;
        
        LayerMode(final XoneRgbColor color) {
            this.color = color;
            this.dimColor = color.bright(3);
        }
    }
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    protected XoneK3ControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
        final int deviceCount) {
        super(definition, host);
        globalStates = new XoneK3GlobalStates(host, deviceCount);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        diContext = new Context(this);
        diContext.registerService(XoneK3GlobalStates.class, globalStates);
        surface = diContext.getService(HardwareSurface.class);
        layerCollection = diContext.getService(LayerCollection.class);
        viewControl = diContext.getService(ViewControl.class);
        final Transport transport = diContext.getService(Transport.class);
        tempo = transport.tempo();
        tempo.value().markInterested();
        for (int i = 0; i < globalStates.getDeviceCount(); i++) {
            initModifiers(layerCollection.getLayer(LayerId.MAIN), i);
            initMixerControl(layerCollection.getLayer(LayerId.MIXER), i);
        }
        diContext.activate();
        globalStates.activate();
        diContext.getService(XoneMidiProcessor.class).init();
    }
    
    private void initModifiers(final Layer mainLayer, final int deviceIndex) {
        final DeviceHwElements hwElements = diContext.getService(XoneHwElements.class).getDeviceElements(deviceIndex);
        final XoneRgbButton shiftButton = hwElements.getShiftButton();
        shiftButton.bindIsPressed(mainLayer, pressed -> globalStates.getShiftHeld().set(pressed));
        shiftButton.bindLight(
            mainLayer,
            () -> globalStates.getShiftHeld().get() ? XoneRgbColor.WHITE : XoneRgbColor.WHITE_LO);
        
        final Layer layerChooserLayer = layerCollection.getLayer(LayerId.LAYER_CHOOSER);
        final XoneRgbButton scrollEncoderButton = hwElements.getShiftEncoder().getPushButton();
        final Layer gridModeSelectionLayer = layerCollection.getLayer(LayerId.GRID_LAYER_CHOOSER);
        final XoneEncoder layerEncoder = hwElements.getLayerEncoder();
        final XoneRgbButton layerEncoderButton = layerEncoder.getPushButton();
        mixerMode.addValueObserver(newMode -> applyMixerMode());
        
        if (globalStates.usesLayers()) {
            final BooleanValueObject gridModeLayerActive = new BooleanValueObject();
            final int layerEncoderIndex = 3;
            final XoneEncoder layerSelectionEncoder = hwElements.getEncoders().get(layerEncoderIndex);
            final XoneRgbButton layerButton = layerSelectionEncoder.getPushButton();
            layerButton.bindIsPressed(mainLayer, layerChooserLayer::setIsActive);
            layerEncoderButton.bindIsPressed(
                mainLayer, pressed -> gridModeLayerActive.set(pressed && scrollEncoderButton.isPressed().get()));
            scrollEncoderButton.isPressed()
                .addValueObserver(pressed -> gridModeLayerActive.set(pressed && layerEncoderButton.isPressed().get()));
            bindEncoderLayerForLayerMode(hwElements, layerChooserLayer, layerEncoderIndex);
            gridModeLayerActive.addValueObserver(gridModeSelectionLayer::setIsActive);
            bindGridModeSelector(hwElements, gridModeSelectionLayer);
        } else {
            final Layer sceneLaunchlayer = layerCollection.getLayer(LayerId.SCENE_LAUNCHER);
            layerEncoder.bindEncoder(
                mainLayer, createIncrementBinder(inc -> handleMainLayerEncoder(inc, layerEncoderButton)));
            final XoneRgbButton layerButton = hwElements.getLayerButton();
            layerButton.bindLightPressed(mainLayer, XoneRgbColor.WHITE, XoneRgbColor.WHITE_LO);
            layerButton.bindIsPressed(
                mainLayer, pressed -> {
                    sceneLaunchlayer.setIsActive(pressed);
                    layerChooserLayer.setIsActive(pressed);
                });
            if (deviceIndex == 0) {
                bindUserSelectionModes(hwElements, layerChooserLayer);
            } else {
                hwElements.disableKnobButtonSection(layerChooserLayer);
            }
        }
    }
    
    private void bindUserSelectionModes(final DeviceHwElements hwElements, final Layer layer) {
        final List<XoneRgbButton> buttons = hwElements.getKnobButtons();
        bindModeButton(buttons.get(0), layer, LayerMode.MIXER);
        bindModeButton(buttons.get(1), layer, LayerMode.TRACK_INDIVIDUAL);
        bindModeButton(buttons.get(2), layer, LayerMode.DJ_EQ);
        bindModeButton(buttons.get(4), layer, LayerMode.DEVICE);
        bindModeButton(buttons.get(5), layer, LayerMode.TRACK_REMOTES);
        bindModeButton(buttons.get(6), layer, LayerMode.PROJECT_REMOTES);
        bindEmptyButton(buttons.get(3), layer);
        bindEmptyButton(buttons.get(7), layer);
    }
    
    private void bindModeButton(final XoneRgbButton button, final Layer layer, final LayerMode mode) {
        button.bindLight(layer, () -> mixerMode.get() == mode ? mode.color : mode.dimColor);
        button.bindPressed(layer, () -> mixerMode.set(mode));
    }
    
    private void bindEmptyButton(final XoneRgbButton button, final Layer layer) {
        button.bindLight(layer, () -> XoneRgbColor.OFF);
        button.bindPressed(layer, () -> {});
    }
    
    private void bindGridModeSelector(final DeviceHwElements hwElements, final Layer layer) {
        final List<XoneRgbButton> buttons = hwElements.getGridButtons();
        gridMode.addValueObserver(this::handleGridModeChange);
        final GridMode[] values = GridMode.values();
        for (int i = 0; i < values.length; i++) {
            final GridMode mode = values[i];
            final XoneRgbButton button = buttons.get(i);
            button.bindPressed(layer, () -> gridMode.set(mode));
            button.bindLight(layer, () -> gridMode.get() == mode ? XoneRgbColor.BLUE : XoneRgbColor.WHITE_DIM);
        }
        for (int i = values.length; i < 16; i++) {
            final XoneRgbButton button = buttons.get(i);
            button.bindPressed(layer, () -> {});
            button.bindLight(layer, () -> XoneRgbColor.OFF);
        }
    }
    
    private void handleGridModeChange(final GridMode mode) {
        final GridMode[] values = GridMode.values();
        for (final GridMode modeFromValue : values) {
            layerCollection.getLayer(modeFromValue.getLayerId()).setIsActive(mode == modeFromValue);
        }
    }
    
    private void handleMainLayerEncoder(final int inc, final XoneRgbButton button) {
        if (button.isPressed().get()) {
            tempo.incRaw(inc * (globalStates.getShiftHeld().get() ? 1.0 : 0.1));
        } else {
            if (inc > 0) {
                viewControl.getCursorTrack().selectNext();
            } else {
                viewControl.getCursorTrack().selectPrevious();
            }
        }
    }
    
    private void bindEncoderLayerForLayerMode(final DeviceHwElements hwElements, final Layer layer,
        final int layerEncoderIndex) {
        final XoneEncoder layerEncoder = hwElements.getEncoders().get(layerEncoderIndex);
        layerEncoder.bindEncoder(layer, createIncrementBinder(inc -> mixerMode.increment(inc, false)));
        layerEncoder.getPushButton().bindLight(layer, this::getKnobRow12Mode);
        for (int i = 0; i < 4; i++) {
            if (i != layerEncoderIndex) {
                final XoneEncoder encoder = hwElements.getEncoders().get(i);
                final XoneRgbButton encoderButton = encoder.getPushButton();
                encoderButton.bindLight(layer, () -> XoneRgbColor.OFF);
            }
        }
    }
    
    private InternalHardwareLightState getKnobRow12Mode() {
        return switch (mixerMode.get()) {
            case MIXER -> XoneRgbColor.WHITE;
            case DEVICE -> XoneRgbColor.BLUE;
            case DJ_EQ -> XoneRgbColor.MAGENTA;
            case TRACK_INDIVIDUAL -> XoneRgbColor.PURPLE;
            case TRACK_REMOTES -> XoneRgbColor.ORANGE;
            case PROJECT_REMOTES -> XoneRgbColor.YELLOW;
        };
    }
    
    private void applyMixerMode() {
        layerCollection.setActive(LayerId.REMOTES, mixerMode.get() == LayerMode.DEVICE);
        layerCollection.setActive(LayerId.DJ_EQ, mixerMode.get() == LayerMode.DJ_EQ);
        layerCollection.setActive(LayerId.TRACK_REMOTES, mixerMode.get() == LayerMode.TRACK_REMOTES);
        layerCollection.setActive(LayerId.PROJECT_REMOTES, mixerMode.get() == LayerMode.PROJECT_REMOTES);
        layerCollection.setActive(LayerId.IND_REMOTES, mixerMode.get() == LayerMode.TRACK_INDIVIDUAL);
    }
    
    private void initMixerControl(final Layer layer, final int deviceIndex) {
        final DeviceHwElements hwElements = diContext.getService(XoneHwElements.class).getDeviceElements(deviceIndex);
        final ViewControl viewControl = diContext.getService(ViewControl.class);
        final TrackBank trackBank = viewControl.getTrackBank();
        final XoneEncoder navigateEncoder = hwElements.getShiftEncoder();
        final XoneRgbButton button = navigateEncoder.getPushButton();
        navigateEncoder.bindEncoder(layer, createTrackScrollingBinder(button, trackBank));
    }
    
    private RelativeHardwarControlBindable createTrackScrollingBinder(final XoneRgbButton button,
        final TrackBank trackBank) {
        return createIncrementBinder(inc -> {
            if (button.isPressed().get()) {
                trackBank.sceneBank().scrollBy(inc);
            } else {
                trackBank.scrollBy(inc);
            }
        });
    }
    
    @Override
    public void exit() {
        diContext.deactivate();
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
        return getHost().createRelativeHardwareControlStepTarget(//
            getHost().createAction(() -> consumer.accept(1), () -> "+"),
            getHost().createAction(() -> consumer.accept(-1), () -> "-"));
    }
    
    
}
