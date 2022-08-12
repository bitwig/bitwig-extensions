package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenPro25Definition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("133cd26c-1da1-44ac-8ccf-0e9624d908a6");

    @Override
    String getModel() {
        // TODO Auto-generated method stub
        return "25";
    }

    @Override
    public UUID getId() {
        // TODO Auto-generated method stub
        return DRIVER_ID;
    }

    
}
