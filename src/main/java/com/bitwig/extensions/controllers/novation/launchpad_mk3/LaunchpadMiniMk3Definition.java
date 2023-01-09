package com.bitwig.extensions.controllers.novation.launchpad_mk3;

import java.util.UUID;

public class LaunchpadMiniMk3Definition extends LaunchpadMk3Definition {

    private final static UUID ID = UUID.fromString("af4538c0-4066-411a-bd2f-0a1ac640cb38");

    @Override
    public String getModelName(){
        return LAUNCHPAD_MINI_MODEL;
    };

    @Override
    public UUID getId() {
        return ID;
    }

    
}
