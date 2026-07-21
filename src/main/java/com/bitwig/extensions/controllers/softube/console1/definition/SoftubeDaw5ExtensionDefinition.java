package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

public class SoftubeDaw5ExtensionDefinition extends SoftubeDawControlMultiExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("05c2d532-17f9-47a2-b89b-25c10ccc420e");
    
    public SoftubeDaw5ExtensionDefinition() {
        super(5);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
