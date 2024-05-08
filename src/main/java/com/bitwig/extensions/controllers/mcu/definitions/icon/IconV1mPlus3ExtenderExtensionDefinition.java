package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconV1mPlus3ExtenderExtensionDefinition extends IconV1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ada99216-3362-4159-afc3-970342d6dd9d");
    
    public IconV1mPlus3ExtenderExtensionDefinition() {
        super(3);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
