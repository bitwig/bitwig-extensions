package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class Iconp1NanoPlus1ExtenderExtensionDefinition extends IconP1NanoExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8b027591-fc6b-2dbd-9c23-4b8e05b755db");
    
    public Iconp1NanoPlus1ExtenderExtensionDefinition() {
        this(1);
    }
    
    public Iconp1NanoPlus1ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
