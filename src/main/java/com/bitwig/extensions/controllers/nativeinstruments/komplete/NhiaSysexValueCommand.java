package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.MidiOut;

/**
 * SYSEX command to send values to the display.
 */
public class NhiaSysexValueCommand extends NhiaSysexCommand {
	private final byte[] dataArray;

	public NhiaSysexValueCommand(final int commandId) {
		dataArray = new byte[BASE_FORMAT.length];
		System.arraycopy(BASE_FORMAT, 0, dataArray, 0, BASE_FORMAT.length);
		dataArray[10] = (byte) commandId;
	}

	public void send(final MidiOut midiOut, final int track, final int value) {
		dataArray[11] = (byte) value;
		dataArray[12] = (byte) track;
		midiOut.sendSysex(dataArray);
	}

	public void send(final MidiOut midiOut, final int track, final boolean value) {
		dataArray[11] = value ? ON : OFF;
		dataArray[12] = (byte) track;
		midiOut.sendSysex(dataArray);
	}

}
