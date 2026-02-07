package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightId;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;

public class LaunchLight {
    
    private final int index;
    private final int midiId;
    private final LaunchControlMidiProcessor midiProcessor;
    private RgbColor lastValue = RgbColor.OFF;
    private final LightId lightId;
    
    public LaunchLight(final String name, final int index, final int midiId, final HardwareSurface surface,
        final LaunchControlMidiProcessor midiProcessor) {
        this.midiId = midiId;
        this.index = index;
        this.midiProcessor = midiProcessor;
        this.lightId = new LightId(index);
        final MultiStateHardwareLight light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + index);
        light.state().onUpdateHardware(this::updateStateCC);
    }
    
    private void updateStateCC(final InternalHardwareLightState state) {
        if (state instanceof final RgbState rgbState) {
            //            switch (rgbState.getState()) {
            //                case NORMAL:
            //                    midiProcessor.sendMidi(0xB0, midiId, rgbState.getColorIndex());
            //                    break;
            //                case FLASHING:
            //                    midiProcessor.sendMidi(0xB0, midiId, rgbState.getColorIndex());
            //                    midiProcessor.sendMidi(0xB1, midiId, rgbState.getAltColor());
            //                    break;
            //                case PULSING:
            //                    midiProcessor.sendMidi(0xB2, midiId, rgbState.getColorIndex());
            //                    break;
            //            }
        } else {
            //midiProcessor.sendMidi(0xB0, midiId, 0);
        }
    }
    
    public LightId getLightId() {
        return lightId;
    }
    
    public void sendRgbColor(final RgbColor color) {
        this.lastValue = color;
        midiProcessor.sendRgb(index + 0xD, color);
    }
    
    public void forceUpdate() {
        sendRgbColor(this.lastValue);
    }
}
