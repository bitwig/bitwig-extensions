package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconV1mPlus1ExtenderExtensionDefinition extends IconV1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("db102763-745b-476a-a6ee-f099c3ad1d5e");
    
    public IconV1mPlus1ExtenderExtensionDefinition() {
        super(1);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
