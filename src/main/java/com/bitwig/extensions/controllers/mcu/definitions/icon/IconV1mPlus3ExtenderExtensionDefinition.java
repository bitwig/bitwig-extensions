package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconV1mPlus3ExtenderExtensionDefinition extends IconV1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("defe335e-5828-4c9c-a2f7-8824308c9d7a");
    
    public IconV1mPlus3ExtenderExtensionDefinition() {
        super(3);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
