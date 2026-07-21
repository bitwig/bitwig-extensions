package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

public class SoftubeDaw4ExtensionDefinition extends SoftubeDawControlMultiExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("fe7022c4-9111-4a75-9f7d-d4a90928b4e6");
    
    public SoftubeDaw4ExtensionDefinition() {
        super(4);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
