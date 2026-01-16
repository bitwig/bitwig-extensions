package com.bitwig.extensions.controllers.expressivee.common;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBooleanValue;

public class SynthPreferences {

    private SettableBooleanValue mhakenInAllInputs;

    public SynthPreferences(ControllerHost host) {
        mhakenInAllInputs = host.getPreferences().getBooleanSetting("Include in all inputs", "Osmose Haken", false);
    }

    public boolean hakenInAllInputs() {
        return mhakenInAllInputs.get();
    }
}
