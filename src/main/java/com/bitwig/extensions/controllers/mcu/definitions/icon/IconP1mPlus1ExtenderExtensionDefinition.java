package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1mPlus1ExtenderExtensionDefinition extends IconP1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("f7166b22-b3de-4c8f-8173-06e0995f102e");
    
    public IconP1mPlus1ExtenderExtensionDefinition() {
        this(1);
    }
    
    public IconP1mPlus1ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
