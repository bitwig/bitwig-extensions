package com.bitwig.extensions.controllers.akai.apc.common;

import java.util.Arrays;

public enum OrientationFollowType {
    AUTOMATIC("Automatic", "Auto"), //
    FIXED_VERTICAL("Mix Panel Layout", "Mixer"), //
    FIXED_HORIZONTAL("Arrange Panel Layout", "Arrange");

    private final String label;
    private final String shortLabel;

    OrientationFollowType(final String label, final String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public static OrientationFollowType toType(final String value) {
        return Arrays.stream(OrientationFollowType.values())
                .filter(type -> type.label.equals(value))
                .findFirst()
                .orElse(OrientationFollowType.FIXED_VERTICAL);
    }
}
