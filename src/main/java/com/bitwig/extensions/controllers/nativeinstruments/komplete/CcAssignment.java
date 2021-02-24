package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

/**
 * Midi CC assignment constants.
 */
public enum CcAssignment {
	PLAY(0x10), //
	RESTART(0x11), //
	REC(0x12), //
	COUNTIN(0x13), //
	STOP(0x14), //
	CLEAR(0x15), //
	LOOP(0x16), //
	METRO(0x17), //
	TAPTEMPO(0x18), //
	UNDO(0x20), //
	REDO(0x21), //
	QUANTIZE(0x22), //
	AUTO(0x23), //
	PRESS_4D_KNOB(0x60), //
	PRESS_4D_KNOB_SHIFT(0x61) //
	;
	private int stateId;

	private CcAssignment(final int stateId) {
		this.stateId = stateId;
	}

	public int getStateId() {
		return stateId;
	};

	public HardwareActionMatcher createActionMatcher(final MidiIn midiIn, final int matchvalue) {
		return midiIn.createCCActionMatcher(15, stateId, matchvalue);
	}

}
