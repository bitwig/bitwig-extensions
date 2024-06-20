package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.control.RingEncoder;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.value.IEnumDisplayValue;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class MenuBuilder {
    private static final BasicStringValue EMPTY = new BasicStringValue("");
    private final LayerGroup layerGroup;
    private final DisplayManager displayManager;
    private final MixerSectionHardware hwElements;
    private int index = 0;
    private final GlobalStates globalStates;
    
    public MenuBuilder(final String name, final Context context, final MixerSectionHardware hwElements,
        final DisplayManager displayManager) {
        this.displayManager = displayManager;
        this.layerGroup = new LayerGroup(context, name);
        this.globalStates = context.getService(GlobalStates.class);
        this.hwElements = hwElements;
    }
    
    public LayerGroup getLayerGroup() {
        fillRest();
        return layerGroup;
    }
    
    private void fillRest() {
        while (index < 8) {
            fillNext();
        }
    }
    
    public void fillNext() {
        if (index >= 8) {
            return;
        }
        layerGroup.bindEmpty(displayManager, hwElements.getRingEncoder(index), index);
        index++;
    }
    
    public void addLabelMenu(final StringValue label, final StringValue value) {
        if (index >= 8) {
            return;
        }
        layerGroup.bindDisplay(displayManager, label, value, index);
        layerGroup.bindEncoderEmpty(hwElements.getRingEncoder(index));
        index++;
    }
    
    public void addEnumValue(final String label, final IEnumDisplayValue enumValue) {
        if (index >= 8) {
            return;
        }
        layerGroup.bindDisplay(displayManager, label, enumValue.getDisplayValue(), index);
        layerGroup.bindRingValue(hwElements.getRingEncoder(index), enumValue.getRingValue());
        layerGroup.bindEncoderIncrement(hwElements.getRingEncoder(index), enumValue::increment, 0.1);
        // TODO Button Press Option need to be added
        index++;
    }
    
    public void addToggleParameterMenu(final String name, final SettableBooleanValue value) {
        // Consider Ring encoder turn to set value
        if (index >= 8) {
            return;
        }
        layerGroup.bindDisplay(displayManager, name, value, index);
        final RingEncoder encoder = hwElements.getRingEncoder(index);
        layerGroup.bindEncoderPressed(encoder, value::toggle);
        layerGroup.bindRingValue(encoder, value);
        layerGroup.bindEncoderTurnedBoolean(encoder, value);
        index++;
    }
    
    public void addToggleParameterMenu(final String name, final Parameter parameter) {
        if (index >= 8) {
            return;
        }
        final BooleanValueObject booleanProxy = new BooleanValueObject();
        booleanProxy.addValueObserver(active -> {
            parameter.value().setImmediately(active ? 1.0 : 0.0);
        });
        parameter.value().addValueObserver(v -> {
            booleanProxy.set(v == 1.0);
        });
        booleanProxy.setDirect(parameter.value().get() == 1.0);
        
        layerGroup.bindDisplay(displayManager, name, parameter, index);
        final RingEncoder encoder = hwElements.getRingEncoder(index);
        layerGroup.bindEncoderPressed(encoder, () -> booleanProxy.toggle());
        layerGroup.bindRingValue(encoder, booleanProxy);
        layerGroup.bindEncoderTurnedBoolean(encoder, booleanProxy);
        index++;
    }
    
    public void addPositionAdjustment(final String name, final SettableBeatTimeValue value,
        final BeatTimeFormatter formatter) {
        if (index >= 8) {
        }
        final BasicStringValue valueString = new BasicStringValue("");
        final int cueIndex = index;
        final RingEncoder encoder = hwElements.getRingEncoder(cueIndex);
        final BooleanValueObject encoderPressed = new BooleanValueObject();
        value.addValueObserver(position -> valueString.set(value.getFormatted(formatter)));
        layerGroup.bindRingToIsPressed(encoder);
        layerGroup.bindEncoderIsPressed(encoder, pressed -> encoderPressed.set(pressed));
        layerGroup.bindDisplay(displayManager, name, valueString, cueIndex);
        layerGroup.bindEncoderIncrement(encoder, inc -> {
            final double position = value.get();
            value.set(position + (encoderPressed.get() ? 0.25 : 1.0) * inc);
        }, 0.25);
        index++;
    }
    
    public void addCueMenu(final CueMarker cueMarker, final Transport transport, final BeatTimeFormatter formatter) {
        if (index >= 8) {
            return;
        }
        // TODO ADD SHIFT Option
        final BasicStringValue name = new BasicStringValue("");
        final BasicStringValue value = new BasicStringValue("---");
        final int cueIndex = index;
        
        cueMarker.exists().addValueObserver(exist -> {
            updateCueMarkerValue(cueMarker, formatter, value, exist);
            updateCueMarkerLabel(cueMarker, name, cueIndex, cueMarker.name().get(), exist);
        });
        cueMarker.position().addValueObserver(position -> {
            updateCueMarkerValue(cueMarker, formatter, value, cueMarker.exists().get());
        });
        cueMarker.name().addValueObserver(
            newName -> updateCueMarkerLabel(cueMarker, name, cueIndex, newName, cueMarker.exists().get()));
        
        layerGroup.bindDisplay(displayManager, name, value, cueIndex);
        final RingEncoder encoder = hwElements.getRingEncoder(cueIndex);
        layerGroup.bindRingValue(encoder, cueMarker.exists());
        layerGroup.bindEncoderPressed(encoder, () -> {
            if (cueMarker.exists().get()) {
                transport.getPosition().set(cueMarker.position().get());
            } else {
                transport.addCueMarkerAtPlaybackPosition();
            }
        });
        layerGroup.bindEncoderIncrement(encoder, inc -> {
            final double position = cueMarker.position().get();
            cueMarker.position().set(position + 0.25 * inc);
        }, 0.25);
        index++;
    }
    
    private static void updateCueMarkerValue(final CueMarker cueMarker, final BeatTimeFormatter formatter,
        final BasicStringValue value, final boolean exists) {
        if (exists) {
            value.set(cueMarker.position().getFormatted(formatter));
        } else {
            value.set("----");
        }
    }
    
    private static void updateCueMarkerLabel(final CueMarker cueMarker, final BasicStringValue value, final int index,
        final String nameValue, final boolean exists) {
        if (exists) {
            if (nameValue.isEmpty() || nameValue.equals("Untitled")) {
                value.set("<Cue%d>".formatted(index + 1));
            } else {
                value.set(nameValue);
            }
        } else {
            value.set("[Cue%d]".formatted(index + 1));
        }
    }
    
    public void addActionMenu(final String name, final Runnable action) {
        if (index >= 8) {
            return;
        }
        // Maybe Label with press
        // Value shows state
        layerGroup.bindDisplay(displayManager, name, EMPTY, index);
        layerGroup.bindEncoderPressed(hwElements.getRingEncoder(index), action);
        layerGroup.bindRingToIsPressed(hwElements.getRingEncoder(index));
        index++;
    }
    
    public void addIncParameter(final Parameter parameter, final double incrementMultiplier) {
        if (index >= 8) {
            return;
        }
        final BooleanValueObject pressTurn = new BooleanValueObject();
        layerGroup.bindControlsInc(hwElements.getRingEncoder(index), dir -> modifyRaw(dir, pressTurn, parameter),
            parameter, RingDisplayType.SINGLE, incrementMultiplier);
        layerGroup.bindEncoderIsPressed(hwElements.getRingEncoder(index), pressTurn);
        layerGroup.bindDisplay(displayManager, parameter, index);
        index++;
    }
    
    private void modifyRaw(final int diff, final BooleanValueObject modifier, final Parameter parameter) {
        parameter.setRaw(parameter.getRaw() + (modifier.get() ? 0.1 * diff : diff));
    }
    
    public void addValue(final String label, final SettableRangedValue parameter, final RingDisplayType type,
        final double sensitivity) {
        if (index >= 8) {
            return;
        }
        layerGroup.bindControls(hwElements.getRingEncoder(index), type, parameter, sensitivity);
        layerGroup.bindEncoderPressed(hwElements.getRingEncoder(index), () -> parameter.set(0));
        layerGroup.bindDisplay(displayManager, label, parameter, index);
        index++;
    }
    
    public void addStepValue(final RelativeHardwarControlBindable value, final Runnable pressAction,
        final String label) {
        if (index >= 8) {
            return;
        }
        layerGroup.bindControls(hwElements.getRingEncoder(index), value);
        if (pressAction != null) {
            layerGroup.bindPressAction(hwElements.getRingEncoder(index), pressAction);
        }
        layerGroup.bindDisplay(displayManager, label, EMPTY, index);
        layerGroup.bindRingEmpty(hwElements.getRingEncoder(index));
        
        index++;
    }
    
    public void addStepIncDecValue(final HardwareActionBindable incAction, final HardwareActionBindable decAction,
        final String label) {
        if (index >= 8) {
            return;
        }
        final RingEncoder ringEncoder = hwElements.getRingEncoder(index);
        layerGroup.bindEncoderIncrement(ringEncoder, incAction, decAction, 1.0);
        layerGroup.bindDisplay(displayManager, label, EMPTY, index);
        layerGroup.bindRingEmpty(ringEncoder);
        index++;
    }
}
