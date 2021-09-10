package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.Device;

public enum VPotMode {
	// TRACK(NoteOnAssignment.V_TRACK, Assign.MIXER), //
	SEND(NoteOnAssignment.V_SEND, Assign.BOTH), //
	PAN(NoteOnAssignment.V_PAN, Assign.MIXER), //
	PLUGIN(NoteOnAssignment.V_PLUGIN, Assign.CHANNEL, "audio-effect", "PLUGIN"), //
	EQ(NoteOnAssignment.V_EQ, Assign.CHANNEL, "EQ+ device", "EQ", "EQ+"), // possibly both
	INSTRUMENT(NoteOnAssignment.V_INSTRUMENT, Assign.CHANNEL, "instrument", "INSTRUMENT"),
	MIDI_EFFECT(NoteOnAssignment.GV_MIDI, Assign.CHANNEL, "note-effect", "MIDI TRACKS"),
	ARPEGGIATOR(NoteOnAssignment.V_INSTRUMENT, Assign.CHANNEL, "note-effect", "ALT+INSTRUMENT");

	public enum Assign {
		MIXER, CHANNEL, BOTH;
	}

	private final NoteOnAssignment buttonAssignment;
	private final Assign assign;
	private final String typeName;
	private final String deviceName;
	private final String typeDisplayName;
	private final String buttonDescription;

	private VPotMode(final NoteOnAssignment buttonAssignment, final Assign assign, final String typeName,
			final String buttonDescription, final String deviceName) {
		this.buttonAssignment = buttonAssignment;
		this.assign = assign;
		this.typeName = typeName;
		this.deviceName = deviceName;
		if (this.typeName != null && this.typeName.length() > 1) {
			this.typeDisplayName = this.typeName.substring(0, 1).toUpperCase() + this.typeName.substring(1);
		} else {
			this.typeDisplayName = null;
		}
		this.buttonDescription = buttonDescription;
	}

	private VPotMode(final NoteOnAssignment buttonAssignment, final Assign assign, final String typeName,
			final String buttonDescription) {
		this(buttonAssignment, assign, typeName, buttonDescription, null);
	}

	private VPotMode(final NoteOnAssignment buttonAssignment, final Assign assign) {
		this(buttonAssignment, assign, null, null);
	}

	public static VPotMode fittingMode(final Device device) {
		return fittingMode(device.deviceType().get(), device.name().get());
	}

	public static VPotMode fittingMode(final String typeName, final String deviceName) {
		final VPotMode[] values = VPotMode.values();
		for (final VPotMode value : values) {
			if (value.deviceName != null && value.deviceName.equals(deviceName)) {
				return value;
			}
		}
		for (final VPotMode value : values) {
			if (value.deviceName == null && value.typeName != null && value.typeName.equals(typeName)) {
				return value;
			}
		}
		return null;
	}

	public String getDeviceName() {
		return deviceName;
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

	public String getTypeName() {
		return typeName;
	}

	public boolean isDeviceMode() {
		return typeName != null;
	}

	public String getTypeDisplayName() {
		return typeDisplayName;
	}

	public String getButtonDescription() {
		return buttonDescription;
	}

}
