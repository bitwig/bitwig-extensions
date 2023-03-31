package com.bitwig.extensions.framework.time;

public class TimeRepeatEvent extends TimedEvent {
    private final int repeatTime;
    private long repeatTimer = -1;

    public TimeRepeatEvent(final Runnable doneAction, final long delayTime, final int repeatTime) {
        super(doneAction, delayTime);
        this.repeatTime = repeatTime;
    }

    @Override
    public void process() {
        if (completed) {
            return;
        }
        final long passedTime = System.currentTimeMillis() - startTime;
        if (repeatTimer == -1 && passedTime > delayTime) {
            timedAction.run();
            repeatTimer = System.currentTimeMillis();
        } else if (repeatTimer > 0 && (System.currentTimeMillis() - repeatTimer) >= repeatTime) {
            timedAction.run();
            repeatTimer = System.currentTimeMillis();
        }
    }
}
