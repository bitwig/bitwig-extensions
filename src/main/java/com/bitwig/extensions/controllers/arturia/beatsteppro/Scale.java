package com.bitwig.extensions.controllers.arturia.beatsteppro;

public enum Scale {
    CHROMATIC("Chromatic", new int[]{0, 2, 4, 5, 7, 9, 11, 12, 1, 3, -1, 6, 8, 10, -1, -1}, false),
    DRUM("Drum", new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, true),
    MAJOR("Major", new int[]{0, 2, 4, 5, 7, 9, 11, 12}, false),
    MINOR("Minor", new int[]{0, 2, 3, 5, 7, 8, 10, 12}, false),
    DORIAN("Dorian", new int[]{0, 2, 3, 5, 7, 9, 10, 12}, false),
    MIXOLYDIAN("Mixolydian", new int[]{0, 2, 4, 5, 7, 9, 10, 12}, false),
    HARM_MINOR("Harm Minor", new int[]{0, 2, 3, 5, 7, 8, 11, 12}, false),
    BLUES("Blues", new int[]{0, 3, 5, 6, 7, 10, 12, 15}, false),
    ;

    private final String name;
    private final int[] offsets;
    private final boolean isDrum;

    Scale(final String name, final int[] offsets, final boolean drum) {
        this.name = name;
        this.offsets = offsets;
        isDrum = drum;
    }

    public String getName() {
        return name;
    }

    public boolean isDrum() {
        return isDrum;
    }

    public int getOffset(final int index) {
        if (index < offsets.length) {
            return offsets[index];
        }
        if (offsets.length == 8) {
            return offsets[index % 8] + 12;
        }
        return -1;
    }
}
