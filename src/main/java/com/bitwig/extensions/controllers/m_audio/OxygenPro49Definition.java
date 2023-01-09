package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenPro49Definition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("f20b92f3-5c1a-4287-9257-eb723c9d0ab1");

    @Override
    String getModel() {
        return "49";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    
}
