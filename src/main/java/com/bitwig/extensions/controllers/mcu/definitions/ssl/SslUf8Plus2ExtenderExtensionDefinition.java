package com.bitwig.extensions.controllers.mcu.definitions.ssl;

import java.util.UUID;

public class SslUf8Plus2ExtenderExtensionDefinition extends SslUf8ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ebd7dcbe-7093-4f80-8a8a-a77308ab20a3");
    
    public SslUf8Plus2ExtenderExtensionDefinition() {
        super(2, 0);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
