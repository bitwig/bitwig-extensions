package com.bitwig.extensions.controllers.arturia.minilab3;

public enum PadBank {
    BANK_A(0, 0x30, 0x34),
    BANK_B(1, 0x40, 0x44),
    TRANSPORT(-1, 0x50, 0x54);

    private final int commandId;
    private final int firstPadId;
    private final int index;

    PadBank(final int index, final int commandId, final int firstPadId) {
        this.index = index;
        this.commandId = commandId;
        this.firstPadId = firstPadId;
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
}
