package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.Midi;

public class RingDisplay {
	private final MidiOut midi;
	private final int index;
	private int lastValue = -1;

	public RingDisplay(final MidiOut midi, final int index) {
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

}
