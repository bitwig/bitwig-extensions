package com.bitwig.extensions.controllers.arturia.minilab3;

public enum PadBank {
    BANK_A(0, 0x30, 0x34, "Bank A"),
    BANK_B(1, 0x40, 0x44, "Bank B"),
    TRANSPORT(-1, 0x50, 0x54, "Transport");

    private final int commandId;
    private final int firstPadId;
    private final int index;
    private final String displayString;

    PadBank(final int index, final int commandId, final int firstPadId, final String displayString) {
        this.index = index;
        this.commandId = commandId;
        this.firstPadId = firstPadId;
        this.displayString = displayString;
    }

    public int getCommandId() {
        return commandId;
    }

    public int getIndex() {
        return index;
    }

    public int getFirstPadId() {
        return firstPadId;
    }

    public String displayName()
    {
        return displayString;
    }
}
