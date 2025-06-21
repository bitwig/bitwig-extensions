package com.bitwig.extensions.controllers.novation.slmk3;

import java.util.Arrays;

import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;

public enum DeviceType {
    
    NOTE_EFFECT("note-effect", SlRgbState.BITWIG_BLUE),
    INSTRUMENT("instrument", SlRgbState.YELLOW),
    AUDIO_EFFECT("audio_to_audio", SlRgbState.ORANGE),
    AUDIO_EFFECT2("audio-effect", SlRgbState.ORANGE);
    String id;
    SlRgbState color;
    
    DeviceType(final String id, final SlRgbState color) {
        this.id = id;
        this.color = color;
    }
    
    public String getId() {
        return id;
    }
    
    public static DeviceType toType(final String id) {
        return Arrays.stream(DeviceType.values()).filter(type -> type.id.equals(id)).findFirst().orElse(null);
    }
    
    public SlRgbState getColor() {
        return color;
    }
}
