package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenPro61Definition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("dedd215a-388d-4dda-902b-4e2f4f7bf7df");

    @Override
    String getModel() {
        // TODO Auto-generated method stub
        return "61";
    }

    @Override
    public UUID getId() {
        // TODO Auto-generated method stub
        return DRIVER_ID;
    }

    
}
