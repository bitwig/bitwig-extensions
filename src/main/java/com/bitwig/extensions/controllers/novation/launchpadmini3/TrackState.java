package com.bitwig.extensions.controllers.novation.launchpadmini3;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.framework.di.Component;

@Component
public class TrackState {
    private final int[] colorIndex;
    private final boolean[] exists;
    private int cursorColor = ColorLookup.toColor(0, 0, 0);
    
    private final List<BoolStateListener> existsListeners = new ArrayList<>();
    private final List<ColorStateListener> colorListeners = new ArrayList<>();
    
    public interface BoolStateListener {
        void changed(int trackIndex, boolean state);
    }
    
    public interface ColorStateListener {
        void changed(int trackIndex, int color);
    }
    
    public TrackState(final ViewCursorControl viewCursorControl) {
        final TrackBank trackBank = viewCursorControl.getTrackBank();
        final int sizeOfBank = trackBank.getSizeOfBank();
        
        final CursorTrack cursorTrack = viewCursorControl.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> cursorColor = ColorLookup.toColor(r, g, b));
        colorIndex = new int[sizeOfBank];
        exists = new boolean[sizeOfBank];
        for (int i = 0; i < sizeOfBank; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            track.color().addValueObserver((r, g, b) -> handleColorChanged(trackIndex, ColorLookup.toColor(r, g, b)));
            track.exists().addValueObserver(exists -> handleExistsChanged(trackIndex, exists));
        }
    }
    
    public void addColorStateListener(final ColorStateListener colorStateListener) {
        colorListeners.add(colorStateListener);
    }
    
    public void addExistsListener(final BoolStateListener listener) {
        existsListeners.add(listener);
    }
    
    private void handleColorChanged(final int trackIndex, final int color) {
        colorIndex[trackIndex] = color;
        colorListeners.forEach(l -> l.changed(trackIndex, color));
    }
    
    private void handleExistsChanged(final int trackIndex, final boolean exists) {
        this.exists[trackIndex] = exists;
        existsListeners.forEach(l -> l.changed(trackIndex, exists));
    }
    
    public int[] getColorIndex() {
        return colorIndex;
    }
    
    public boolean[] getExists() {
        return exists;
    }
    
    public int getCursorColor() {
        return cursorColor;
    }
    
    public int getColorOfTrack(final int index) {
        return colorIndex[index];
    }
    
}
