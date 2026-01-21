package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedEvent;

@Component
public class XoneMidiProcessor {
    private final ControllerHost host;
    private int layerMode = 0;
    private final List<IntConsumer> modeChangeListeners = new ArrayList<>();
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final List<XoneMidiDevice> midiDevices = new ArrayList<>();
    private final boolean usesLayer;
    
    protected static class InternalRgbColor {
        protected int red;
        protected int green;
        protected int blue;
        protected int brightness;
        
        public InternalRgbColor() {
        }
        
        public void set(final int red, final int green, final int blue, final int brightness) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.brightness = brightness;
        }
    }
    
    
    public XoneMidiProcessor(final ControllerHost host, final XoneK3GlobalStates globalStates) {
        this.host = host;
        
        for (int i = 0; i < globalStates.getDeviceCount(); i++) {
            midiDevices.add(new XoneMidiDevice(i, host.getMidiInPort(i), host.getMidiOutPort(i)));
        }
        usesLayer = globalStates.usesLayers();
        //        if (globalStates.usesLayers()) {
        //            midiIn.setMidiCallback(this::handleMidiInLayers);
        //        } else {
        //            midiIn.setMidiCallback(this::handleMidiIn);
        //        }
    }
    
    public XoneMidiDevice getMidiDevice(final int index) {
        return midiDevices.get(index);
    }
    
    public void init() {
        host.scheduleTask(() -> this.processMidi(), 100);
        midiDevices.forEach(xoneMidiDevice -> xoneMidiDevice.init());
        //        if (usesLayer) {
        //            midiOut.sendSysex("F0 00 00 1A 50 15 00 07 F7");  // Reqeust Set up
        //        }
    }
    
    private void changeLayerMode(final int newMode) {
        this.layerMode = newMode;
        modeChangeListeners.forEach(l -> l.accept(layerMode));
    }
    
    public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
        return host.createRelativeHardwareControlStepTarget(//
            host.createAction(() -> consumer.accept(1), () -> "+"),
            host.createAction(() -> consumer.accept(-1), () -> "-"));
    }
    
    
    private void processMidi() {
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        host.scheduleTask(this::processMidi, 50);
    }
    
}
