package com.bitwig.extensions.controllers.mackie;

public enum VPotMode {
	// TRACK(NoteOnAssignment.V_TRACK, Assign.MIXER), //
	SEND(NoteOnAssignment.V_SEND, Assign.BOTH), //
	PAN(NoteOnAssignment.V_PAN, Assign.MIXER), //
	PLUGIN(NoteOnAssignment.V_PLUGIN, Assign.CHANNEL), //
	EQ(NoteOnAssignment.V_EQ, Assign.CHANNEL), // possibly both
	INSTRUMENT(NoteOnAssignment.V_INSTRUMENT, Assign.CHANNEL),
	MIDI_EFFECT(NoteOnAssignment.V_INSTRUMENT, Assign.CHANNEL);

	public enum Assign {
		MIXER, CHANNEL, BOTH;
	}

	private final NoteOnAssignment buttonAssignment;
	private final Assign assign;

	private VPotMode(final NoteOnAssignment buttonAssignment, final Assign assign) {
		this.buttonAssignment = buttonAssignment;
		this.assign = assign;
	}

	public Assign getAssign() {
		return assign;
	}

	public NoteOnAssignment getButtonAssignment() {
		return buttonAssignment;
	}

	public String getName() {
		return buttonAssignment.toString();
	}

}
