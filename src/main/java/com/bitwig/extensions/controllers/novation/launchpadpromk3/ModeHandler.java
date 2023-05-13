package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;

public interface ModeHandler {
    void toFaderMode(final ControlMode controlMode, final ControlMode previousMode);
}
