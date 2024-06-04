package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconV1mPlus2ExtenderExtensionDefinition extends IconV1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ffc8800a-2801-45e0-80cb-928684d83cc9");
    
    public IconV1mPlus2ExtenderExtensionDefinition() {
        super(2);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
