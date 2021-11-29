package com.bitwig.extensions.controllers.mackie.section;

public enum Scale {
	CHROMATIC("Chromatic", "Chrom", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
	MAJOR("Major", "Major", 0, 2, 4, 5, 7, 9, 11), //
	MINOR("Minor", "Minor", 0, 2, 3, 5, 7, 8, 10), //
	PENTATONIC("Pentatonic", "Penta", 0, 2, 4, 7, 9), //
	PENTA_MINOR("Pentatonic Minor", "PenMin", 0, 3, 5, 7, 10), //
	DORIAN("Dorian", "Dorian", 0, 2, 3, 5, 7, 9, 10);

	private final String name;
	private final int[] notes;
	private final String shortName;

	Scale(final String name, final String shortName, final int... notes) {
		this.name = name;
		this.notes = notes;
		this.shortName = shortName;
	}

	public String getName() {
		return name;
	}

	public int[] getNotes() {
		return notes;
	}

	public String getShortName() {
		return shortName;
	}

}
