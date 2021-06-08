package com.bitwig.extensions.controllers.mackie;

public enum VPotMode {
	TRACK(NoteOnAssignment.V_TRACK), //
	SEND(NoteOnAssignment.V_SEND), //
	PAN(NoteOnAssignment.V_PAN), //
	PLUGIN(NoteOnAssignment.V_PLUGIN), //
	EQ(NoteOnAssignment.V_EQ), //
	INSTRUMENT(NoteOnAssignment.V_INSTRUMENT);

	private final NoteOnAssignment buttonAssignment;

	private VPotMode(final NoteOnAssignment buttonAssignment) {
		this.buttonAssignment = buttonAssignment;

	}

	public NoteOnAssignment getButtonAssignment() {
		return buttonAssignment;
	}

	public String getName() {
		return buttonAssignment.toString();
	}
}
