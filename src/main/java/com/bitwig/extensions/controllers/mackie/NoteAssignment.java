package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public interface NoteAssignment {

	int getNoteNo();

	int getType();

	int getChannel();

	default void holdActionAssign(final MidiIn midiIn, final HardwareButton button) {
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(getChannel(), getNoteNo()));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(getChannel(), getNoteNo()));
	}

	default void pressActionAssign(final MidiIn midiIn, final HardwareButton button) {
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(getChannel(), getNoteNo()));
	}

	default void send(final MidiOut midiOut, final int value) {
		midiOut.sendMidi(Midi.NOTE_ON | getChannel(), getNoteNo(), value);
	}

}