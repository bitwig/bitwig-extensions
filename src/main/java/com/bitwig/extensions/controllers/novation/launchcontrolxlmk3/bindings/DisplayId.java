package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings;


import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;

public record DisplayId(int index, DisplayControl display) {
    //
    
    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        final DisplayId displayId = (DisplayId) o;
        return index == displayId.index;
    }
    
    @Override
    public int hashCode() {
        return index;
    }
}
