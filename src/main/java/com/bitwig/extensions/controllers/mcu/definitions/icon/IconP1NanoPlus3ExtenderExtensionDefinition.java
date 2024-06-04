package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1NanoPlus3ExtenderExtensionDefinition extends IconP1NanoExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("a0b39436-f6b6-4662-b535-85de86626ad4");
    
    public IconP1NanoPlus3ExtenderExtensionDefinition() {
        this(3);
    }
    
    public IconP1NanoPlus3ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
