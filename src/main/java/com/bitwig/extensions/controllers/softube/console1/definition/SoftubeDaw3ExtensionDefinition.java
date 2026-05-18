package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public class SoftubeDaw3ExtensionDefinition extends SoftubeDawControlMultiExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("98630b94-ed23-4a11-9981-05d3995b49AB");
    
    public SoftubeDaw3ExtensionDefinition() {
        super(3);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
}
