package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public enum NoteOnAssignment {
	PLAY(94), //
	STOP(93), //
	RECORD(95), //
	REWIND(91), //
	FFWD(92), //
	AUTO_WRITE(75), //
	AUTO_READ_OFF(74), //
	TRIM(76), TOUCH(77), LATCH(78), GROUP(79), //
	SOLO_BASE(8), REC_BASE(0), MUTE_BASE(16), SELECT_BASE(24), //
	ENCPRESS_BASE(32), //
	SIGNAL_BASE(104), //
	TOUCH_VOLUME(104), //
	SHIFT(70), OPTION(71), CONTROL(72), ALT(73), UNDO(81), SAVE(80), ENTER(83), MARKER(84), NUDGE(85), CYCLE(86),
	DROP(87), REPLACE(88), CLICK(89), SOLO(90), //
	FLIP(50), DIPLAY_NAME(52), DISPLAY_SMPTE(53), BEATS_MODE(114), SMPTE_MODE(113), //
	V_TRACK(40), V_SEND(41), V_PAN(42), V_PLUGIN(43), V_EQ(44), V_INSTRUMENT(45), //
	F1(54), F2(55), F3(56), F4(57), F5(58), F6(59), F7(60), F8(61), //
	CURSOR_UP(96), CURSOR_DOWN(97), CURSOR_LEFT(98), CURSOR_RIGHT(91), //
	ZOOM(100), SCRUB(101), //
	BANK_LEFT(46), BANK_RIGH(47), TRACK_LEFT(48), TRACK_RIGHT(49);

	private final int notNr;
	private final int channel;

	private NoteOnAssignment(final int noteNo, final int channel) {
		this.notNr = noteNo;
		this.channel = channel;
	}

	private NoteOnAssignment(final int notNo) {
		this.notNr = notNo;
		this.channel = 0;
	}

	public int getNoteNo() {
		return notNr;
	}

	public int getChannel() {
		return channel;
	}

	public int getType() {
		return Midi.NOTE_ON | channel;
	}

	public void holdActionAssign(final MidiIn midiIn, final HardwareButton button) {
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, notNr));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, notNr));
	}

	public void pressActionAssign(final MidiIn midiIn, final HardwareButton button) {
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, notNr));
	}

	public void send(final MidiOut midiOut, final int value) {
		midiOut.sendMidi(Midi.NOTE_ON | channel, notNr, value);
	}

}
