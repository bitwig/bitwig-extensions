package com.bitwig.extensions.controllers.mcu.control;

import java.util.Optional;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.McuAssignments;
import com.bitwig.extensions.controllers.mcu.display.ControllerDisplay;
import com.bitwig.extensions.controllers.mcu.value.TrackColor;
import com.bitwig.extensions.framework.di.Context;

public class MixerSectionHardware {
    private final MidiProcessor midiProcessor;
    private final int index;
    private final MotorSlider[] sliders = new MotorSlider[8];
    private final RingEncoder[] encoders = new RingEncoder[8];
    private final McuButton[] armButtons = new McuButton[8];
    private final McuButton[] soloButtons = new McuButton[8];
    private final McuButton[] muteButtons = new McuButton[8];
    private final McuButton[] selectButtons = new McuButton[8];
    private final MotorSlider masterFader;
    private MultiStateHardwareLight backgroundColoring;
    
    public MixerSectionHardware(final int index, final Context context, final MidiProcessor midiProcessor,
        final int offset) {
        final HardwareSurface surface = context.getService(HardwareSurface.class);
        final ControllerConfig config = context.getService(ControllerConfig.class);
        final TimedProcessor timedProcessor = context.getService(TimedProcessor.class);
        this.index = index;
        this.midiProcessor = midiProcessor;
        
        for (int i = 0; i < 8; i++) {
            sliders[i] = new MotorSlider(surface, midiProcessor, i);
            sliders[i].addTouchAction(midiProcessor::handleTouch);
            encoders[i] = new RingEncoder(surface, midiProcessor, i);
            armButtons[i] =
                new McuButton(McuAssignments.REC_BASE.getNoteNo() + i, "ARM_%d".formatted(i + 1 + offset), surface,
                    midiProcessor, timedProcessor);
            soloButtons[i] =
                new McuButton(McuAssignments.SOLO_BASE.getNoteNo() + i, "SOLO_%d".formatted(i + 1 + offset), surface,
                    midiProcessor, timedProcessor);
            muteButtons[i] =
                new McuButton(McuAssignments.MUTE_BASE.getNoteNo() + i, "MUTE_%d".formatted(i + 1 + offset), surface,
                    midiProcessor, timedProcessor);
            selectButtons[i] =
                new McuButton(McuAssignments.SELECT_BASE.getNoteNo() + i, "SELECT_%d".formatted(i + 1 + offset),
                    surface, midiProcessor, timedProcessor);
        }
        masterFader = config.getMasterFaderChannel() > 0
            ? new MotorSlider(surface, midiProcessor, config.getMasterFaderChannel())
            : null;
        if (masterFader != null) {
            masterFader.addTouchAction(midiProcessor::handleTouch);
        }
        
        if (config.hasIconTrackColoring()) {
            backgroundColoring = surface.createMultiStateHardwareLight("BACKGROUND_COLOR_" + "%d".formatted(index));
            backgroundColoring.state().onUpdateHardware(state -> {
                if (state instanceof TrackColor color) {
                    midiProcessor.updateIconColors(color.getColors());
                }
            });
        }
    }
    
    public void clearAll() {
        for (int i = 0; i < 8; i++) {
            armButtons[i].clear(midiProcessor);
            soloButtons[i].clear(midiProcessor);
            muteButtons[i].clear(midiProcessor);
            selectButtons[i].clear(midiProcessor);
        }
    }
    
    public McuButton getButtonFromGridBy2Lane(final int row, final int column) {
        if (column < 8) {
            return switch (row) {
                case 0 -> soloButtons[column];
                case 1 -> muteButtons[column];
                default -> null;
            };
        }
        return null;
    }
    
    public McuButton getButtonFromGridBy4Lane(final int row, final int column) {
        if (column < 8) {
            return switch (row) {
                case 0 -> armButtons[column];
                case 1 -> soloButtons[column];
                case 2 -> muteButtons[column];
                case 3 -> selectButtons[column];
                default -> null;
            };
        }
        return null;
    }
    
    
    public Optional<MultiStateHardwareLight> getBackgroundColoring() {
        return Optional.ofNullable(backgroundColoring);
    }
    
    
    public int getIndex() {
        return index;
    }
    
    public ControllerDisplay getDisplay() {
        return midiProcessor;
    }
    
    public MotorSlider getSlider(final int index) {
        return sliders[index];
    }
    
    public McuButton getArmButton(final int index) {
        return armButtons[index];
    }
    
    public McuButton getSoloButton(final int index) {
        return soloButtons[index];
    }
    
    public McuButton getSelectButton(final int index) {
        return selectButtons[index];
    }
    
    public McuButton getMuteButton(final int index) {
        return muteButtons[index];
    }
    
    public RingEncoder getRingEncoder(final int index) {
        return encoders[index];
    }
    
    public Optional<MotorSlider> getMasterFader() {
        return Optional.ofNullable(masterFader);
    }
    
    
}
