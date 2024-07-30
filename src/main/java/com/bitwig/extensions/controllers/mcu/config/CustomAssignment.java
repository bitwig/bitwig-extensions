package com.bitwig.extensions.controllers.mcu.config;

import java.util.Objects;

public class CustomAssignment implements ButtonAssignment {
    
    private final int noteNo;
    private final int channel;
    private final McuFunction function;
    
    public CustomAssignment(final McuFunction function, final int noteNo, final int channel) {
        this.function = function;
        this.noteNo = noteNo;
        this.channel = channel;
    }
    
    @Override
    public int getNoteNo() {
        return noteNo;
    }
    
    @Override
    public int getChannel() {
        return channel;
    }
    
    public McuFunction getFunction() {
        return function;
    }
    
    @Override
    public String toString() {
        return function.toString();
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CustomAssignment that = (CustomAssignment) o;
        return noteNo == that.noteNo && channel == that.channel;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(noteNo, channel);
    }
}
