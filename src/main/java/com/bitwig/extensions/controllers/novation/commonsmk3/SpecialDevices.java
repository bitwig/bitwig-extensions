package com.bitwig.extensions.controllers.novation.commonsmk3;

import java.util.UUID;

public enum SpecialDevices {
    DRUM("8ea97e45-0255-40fd-bc7e-94419741e9d1");

    private final UUID uuid;

    SpecialDevices(final String uuid) {
        this.uuid = UUID.fromString(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
