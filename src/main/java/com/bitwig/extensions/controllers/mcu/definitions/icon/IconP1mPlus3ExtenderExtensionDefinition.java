package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1mPlus3ExtenderExtensionDefinition extends IconP1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8b027591-fc6b-4db1-9c23-4b8e05b755db");
    
    public IconP1mPlus3ExtenderExtensionDefinition() {
        this(3);
    }
    
    public IconP1mPlus3ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
