package com.bitwig.extensions.controllers.novation.slmk3.layer;

import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;

public enum SoftButtonMode {
    MUTE_SOLO("Mute", "Solo", SlRgbState.ORANGE, SlRgbState.YELLOW),
    MONITOR_ARM("Monitor", "Arm", SlRgbState.BLUE, SlRgbState.RED),
    STOP("Stop", "", SlRgbState.RED, SlRgbState.OFF);
    String item1;
    String item2;
    SlRgbState color1;
    SlRgbState color2;
    
    SoftButtonMode(final String item1, final String item2, final SlRgbState color1, final SlRgbState color2) {
        this.item1 = item1;
        this.item2 = item2;
        this.color1 = color1;
        this.color2 = color2;
    }
    
    public String getItem1() {
        return item1;
    }
    
    public String getItem2() {
        return item2;
    }
    
    public SlRgbState getColor1() {
        return color1;
    }
    
    public SlRgbState getColor2() {
        return color2;
    }
}
