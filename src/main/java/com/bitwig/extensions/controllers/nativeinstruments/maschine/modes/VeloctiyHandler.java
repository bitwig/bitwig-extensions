package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.BooleanValueObject;

public class VeloctiyHandler {
	protected final Integer[] velTable = new Integer[128];
	protected final BooleanValueObject fixed = new BooleanValueObject(); // Needs to shared with Pad Mode

	private int fixedVelocity = 100;

	public VeloctiyHandler() {
		for (int i = 0; i < 128; i++) {
			velTable[i] = Integer.valueOf(i);
		}
	}

	public int getFixedVelocity() {
		return fixedVelocity;
	}

	public void assingTranslationTable(final NoteInput noteInput) {
		noteInput.setVelocityTranslationTable(velTable);
	}

	public BooleanValueObject getFixed() {
		return fixed;
	}

	public void toggleFixedValue(final NoteInput noteInput) {
		if (fixed.get()) {
			fixed.toggle();
			for (int i = 0; i < 128; i++) {
				velTable[i] = Integer.valueOf(i);
			}
		} else {
			fixed.toggle();
			for (int i = 0; i < 128; i++) {
				velTable[i] = Integer.valueOf(127);
			}
		}
		noteInput.setVelocityTranslationTable(velTable);
	}

	public void inc(final int incval) {
		fixedVelocity = Math.min(Math.max(1, fixedVelocity + incval), 127);
	}

}
