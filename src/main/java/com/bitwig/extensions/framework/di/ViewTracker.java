package com.bitwig.extensions.framework.di;

public interface ViewTracker {
    void fireViewChanged(String source, int trackIndex);

    TrackerRegistration registerViewPositionListener(String origin, ViewPositionListener listener);

}
