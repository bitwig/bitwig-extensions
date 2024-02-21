package com.bitwig.extensions.controllers.mcu.definitions.mackie;

import java.util.UUID;

public class MackieMcuPro1ExtenderExtensionDefinition extends MackieMcuProExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-f1cc-81ad-1de77ffa2dab");
    
    public MackieMcuPro1ExtenderExtensionDefinition() {
        super(1);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
