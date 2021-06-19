package com.bitwig.extensions.remoteconsole;

public class DisabledRemoteConsole implements RemoteConsole {

	@Override
	public void printSysEx(final String prefix, final byte[] data) {
	}

	@Override
	public void println(final String format, final Object... params) {
	}

	@Override
	public String getStackTrace(final int max) {
		return "";
	}

}
