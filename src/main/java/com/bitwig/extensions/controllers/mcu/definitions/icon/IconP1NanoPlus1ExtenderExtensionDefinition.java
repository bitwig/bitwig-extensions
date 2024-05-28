package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1NanoPlus1ExtenderExtensionDefinition extends IconP1NanoExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8c027591-fc6b-2dbd-9c23-4b8e05b755db");
    
    public IconP1NanoPlus1ExtenderExtensionDefinition() {
        this(1);
    }
    
    public IconP1NanoPlus1ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
