package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

public enum ControlMode {
    NONE,
    VOLUME,
    PAN,
    SENDS,
    SENDS_A(SENDS),
    SENDS_B(SENDS),
    DEVICE;

    private final ControlMode refMode;

    ControlMode() {
        refMode = this;
    }

    ControlMode(final ControlMode refMode) {
        this.refMode = refMode;
    }

    public ControlMode getRefMode() {
        return refMode;
    }
}
