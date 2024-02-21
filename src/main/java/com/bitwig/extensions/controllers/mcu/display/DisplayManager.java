package com.bitwig.extensions.controllers.mcu.display;

import java.util.List;
import java.util.Optional;

import com.bitwig.extensions.controllers.mcu.layer.ControlMode;

public class DisplayManager {
    private final ControllerDisplay display;
    private ControlMode upperMode = ControlMode.PAN;
    private ControlMode lowerMode = ControlMode.VOLUME;
    
    public DisplayManager(final ControllerDisplay display) {
        this.display = display;
    }
    
    public void sendText(final int rowIndex, final int cellIndex, final String text) {
        display.showText(DisplayPart.UPPER, rowIndex, cellIndex, text);
    }
    
    public void sendText(final ControlMode mode, final int rowIndex, final int cellIndex, final String text) {
        getTargetDisplay(mode).ifPresent(part -> {
            //            if (cellIndex == 0) {
            //                McuExtension.println(" DEBUG %d : %s  mode=%s => part=%s", rowIndex, text, mode, part);
            //            }
            display.showText(part, rowIndex, cellIndex, text);
        });
    }
    
    public void sendText(final ControlMode mode, final int rowIndex, final String text) {
        getTargetDisplay(mode).ifPresent(part -> {
            display.showText(part, rowIndex, text);
        });
    }
    
    public void sendText(final ControlMode mode, final int rowIndex, final List<String> texts) {
        getTargetDisplay(mode).ifPresent(part -> {
            display.showText(part, rowIndex, texts);
        });
    }
    
    public void registerModeAssignment(final ControlMode upperMode, final ControlMode lowerMode) {
        //McuExtension.println("ASSIGNE UP = %s  Low = %s", upperMode, lowerMode);
        this.upperMode = upperMode;
        this.lowerMode = lowerMode;
    }
    
    private Optional<DisplayPart> getTargetDisplay(final ControlMode mode) {
        if (mode == ControlMode.MENU) {
            return Optional.of(DisplayPart.UPPER);
        }
        if (display.hasLower()) {
            if (mode == upperMode) {
                return Optional.of(DisplayPart.UPPER);
            } else if (mode == lowerMode) {
                return Optional.of(DisplayPart.LOWER);
            }
        } else {
            return Optional.of(DisplayPart.UPPER);
        }
        return Optional.empty();
    }
    
    public void sendVuUpdate(final int index, final int value) {
        display.sendVuUpdate(index, value);
    }
}
