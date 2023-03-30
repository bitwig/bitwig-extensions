package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import java.util.Arrays;

public enum ControlMode {

    NONE(-1, -1, -1, -1, -1),
    VOLUME(0, 0, 20, 9, 0),
    PAN(1, 1, 30, -1, 1),
    SENDS(2, 0, 40, 13, 2),
    DEVICE(3, 0, 50, 61, 3);

    private final int bankId;
    private final int type;
    private final int ccNr;
    private final int color;
    private final int pageId;

    ControlMode(final int bankId, final int type, final int ccNr, final int color, final int pageId) {
        this.bankId = bankId;
        this.type = type;
        this.ccNr = ccNr;
        this.color = color;
        this.pageId = pageId;
    }

    public int getBankId() {
        return bankId;
    }

    public int getType() {
        return type;
    }

    public int getCcNr() {
        return ccNr;
    }

    public int getColor() {
        return color;
    }

    public boolean hasFaders() {
        return bankId != -1;
    }
    
    public static ControlMode fromPageId(int pageId) {
        return Arrays.stream(ControlMode.values()) //
            .filter(mode->mode.pageId==pageId) //
            .findFirst() //
            .orElse(ControlMode.NONE);
    }
}
