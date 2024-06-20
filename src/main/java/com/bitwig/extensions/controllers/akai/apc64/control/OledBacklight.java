package com.bitwig.extensions.controllers.akai.apc64.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Supplier;

public class OledBacklight {

    private final MultiStateHardwareLight light;
    private final MidiProcessor midiProcessor;
    private final int midiId;

    public OledBacklight(HardwareSurface hwSurface, MidiProcessor midiProcessor, int midiId) {
        light = hwSurface.createMultiStateHardwareLight("OLED_COLOR_" + midiId);
        this.midiProcessor = midiProcessor;
        this.midiId = midiId;
        light.state().onUpdateHardware(this::updateState);
    }

    // Touch State Base 0x68
    // 0 - Off
    // 1 - V white
    // 2 - V red
    // 3 - P white
    // 4 - P red

    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof RgbLightState) {
            RgbLightState state = (RgbLightState) internalHardwareLightState;
            //midiProcessor.sendMidi(0xB0, 0x68, 1);
            midiProcessor.sendMidi(0xB0, midiId, state.getColorIndex());
        }
    }

    public int getState() {
        return ((RgbLightState) light.state().currentValue()).getColorIndex();
    }

    public void bind(Layer layer, Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }

}
