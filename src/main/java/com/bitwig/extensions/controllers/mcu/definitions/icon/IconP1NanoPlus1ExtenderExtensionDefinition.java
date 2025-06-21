package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1NanoPlus1ExtenderExtensionDefinition extends IconP1NanoExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("3fb9dd44-f934-41e4-b5c2-83842d1cf526");
    
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
