package com.bitwig.extensions.controllers.expressivee.common;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBooleanValue;

public class SynthPreferences {

    private SettableBooleanValue mSoundEngineInAllInputs;

    public SynthPreferences(ControllerHost host) {
        mSoundEngineInAllInputs = host.getPreferences().getBooleanSetting("Include in all inputs", "Osmose Sound Engine", false);
    }

    public boolean soundEngineInAllInputs() {
        return mSoundEngineInAllInputs.get();
    }
}
