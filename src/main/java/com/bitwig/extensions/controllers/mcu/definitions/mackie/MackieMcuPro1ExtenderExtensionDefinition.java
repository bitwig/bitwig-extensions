package com.bitwig.extensions.controllers.mcu.definitions.mackie;

import java.util.UUID;

public class MackieMcuPro1ExtenderExtensionDefinition extends MackieMcuProExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("42133b66-3870-4e4c-a833-f3b1dd7389f4");
    
    public MackieMcuPro1ExtenderExtensionDefinition() {
        super(1);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
