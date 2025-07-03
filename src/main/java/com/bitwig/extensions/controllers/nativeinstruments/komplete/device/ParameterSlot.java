package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RemoteControl;

public class ParameterSlot {
    
    private final int index;
    private final RemoteControl parameter;
    private String name = "";
    private boolean exists;
    private double origin;
    private int discreteSteps;
    private final RelativeHardwareControlBinding binding;
    private boolean onPlugin;
    
    public ParameterSlot(final int index, final RemoteControl parameter, final RelativeHardwareControlBinding binding) {
        this.index = index;
        this.parameter = parameter;
        this.binding = binding;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getName() {
        return name;
    }
    
    public int getType() {
        if (origin == 0.5) {
            return 1;
        }
        if (discreteSteps == 2) {
            return 2;
        }
        if (discreteSteps > 2 && discreteSteps < 16) {
            return 3;
        }
        return 0;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public boolean isExists() {
        return exists;
    }
    
    public void setExists(final boolean exists) {
        this.exists = exists;
    }
    
    public void setOnPlugin(final boolean onPlugin) {
        this.onPlugin = onPlugin;
        setSensitivity();
    }
    
    public void setValueCount(final int values) {
        this.discreteSteps = values;
        setSensitivity();
    }
    
    public void setOrigin(final double origin) {
        this.origin = origin;
    }
    
    private void setSensitivity() {
        if (discreteSteps < 1 || onPlugin) {
            binding.setSensitivity(0.4);
        } else {
            final double sens = Math.min(0.005 * discreteSteps, 0.4);
            binding.setSensitivity(sens);
        }
    }
    
    public void applyFineAdjust(final boolean shift) {
        if (discreteSteps < 1) {
            binding.setSensitivity(shift ? 0.125 : 0.4);
        }
    }
}
