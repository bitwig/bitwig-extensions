package com.bitwig.extensions.controllers.nativeinstruments.maschine;

public enum MaschineMode {
	MK3("Maschine Mk3", "1600"), PLUS("Maschine Plus", "1820");

	private final String descriptor;

	private final String deviceId;

	private final String sysexShiftDownCommand;
	private final String sysexShiftReleaseCommand;
	private final String sysexReturnFromHostCommand;

	private MaschineMode(final String descriptor, final String deviceId) {
		this.descriptor = descriptor;
		this.deviceId = deviceId;
		this.sysexShiftDownCommand = createCommand("4d", "01");
		this.sysexShiftReleaseCommand = createCommand("4d", "00");
		this.sysexReturnFromHostCommand = createCommand("46", "01");
	}

	public String getDescriptor() {
		return descriptor;
	}

	private String createCommand(final String commandId, final String value) {
		return "f0002109" + deviceId + "4d500001" + commandId + value + "f7";
	}

	public SysexCommandType toCommandType(final String sysString) {
		if (sysString == null) {
			return SysexCommandType.UNKNOWN;
		}
		if (sysString.contentEquals(sysexShiftDownCommand)) {
			return SysexCommandType.SHIFT_DOWN;
		}
		if (sysString.contentEquals(sysexShiftReleaseCommand)) {
			return SysexCommandType.SHIFT_UP;
		}
		if (sysString.contentEquals(sysexReturnFromHostCommand)) {
			return SysexCommandType.RETURN_FROM_HOST;
		}
		return SysexCommandType.UNKNOWN;
	}

}
