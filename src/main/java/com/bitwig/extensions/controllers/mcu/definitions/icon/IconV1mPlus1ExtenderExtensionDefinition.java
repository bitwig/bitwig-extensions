package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconV1mPlus1ExtenderExtensionDefinition extends IconV1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("aaa99215-1162-4159-afc3-970342d6dd9d");
    
    public IconV1mPlus1ExtenderExtensionDefinition() {
        super(1);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
