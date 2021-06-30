package com.bitwig.extensions.controllers.mackie;

import java.util.function.IntConsumer;

/**
 * This tracks holding down a button over time. Use for fast forward/reverse.
 * While holding down the action is repeated and goes through stages in which
 * the behavior can change over time.
 *
 */
public class HoldMenuButtonState {
	private long startTime = 0;
	private IntConsumer executor = null;
	private long[] times;
	private boolean first = false;

	/**
	 * @return if the action is still active
	 */
	public boolean isRunning() {
		return executor != null;
	}

	/**
	 * Stops the action.
	 */
	public void stop() {
		executor = null;
	}

	/**
	 * Starts/Restarts an action.
	 *
	 * @param consumer the callback that consumes the stage index
	 * @param times    array of time stages in milis, the first being the delay
	 *                 after which the actual repetition begins.
	 */
	public void start(final IntConsumer consumer, final long[] times) {
		startTime = System.currentTimeMillis();
		executor = consumer;
		this.times = times;
		this.first = true;
	}

	/**
	 * Triggers an a single execution. This is meant to be called in intervals.
	 */
	void execute() {
		if (executor == null) {
			return;
		}
		final long runningTime = System.currentTimeMillis() - startTime;
		if (first) {
			executor.accept(0);
			first = false;
		} else if (runningTime > times[0]) {
			int stage = 0;
			for (int i = 1; i < times.length; i++) {
				stage = i - 1;
				if (runningTime < times[i]) {
					break;
				}
			}
			executor.accept(stage);
		}

	}

}
