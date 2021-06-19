package com.bitwig.extensions.controllers.mackie;

public class DelayAction {
	private final Runnable execution;
	private final int duration;
	private final String actionId;
	private final long time;

	public DelayAction(final int duration, final String actionId, final Runnable execution) {
		super();
		this.duration = duration;
		this.actionId = actionId;
		this.execution = execution;
		this.time = System.currentTimeMillis();
	}

	public boolean isReady() {
		return System.currentTimeMillis() - this.time > duration;
	}

	public String getActionId() {
		return actionId;
	}

	public void run() {
		this.execution.run();
	}

}
