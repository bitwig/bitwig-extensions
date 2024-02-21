package com.bitwig.extensions.controllers.mcu;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

@Component
public class TimedProcessor {
    
    private final ControllerHost host;
    private final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final List<Runnable> timedAction = new ArrayList<>();
    private int blinkCounter = 0;
    private TimedDelayEvent holdEvent = null;
    
    public TimedProcessor(final ControllerHost host) {
        this.host = host;
    }
    
    private void handlePing() {
        blinkCounter = (blinkCounter + 1) % 8;
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        for (final Runnable action : timedAction) {
            action.run();
        }
        host.scheduleTask(this::handlePing, 50);
    }
    
    public void addActionListener(final Runnable action) {
        timedAction.add(action);
    }
    
    @Activate
    public void start() {
        host.scheduleTask(this::handlePing, 100);
    }
    
    public boolean blinkSlow() {
        return blinkCounter % 8 < 4;
    }
    
    public boolean blinkMid() {
        return blinkCounter % 4 < 2;
    }
    
    public boolean blinkFast() {
        return blinkCounter % 2 == 0;
    }
    
    public boolean blinkPeriodic() {
        return blinkCounter == 0 || blinkCounter == 3;
    }
    
    
    public void delayTask(final Runnable action, final long delay) {
        host.scheduleTask(action, delay);
    }
    
    public void startHoldEvent(final Runnable delayedAction) {
        if (holdEvent != null) {
            holdEvent.cancel();
        }
        holdEvent = new TimedDelayEvent(delayedAction, 600);
        queueEvent(holdEvent);
    }
    
    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }
    
    public void completeHoldEvent(final Runnable releaseAction) {
        if (holdEvent != null && !holdEvent.isCompleted()) {
            holdEvent.cancel();
            holdEvent = null;
        } else {
            releaseAction.run();
        }
    }
}
