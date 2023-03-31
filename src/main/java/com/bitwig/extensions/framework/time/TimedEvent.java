package com.bitwig.extensions.framework.time;

/**
 * An Event that can be processed at later time. The stop watch starts at creation of the event.
 * The event needs to be queued and the queue repeatedly invokes the process method.
 * Once the given time has passed, the event is executed and will be removed from the queue.
 */
public class TimedEvent {
    protected final long startTime;
    protected final Runnable timedAction;
    protected boolean completed;
    protected final long delayTime;

    public TimedEvent(final Runnable timedAction, final long delayTime) {
        startTime = System.currentTimeMillis();
        completed = false;
        this.delayTime = delayTime;
        this.timedAction = timedAction;
    }

    public void cancel() {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void process() {
        if (completed) {
            return;
        }
        final long passedTime = System.currentTimeMillis() - startTime;
        if (passedTime >= delayTime) {
            timedAction.run();
            completed = true;
        }
    }
}
