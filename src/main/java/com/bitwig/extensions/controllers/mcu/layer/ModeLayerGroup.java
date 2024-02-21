package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.bindings.display.DisplayTarget;
import com.bitwig.extensions.controllers.mcu.bindings.display.ParameterValueDisplayBinding;
import com.bitwig.extensions.controllers.mcu.bindings.display.StringDisplayBinding;
import com.bitwig.extensions.controllers.mcu.control.MotorSlider;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.control.RingEncoder;
import com.bitwig.extensions.controllers.mcu.devices.ParamPageSlot;
import com.bitwig.extensions.controllers.mcu.devices.SpecificDevice;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.controllers.mcu.value.DoubleValueConverter;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;


public class ModeLayerGroup {
    
    private final ControlMode mode;
    private final Layer faderLayer;
    private final Layer encoderLayer;
    private final Layer displayLabelLayer;
    private final Layer displayValueLayer;
    private final int sectionIndex;
    
    public ModeLayerGroup(final ControlMode mode, final Layers layers, final int index) {
        this.mode = mode;
        this.sectionIndex = index;
        this.faderLayer = new Layer(layers, "%s_FADER_%d".formatted(mode, index));
        this.encoderLayer = new Layer(layers, "%s_ENCODER_%d".formatted(mode, index));
        this.displayLabelLayer = new Layer(layers, "%s_DISPLAY_LABEL_%d".formatted(mode, index));
        this.displayValueLayer = new Layer(layers, "%s_DISPLAY_VALUE_%d".formatted(mode, index));
    }
    
    public void bindControls(final MotorSlider slider, final RingEncoder encoder, final RingDisplayType ringType,
        final Parameter parameter) {
        slider.bindParameter(this.faderLayer, parameter);
        encoder.bindParameter(this.encoderLayer, parameter, ringType);
    }
    
    public void bindDisplay(final DisplayManager displayManager, final StringValue labelValue,
        final BooleanValue exists, final Parameter parameter, final int index) {
        displayLabelLayer.addBinding(
            new StringDisplayBinding(displayManager, mode, DisplayTarget.of(DisplayRow.LABEL, index, sectionIndex),
                labelValue, exists, name -> StringUtil.reduceAscii(name, 6)));
        displayValueLayer.addBinding(
            new StringDisplayBinding(displayManager, mode, DisplayTarget.of(DisplayRow.VALUE, index, sectionIndex),
                parameter.displayedValue(), exists));
    }
    
    public void bindValue(final DisplayManager displayManager, final StringValue labelValue, final BooleanValue exists,
        final Parameter parameter, final int index, final DoubleValueConverter valueConverter) {
        displayLabelLayer.addBinding(
            new StringDisplayBinding(displayManager, mode, DisplayTarget.of(DisplayRow.LABEL, index, sectionIndex),
                labelValue, exists, name -> StringUtil.reduceAscii(name, 6)));
        displayValueLayer.addBinding(new ParameterValueDisplayBinding(displayManager, mode,
            DisplayTarget.of(DisplayRow.VALUE, index, sectionIndex), parameter, valueConverter));
    }
    
    
    public void bindControls(final SpecificDevice device, final MotorSlider slider, final RingEncoder encoder,
        final int paramIndex) {
        final ParamPageSlot slot = device.getParamPageSlot(paramIndex);
        slider.bindParameter(faderLayer, slot);
        encoder.bindParameter(encoderLayer, slot);
    }
    
    public void bindDisplay(final SpecificDevice device, final DisplayManager displayManager, final int index) {
        final ParamPageSlot slot = device.getParamPageSlot(index);
        displayLabelLayer.addBinding(
            new StringDisplayBinding(displayManager, mode, DisplayTarget.of(DisplayRow.LABEL, index, sectionIndex),
                slot.getNameValue(), slot.getExistsValue(), name -> StringUtil.reduceAscii(name, 7)));
        displayValueLayer.addBinding(
            new StringDisplayBinding(displayManager, mode, DisplayTarget.of(DisplayRow.VALUE, index, sectionIndex),
                slot.getDisplayValue(), slot.getExistsValue(), name -> StringUtil.reduceAscii(name, 7)));
    }
    
    
    public Layer getFaderLayer() {
        return faderLayer;
    }
    
    public Layer getEncoderLayer() {
        return encoderLayer;
    }
    
    public Layer getDisplayLabelLayer() {
        return displayLabelLayer;
    }
    
    public Layer getDisplayValueLayer() {
        return displayValueLayer;
    }
}
