package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

public class SoftubeDaw6ExtensionDefinition extends SoftubeDawControlMultiExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("ed55d507-1a02-40ba-8fcc-e42994aaa4ea");
    
    public SoftubeDaw6ExtensionDefinition() {
        super(6);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
}
