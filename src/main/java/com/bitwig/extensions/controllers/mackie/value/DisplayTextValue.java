package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

public class DisplayTextValue {
	private final String[] lines = { "", "" };
	private final List<Listener> listeners = new ArrayList<>();

	public interface Listener {
		void valueChanged(int index, String value);
	}

	public String getLine(final int index) {
		if (index < lines.length) {
			return lines[index];
		}
		return "";
	}

	public void setLine(final int index, final String value) {
		if (index < lines.length) {
			lines[index] = value;
			listeners.forEach(listener -> listener.valueChanged(index, value));
		}
	}

	public void addListener(final Listener listener) {
		listeners.add(listener);
	}

}
