package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class Iconp1NanoPlus3ExtenderExtensionDefinition extends IconP1NanoExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8b027591-fc1a-2dbd-9c71-4b8e05b755db");
    
    public Iconp1NanoPlus3ExtenderExtensionDefinition() {
        this(3);
    }
    
    public Iconp1NanoPlus3ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
