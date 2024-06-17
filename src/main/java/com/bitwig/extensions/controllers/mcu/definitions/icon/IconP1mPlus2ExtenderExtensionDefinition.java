package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1mPlus2ExtenderExtensionDefinition extends IconP1mExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("aa9bdf05-be3f-45f1-b9d2-4ec2016c2851");
    
    public IconP1mPlus2ExtenderExtensionDefinition() {
        this(2);
    }
    
    public IconP1mPlus2ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
