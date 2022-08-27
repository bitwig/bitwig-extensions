package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenProMiniDefinition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("0e04e475-66e3-41c2-90bf-e32373c087db");

    @Override
    String getModel() {
        return "Mini";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    
}
