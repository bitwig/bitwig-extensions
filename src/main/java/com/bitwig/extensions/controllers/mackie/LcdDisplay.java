package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.layer.ChannelSection.SectionType;

public class LcdDisplay {
	private final byte[] displayBuffer = { //
			(byte) 0XF0, 0, 0, 0X66, 0x14, 0x12, 0, // z: the grid number Zone number 0-3 * 28
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
			0, 0, 0, 0, 0, (byte) 247 };
	private final byte[] segBuffer = { //
			(byte) 240, 0, 0, 102, 20, 18, 0, // z: the grid number Zone number 0-3 * 28
			0, 0, 0, 0, 0, 0, 0, // 7: 10 Chars
			(byte) 247 };
	public String sysHead = "f0 00 00 66 14 ";
	private final String[][] lastSendGrids = new String[][] { { "", "", "", "", "", "", "", "" },
			{ "", "", "", "", "", "", "", "" } };

	private final MidiOut midiOut;

	public LcdDisplay(final MackieMcuProExtension driver, final MidiOut midiOut, final SectionType type) {
		this.midiOut = midiOut;
		if (type == SectionType.XTENDER) {
			displayBuffer[4] = 0x15;
			segBuffer[4] = 0x15;
			sysHead = "f0 00 00 66 15 ";
		}
		appyVuMode(driver.getVuMode());
	}

	public void appyVuMode(final VuMode mode) {
		switch (mode) {
		case LED:
			midiOut.sendSysex(sysHead + "21 01 f7"); // Vertical VU
			for (int i = 0; i < 8; i++) {
				midiOut.sendMidi(Midi.CHANNEL_AT, i << 4, 0);
				midiOut.sendSysex(sysHead + "20 0" + i + " 01 f7");
			}
			break;
		case LED_LCD_VERTICAL:
			midiOut.sendSysex(sysHead + "21 01 f7"); // Vertical VU
			for (int i = 0; i < 8; i++) {
				midiOut.sendSysex(sysHead + "20 0" + i + " 03 f7");
				midiOut.sendMidi(Midi.CHANNEL_AT, i << 4, 0);
			}
			midiOut.sendSysex(sysHead + "20 00 03 f7");
			break;
		case LED_LCD_HORIZONTAL:
			midiOut.sendSysex(sysHead + "21 00 f7"); // Horizontal VU
			for (int i = 0; i < 8; i++) {
				midiOut.sendSysex(sysHead + "20 0" + i + " 03 f7");
				midiOut.sendMidi(Midi.CHANNEL_AT, i << 4, 0);
			}
			break;
		}
		refreshDisplay();
	}

	public void sendToDisplay(final int zone, final String text) {
		displayBuffer[6] = (byte) zone;
		final char[] ca = text.toCharArray();
		for (int i = 0; i < 55; i++) {
			displayBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
		}
		midiOut.sendSysex(displayBuffer);
	}

	public void sendToRow(final int row, final int segment, final String text) {
		if (row > 1 || row < 0) {
			return;
		}
		if (!text.equals(lastSendGrids[row][segment])) {
			lastSendGrids[row][segment] = text;
			sendTextSeg(row, segment, text);
		}
	}

	private void sendTextSeg(final int row, final int segment, final String text) {
		// RemoteConsole.out.println(" SEND r={} s={} txt={}", row, segment, text);
		segBuffer[6] = (byte) (row * 56 + segment * 7);
		final char[] ca = text.toCharArray();
		for (int i = 0; i < 6; i++) {
			segBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
		}
		if (segment < 7) {
			segBuffer[13] = ' ';
		}
		midiOut.sendSysex(segBuffer);
	}

	public void refreshDisplay() {
		for (int row = 0; row < 2; row++) {
			for (int segment = 0; segment < 8; segment++) {
				sendTextSeg(row, segment, lastSendGrids[row][segment]);
			}
		}
	}

	public void sendChar(final int index, final char cx) {
//		segBuffer[6] = (byte) index;
//		for (int i = 0; i < 6; i++) {
//			segBuffer[i + 7] = (byte) cx;
//		}
//		midiOut.sendSysex(segBuffer);
		// midiOut.sendSysex(SYS_HEAD + "12 00 03 03 03 03 03 03 f7");
//		midiOut.sendSysex(SYS_HEAD + "0a 01 f7");
		// midiOut.sendSysex(SYS_HEAD + "20 00 01 f7");
		// midiOut.sendMidi(Midi.CHANNEL_AT, 0x08, 0);
		midiOut.sendMidi(Midi.CC, 0x30, cx);
	}

	public void clearAll() {
		midiOut.sendSysex(sysHead + "62 f7");
		sendToDisplay(0, "");
		sendToDisplay(55, "");
	}

	public void exitMessage() {
		midiOut.sendSysex(sysHead + "62 f7");
		sendToDisplay(0, "                     Bitwig Studio ");
		sendToDisplay(55, "            ");
	}

}
