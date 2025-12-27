package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.framework.time.TimedDelayEvent;

public class LineDisplay {
    
    private final MpkMidiProcessor midiProcessor;
    private int currentLayer;
    
    private TimedDelayEvent fallbackEvent = null;
    
    private static class Line {
        private String text = "";
        private int colorIndex = 0;
        private MpkDisplayFont fontStyle = MpkDisplayFont.PT24;
    }
    
    private final Line[] lines;
    private boolean active;
    
    public LineDisplay(final MpkMidiProcessor midiProcessor, final MpkDisplayFont fontStyle, final int lines) {
        this.midiProcessor = midiProcessor;
        this.lines = new Line[lines];
        for (int i = 0; i < lines; i++) {
            this.lines[i] = new Line();
            this.lines[i].fontStyle = fontStyle;
        }
    }
    
    public void setText(final int rowIndex, final String text, final MpkDisplayFont font, final int colorIndex) {
        this.lines[rowIndex].text = text;
        this.lines[rowIndex].fontStyle = font;
        this.lines[rowIndex].colorIndex = colorIndex;
        if (active) {
            midiProcessor.setText(rowIndex, this.lines[rowIndex].fontStyle, text);
            midiProcessor.setDisplayColor(rowIndex, colorIndex);
        }
    }
    
    public void setText(final int layer, final int rowIndex, final String text) {
        if (layer == 0) {
            this.lines[rowIndex].text = text;
        } else if (layer != currentLayer) {
            return;
        }
        if (active) {
            midiProcessor.setText(rowIndex, this.lines[rowIndex].fontStyle, text);
        }
    }
    
    public void setColorIndex(final int layer, final int index, final int colorIndex) {
        if (layer == 0) {
            this.lines[index].colorIndex = colorIndex;
        } else if (layer != currentLayer) {
            return;
        }
        if (active) {
            midiProcessor.setDisplayColor(index, colorIndex);
        }
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        if (active) {
            updateCurrent();
        }
    }
    
    public void updateCurrent() {
        for (int i = 0; i < lines.length; i++) {
            midiProcessor.setDisplayColor(i, this.lines[i].colorIndex);
            midiProcessor.setText(i, this.lines[i].fontStyle, this.lines[i].text);
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void activateTemporary(final int layer) {
        this.currentLayer = layer;
        if (fallbackEvent != null) {
            fallbackEvent.cancel();
        }
        fallbackEvent = new TimedDelayEvent(() -> resetLayer(), 2000);
        midiProcessor.queueEvent(fallbackEvent);
    }
    
    private void resetLayer() {
        this.currentLayer = 0;
        this.updateCurrent();
        this.fallbackEvent = null;
    }
}
