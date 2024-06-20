package com.bitwig.extensions.controllers.mcu.definitions.ssl;

import java.util.UUID;

public class SslUf8Plus3ExtenderExtensionDefinition extends SslUf8ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("de4471bd-2eb7-4991-b74a-307b3d5d2338");
    
    public SslUf8Plus3ExtenderExtensionDefinition() {
        super(3, 0);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
