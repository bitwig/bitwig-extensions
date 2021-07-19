package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.layer.SectionType;

/**
 * Represents 2x56 LCD display on the MCU or an extender.
 *
 */
public class LcdDisplay {
	private static final int DISPLAY_LEN = 55;
	private static final int ROW2_START = 56;

	private final byte[] rowDisplayBuffer = { //
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
	public String sysHead;

	private final String[][] lastSendGrids = new String[][] { { "", "", "", "", "", "", "", "" },
			{ "", "", "", "", "", "", "", "" } };
	private final String[] lastSentRows = new String[] { "", "" };
	private final boolean[] fullTextMode = new boolean[] { false, false };

	private final MidiOut midiOut;

	private VuMode vuMode;

	/**
	 * @param driver  the parent
	 * @param midiOut the MIDI out destination for the Display
	 * @param type    the main unit or a an extenter
	 */
	public LcdDisplay(final MackieMcuProExtension driver, final MidiOut midiOut, final SectionType type) {
		this.midiOut = midiOut;
		if (type == SectionType.XTENDER) {
			rowDisplayBuffer[4] = 0x15;
			segBuffer[4] = 0x15;
			sysHead = "f0 00 00 66 15 ";
		} else {
			sysHead = "f0 00 00 66 14 ";
		}
		setVuMode(driver.getVuMode());
	}

	public void setFullTextMode(final int row, final boolean fullTextMode) {
		final boolean vuDisabledPrev = isFullModeActive();
		this.fullTextMode[row] = fullTextMode;
		final boolean vuDisabledNow = isFullModeActive();
		if (vuDisabledPrev != vuDisabledNow) {
			if (fullTextMode) {
				if (this.vuMode != VuMode.LED) {
					switchVuMode(VuMode.LED);
				}
			} else {
				if (this.vuMode != VuMode.LED) {
					switchVuMode(vuMode);
				}
			}
		}
		refreshDisplay();
	}

	private boolean isFullModeActive() {
		return this.fullTextMode[0] | this.fullTextMode[1];
	}

	public void setVuMode(final VuMode mode) {
		this.vuMode = mode;
		if (!isFullModeActive()) {
			switchVuMode(mode);
			refreshDisplay();
		}
	}

	private void switchVuMode(final VuMode mode) {
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
	}

	private void resetGrids(final int row) {
		for (int cell = 0; cell < lastSendGrids[row].length; cell++) {
			lastSendGrids[row][cell] = "      ";
		}
	}

	public void centerText(final int row, final String text) {
		sendToDisplay(row, pad4Center(text));
	}

	private static String pad4Center(final String text) {
		final int fill = DISPLAY_LEN - text.length();
		if (fill < 0) {
			return text.substring(0, DISPLAY_LEN);
		}
		if (fill < 2) {
			return text;
		}
		return StringUtil.padString(text, fill / 2);
	}

	public void sendToDisplay(final int row, final String text) {
		if (text.equals(lastSentRows[row])) {
			return;
		}
		lastSentRows[row] = text;
		resetGrids(row);
		sendFullRow(row, text);
	}

	private void sendFullRow(final int row, final String text) {
		rowDisplayBuffer[6] = (byte) (row * ROW2_START);
		final char[] ca = text.toCharArray();
		for (int i = 0; i < DISPLAY_LEN; i++) {
			rowDisplayBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
		}
		midiOut.sendSysex(rowDisplayBuffer);
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

	public void sendToRowFull(final int row, final int segment, final String text) {
		if (row > 1 || row < 0) {
			return;
		}
		if (!text.equals(lastSendGrids[row][segment])) {
			lastSendGrids[row][segment] = text;
			sendTextSegFull(row, segment, text);
		}
	}

	private void sendTextSegFull(final int row, final int segment, final String text) {
		segBuffer[6] = (byte) (row * ROW2_START + segment * 7);
		final char[] ca = text.toCharArray();
		for (int i = 0; i < 7; i++) {
			segBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
		}
		midiOut.sendSysex(segBuffer);
	}

	private void sendTextSeg(final int row, final int segment, final String text) {
		segBuffer[6] = (byte) (row * ROW2_START + segment * 7);
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
			if (fullTextMode[row]) {
				sendFullRow(row, lastSentRows[row]);
			} else {
				for (int segment = 0; segment < 8; segment++) {
					sendTextSeg(row, segment, lastSendGrids[row][segment]);
				}
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
		sendToDisplay(1, "");
	}

	public void exitMessage() {
		midiOut.sendSysex(sysHead + "62 f7");
		centerText(0, "Bitwig Studio");
		centerText(1, "... not running ...");
	}

	public void clearText() {
		sendToDisplay(0, "");
		sendToDisplay(1, "");
	}

}
