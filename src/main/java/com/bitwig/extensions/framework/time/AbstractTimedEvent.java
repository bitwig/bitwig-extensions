package com.bitwig.extensions.framework.time;

public abstract class AbstractTimedEvent implements TimedEvent {
    protected long startTime;
    protected boolean completed;
    protected final long delayTime;
    
    public AbstractTimedEvent(final long delayTime) {
        startTime = System.currentTimeMillis();
        completed = false;
        this.delayTime = delayTime;
    }
    
    public void resetTime() {
        startTime = System.currentTimeMillis();
    }
    
    public void cancel() {
        completed = true;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
}
