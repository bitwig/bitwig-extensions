package com.bitwig.extensions.controllers.reloop.display;

import java.util.Arrays;

public class Screen {
    private final String[] lines = new String[4];
    private final ScreenMode mode;
    private boolean active;
    private final ScreenBuffer screenBuffer;
    private long updateTime = -1;
    
    public Screen(final ScreenMode mode, final ScreenBuffer screenBuffer) {
        this.mode = mode;
        this.screenBuffer = screenBuffer;
        Arrays.fill(lines, "");
    }
    
    public void setText(final int row, final String text) {
        lines[row] = text;
        if (active) {
            screenBuffer.updateLine(row, text);
        }
    }
    
    public void updateAll() {
        if (active) {
            for (int i = 0; i < lines.length; i++) {
                screenBuffer.updateLine(i, lines[i]);
            }
        }
    }
    
    public void notifyUpdated() {
        updateTime = System.currentTimeMillis();
    }
    
    public long timeSinceUpdate() {
        return System.currentTimeMillis() - updateTime;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
}
