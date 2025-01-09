package com.bitwig.extensions.controllers.reloop.display;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.reloop.MidiProcessor;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ScreenManager {
    private final Map<ScreenMode, Screen> screens = new HashMap<>();
    private ScreenMode mode = ScreenMode.HOME;
    private final ControllerHost host;
    private final ScreenBuffer screenBuffer;
    
    public ScreenManager(final MidiProcessor midiProcessor, final ControllerHost host) {
        this.screenBuffer = midiProcessor.getScreenBuffer();
        Arrays.stream(ScreenMode.values()).forEach(mode -> screens.put(mode, new Screen(mode, screenBuffer)));
        screens.get(mode).setActive(true);
        this.host = host;
    }
    
    public void start() {
        tick();
    }
    
    private void tick() {
        if (mode != ScreenMode.HOME && screens.get(mode).timeSinceUpdate() > 1500) {
            setMode(ScreenMode.HOME);
        }
        this.screenBuffer.updateDisplay();
        host.scheduleTask(this::tick, 35);
    }
    
    public void updateTrackName(final String trackName) {
        screens.get(ScreenMode.HOME).setText(0, trackName);
        screens.get(ScreenMode.DEVICE_PARAMETER).setText(0, trackName);
    }
    
    public void updateSceneName(final String name) {
        screens.get(ScreenMode.HOME).setText(1, name);
    }
    
    public void updateDeviceName(final String deviceName) {
        screens.get(ScreenMode.DEVICE_PARAMETER).setText(1, deviceName);
        screens.get(ScreenMode.HOME).setText(2, deviceName);
    }
    
    public void updatePageName(final String pageName) {
        screens.get(ScreenMode.HOME).setText(3, pageName);
    }
    
    public void changeParameter(final ScreenMode mode, final String targetName, final String name, final String value) {
        if (mode != this.mode) {
            setMode(mode);
        }
        final Screen screen = screens.get(mode);
        if (mode == ScreenMode.MIXER_PARAMETER) {
            screen.setText(0, targetName);
            screen.setText(1, "");
            screen.setText(2, name);
            screen.setText(3, value);
        } else if (mode == ScreenMode.DEVICE_PARAMETER) {
            screen.setText(1, targetName);
            screen.setText(2, name);
            screen.setText(3, value);
        }
        screen.notifyUpdated();
    }
    
    public Screen getScreen(final ScreenMode mode) {
        return screens.get(mode);
    }
    
    public void setMode(final ScreenMode mode) {
        screens.get(mode).setActive(false);
        this.mode = mode;
        screens.get(mode).setActive(true);
        screens.get(mode).updateAll();
    }
    
    
    public void updateCurrent() {
        screens.get(mode).updateAll();
    }
    
}
