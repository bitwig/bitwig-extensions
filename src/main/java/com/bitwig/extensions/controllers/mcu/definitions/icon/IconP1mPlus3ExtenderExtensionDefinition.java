package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1mPlus3ExtenderExtensionDefinition extends IconP1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("358db9a4-b7e7-43f4-8a3d-76590c3a28e3");
    
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
