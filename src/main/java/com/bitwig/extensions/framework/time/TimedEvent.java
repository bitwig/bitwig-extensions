package com.bitwig.extensions.framework.time;

/**
 * An Event that can be processed at later time. The stop watch starts at creation of the event.
 * The event needs to be queued and the queue repeatedly invokes the process method.
 * Once the given time has passed, the event is executed and will be removed from the queue.
 */
public interface TimedEvent {
   boolean isCompleted();

   void cancel();

   void process();
}
