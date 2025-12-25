package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;

public class LineDisplay {
    
    private final MpkMidiProcessor midiProcessor;
    
    private static class Line {
        private String text;
        private int colorIndex;
        private MpkDisplayFont fontStyle = MpkDisplayFont.PT24;
    }
    
    private Line[] lines;
    private boolean active;
    
    public LineDisplay(MpkMidiProcessor midiProcessor, int lines) {
        this.midiProcessor = midiProcessor;
        this.lines = new Line[lines];
        for(int i=0;i<lines;i++) {
            this.lines[i] = new Line();
        }
    }
    
    public void setText(int index, String text) {
        this.lines[index].text = text;
        if(active) {
            midiProcessor.setText(index, this.lines[index].fontStyle, this.lines[index].text);
        }
    }
    
    public void setColorIndex(int index, int colorIndex) {
        this.lines[index].colorIndex = colorIndex;
        if(active) {
            midiProcessor.setDisplayColor( index, this.lines[index].colorIndex);
        }
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        if(active) {
            // TODO udpate all the lines
        }
    }
    
    public boolean isActive() {
        return active;
    }
}
