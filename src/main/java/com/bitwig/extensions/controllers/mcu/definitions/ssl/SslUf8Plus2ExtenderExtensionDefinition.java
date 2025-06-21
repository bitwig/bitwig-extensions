package com.bitwig.extensions.controllers.mcu.definitions.ssl;

import java.util.UUID;

public class SslUf8Plus2ExtenderExtensionDefinition extends SslUf8ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("984c4516-1b71-48ee-9ea3-f004662a904c");
    
    public SslUf8Plus2ExtenderExtensionDefinition() {
        super(2, 0);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
