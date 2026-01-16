package com.bitwig.extensions.controllers.expressivee.common;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBooleanValue;

public class ExternalMidiPreferences {

    private SettableBooleanValue mplayInAllInputs;

    public ExternalMidiPreferences(ControllerHost host) {
        mplayInAllInputs = host.getPreferences().getBooleanSetting("Include in all inputs", "Osmose Play", true);
    }

    public boolean playInAllInputs() {
        return mplayInAllInputs.get();
    }
}
