package com.bitwig.extensions.controllers.mcu.control;

import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.framework.values.Midi;

public class RingDisplay {
	private final MidiProcessor midi;
	private final int index;
	private int lastValue = -1;

	public RingDisplay(final MidiProcessor midi, final int index) {
		this.index = index;
		this.midi = midi;
	}

	public int getIndex() {
		return index;
	}

	public void sendValue(final int value, final boolean showDot) {
		final int newValue = value | (showDot ? 0x40 : 0x00);
		if (newValue != lastValue) {
			midi.sendMidi(Midi.CC, 0x30 | index, newValue);
			lastValue = value;
		}
	}

	public void refresh() {
		midi.sendMidi(Midi.CC, 0x30 | index, lastValue);
	}

	public void clear() {
		midi.sendMidi(Midi.CC, 0x30 | index, 0);
	}

}
