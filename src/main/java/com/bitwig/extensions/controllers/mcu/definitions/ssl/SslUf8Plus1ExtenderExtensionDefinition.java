package com.bitwig.extensions.controllers.mcu.definitions.ssl;

import java.util.UUID;

public class SslUf8Plus1ExtenderExtensionDefinition extends SslUf8ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("31c41848-541d-4119-8553-0c5690ea10e8");
    
    public SslUf8Plus1ExtenderExtensionDefinition() {
        super(1, 0);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
