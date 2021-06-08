package com.bitwig.extensions.controllers.mackie;

public enum RingDisplayType {
	PAN_FILL(17), FILL_LR(33), SINGLE(1), CENTER_FILL(49);
	private final int offset;

	private RingDisplayType(final int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}
}