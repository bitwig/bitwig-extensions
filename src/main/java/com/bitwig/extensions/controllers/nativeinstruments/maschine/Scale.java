package com.bitwig.extensions.controllers.nativeinstruments.maschine;

public class Scale {
	private final String name;
	private final int[] intervalls;

	public Scale(final String name, final int... notes) {
		super();
		this.name = name;
		this.intervalls = notes;
	}

	public String getName() {
		return name;
	}

	public int[] getIntervalls() {
		return intervalls;
	}

	public int getNextNote(final int startNote, final int baseNote, final int amount) {
		final int noteIndex = (startNote + 12 - baseNote) % 12;
		int octave = startNote < baseNote ? (startNote - baseNote - 12) / 12 : (startNote - baseNote) / 12;

		final int index = findScaleIndex(noteIndex, intervalls);

		int nextIndex = index + amount;
		if (nextIndex >= intervalls.length) {
			nextIndex = 0;
			octave++;
		} else if (nextIndex < 0) {
			nextIndex = intervalls.length - 1;
			octave--;
		}
		return intervalls[nextIndex] + baseNote + octave * 12;
	}

	private static int findScaleIndex(final int noteIndex, final int[] intervalls) {
		for (int i = 0; i < intervalls.length; i++) {
			if (intervalls[i] >= noteIndex) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Given a starting note, determines the highest note at the end of the range.
	 *
	 * @param startNote starting note
	 * @param noteRange available notes
	 * @return last note in range
	 */
	public int highestNote(final int startNote, final int noteRange) {
		final int octaves = noteRange / intervalls.length;
		final int lastvalue = intervalls[(noteRange - 1) % intervalls.length];
		return startNote + octaves * 12 + lastvalue;
	}

}
