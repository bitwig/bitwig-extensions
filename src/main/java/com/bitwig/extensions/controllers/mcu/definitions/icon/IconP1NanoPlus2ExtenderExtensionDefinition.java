package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

public class IconP1NanoPlus2ExtenderExtensionDefinition extends IconP1NanoExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("6208f5aa-017c-4d60-8635-1d1c4a04478a");
    
    public IconP1NanoPlus2ExtenderExtensionDefinition() {
        this(2);
    }
    
    public IconP1NanoPlus2ExtenderExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
