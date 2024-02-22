package com.bitwig.extensions.controllers.mcu.definitions.ssl;

import java.util.UUID;

public class SslUf8Plus1ExtenderExtensionDefinition extends SslUf8ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ebd7dcbe-7093-4f80-8a8a-a77308ab20a2");
    
    public SslUf8Plus1ExtenderExtensionDefinition() {
        super(1, 0);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
