package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenPro49Definition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("6f50935f-bd37-4bba-8584-e368aa92444f");

    @Override
    String getModel() {
        // TODO Auto-generated method stub
        return "49";
    }

    @Override
    public UUID getId() {
        // TODO Auto-generated method stub
        return DRIVER_ID;
    }

    
}
