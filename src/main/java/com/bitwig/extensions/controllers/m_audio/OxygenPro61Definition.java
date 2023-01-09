package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

public class OxygenPro61Definition extends OxygenProDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("dedd215a-388d-4dda-902b-4e2f4f7bf7df");

    @Override
    String getModel() {
        return "61";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    public static OxygenPro61Definition getInstance()
    {
       return mInstance;
    }
 
    private static OxygenPro61Definition mInstance = new OxygenPro61Definition();
 
}
