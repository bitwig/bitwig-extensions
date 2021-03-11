package com.bitwig.extensions.remoteconsole;

public interface RemoteConsole {
	public static final RemoteConsole out = new RemoteConsoleActive();

	void printSysEx(String prefix, byte[] data);

	void println(String format, Object... params);

	String getStackTrace(final int max);
}