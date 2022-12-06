package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

public class DisplayUtil {

	private static String[] noteValues = { "C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B " };

	public static String toNote(final int midiNote) {
		final int octave = midiNote / 12;
		final int base = midiNote % 12;
		return noteValues[base] + Integer.toString(octave - 2);
	}

	public static String padString(final String name, final int max) {
		if (name.length() == max) {
			return name;
		}
		if (name.length() > max) {
			return name.substring(0, max);
		}
		final StringBuilder b = new StringBuilder(name);
		for (int i = name.length(); i < max; i++) {
			b.append(' ');
		}
		return b.toString();
	}

	public static String padValue(final int value, final int max) {
		final String stringValue = Integer.toString(value);
		return padString(stringValue, max);
	}

	public static String padZerosValue(final int value, final int max) {
		final String stringValue = Integer.toString(value);
		if (stringValue.length() == max) {
			return stringValue;
		}
		if (stringValue.length() > max) {
			return stringValue.substring(0, max);
		}
		final StringBuilder b = new StringBuilder();
		for (int i = stringValue.length(); i < max; i++) {
			b.append('0');
		}
		b.append(stringValue);
		return b.toString();
	}

	public static String beatsFormatted(final double len) {
		final int bars = (int) (len / 4.0);
		final int beats = (int) len % 4;
		return padZerosValue(bars, 2) + ":" + padZerosValue(beats, 2);
	}

}
