package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenPro61Definition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("5a0c935f-bd37-4bba-8584-e368aa92444f");

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
