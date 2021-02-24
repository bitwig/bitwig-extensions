package com.bitwig.extensions.controllers.nativeinstruments.maschine;

public class TouchHandler {

	private final boolean touchArray[] = new boolean[8];
	private int touchCount = 0;

	public void notifyTouched(final int index, final boolean v) {
		if (v) {
			touchCount++;
		} else if (touchCount > 0) {
			touchCount--;
		}
		this.touchArray[index] = v;
	}

	public boolean isTouched() {
		return touchCount > 0;
	}

	public boolean isTouched(final int index) {
		if (index >= 0 && index < touchArray.length) {
			return this.touchArray[index];
		}
		return false;
	}

	public boolean[] getTouchArray() {
		return touchArray;
	}

}
