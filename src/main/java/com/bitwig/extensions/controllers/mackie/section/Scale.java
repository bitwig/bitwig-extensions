package com.bitwig.extensions.controllers.mackie.section;

public enum Scale {
	CHROMATIC("Chromatic", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
	MAJOR("Major", 0, 2, 4, 5, 7, 9, 11), //
	MINOR("Major", 0, 2, 3, 5, 7, 8, 10), //
	PENTATONIC("Pentatonic", 0, 2, 4, 7, 9), //
	PENTA_MINOR("Pentatonic Minor", 0, 3, 5, 7, 10), //
	DORIAN("Dorian", 0, 2, 3, 5, 7, 9, 10);

	private final String name;
	private final int[] notes;

	Scale(final String name, final int... notes) {
		this.name = name;
		this.notes = notes;
	}

	public String getName() {
		return name;
	}

	public int[] getNotes() {
		return notes;
	}

}
