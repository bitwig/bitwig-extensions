package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.di.Component;

@Component
public class LppPreferences {

    private final SettableBooleanValue altModeWithShift;

    public LppPreferences(final ControllerHost host) {
        final Preferences preferences = host.getPreferences(); // THIS
        altModeWithShift = preferences.getBooleanSetting("Use as ALT trigger modifier", "Shift Button", true);
        altModeWithShift.markInterested();
    }

    public SettableBooleanValue getAltModeWithShift() {
        return altModeWithShift;
    }
}
