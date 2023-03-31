package com.bitwig.extensions.framework.di;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewTrackerImpl implements ViewTracker {
    private final List<String> listeners = new ArrayList<>();
    private final Map<String, ViewPositionListener> listenerMap = new HashMap<>();

    @Override
    public void fireViewChanged(final String source, final int trackIndex) {
        for (final String origin : listeners) {
            if (!source.equals(origin)) {
                listenerMap.get(origin).handlePositionChanged(source, trackIndex);
            }
        }
    }

    @Override
    public TrackerRegistration registerViewPositionListener(final String origin, final ViewPositionListener listener) {
        listeners.remove(origin);
        listeners.add(origin);
        listenerMap.put(origin, listener);
        return () -> {
            listeners.remove(origin);
            listenerMap.remove(origin);
        };
    }

}
