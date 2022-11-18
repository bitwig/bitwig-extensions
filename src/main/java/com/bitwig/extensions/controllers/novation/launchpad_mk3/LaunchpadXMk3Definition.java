package com.bitwig.extensions.controllers.novation.launchpad_mk3;

import java.util.UUID;

public class LaunchpadXMk3Definition extends LaunchpadMk3Definition {

    private final static UUID ID = UUID.fromString("2eb332c3-9595-4bf8-bb7f-3f9f463ca5db");

    @Override
    public String getModelName(){
        return LAUNCHPAD_X_MODEL;
    };

    @Override
    public UUID getId() {
        return ID;
    }

    
}
