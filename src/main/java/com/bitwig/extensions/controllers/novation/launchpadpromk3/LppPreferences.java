package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.controllers.novation.commonsmk3.OrientationFollowType;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class LppPreferences {
    
    private final SettableBooleanValue altModeWithShift;
    private final ValueObject<OrientationFollowType> orientationFollow;
    private final ValueObject<PanelLayout> panelLayout = new ValueObject<>(PanelLayout.VERTICAL);
    private PanelLayout bitwigPanelLayout = PanelLayout.VERTICAL;
    
    public LppPreferences(final ControllerHost host, final Application application) {
        final Preferences preferences = host.getPreferences(); // THIS
        altModeWithShift = preferences.getBooleanSetting("Use as ALT trigger modifier", "Shift Button", true);
        altModeWithShift.markInterested();
        orientationFollow = new ValueObject<>(OrientationFollowType.AUTOMATIC);
        final SettableEnumValue gridLayout =
            preferences.getEnumSetting("Launchpad orientation determined by", "Grid Layout", //
                new String[] {
                    OrientationFollowType.AUTOMATIC.getLabel(), //
                    OrientationFollowType.FIXED_VERTICAL.getLabel(), //
                    OrientationFollowType.FIXED_HORIZONTAL.getLabel()
                }, //
                OrientationFollowType.FIXED_VERTICAL.getLabel());
        gridLayout.addValueObserver(newValue -> orientationFollow.set(OrientationFollowType.toType(newValue)));
        application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);
        orientationFollow.addValueObserver((newValue -> {
            determinePanelLayout(orientationFollow.get());
        }));
    }
    
    private void handlePanelLayoutChanged(final String layout) {
        if (layout.equals("MIX")) {
            bitwigPanelLayout = PanelLayout.VERTICAL;
        } else if (layout.equals("ARRANGE")) {
            bitwigPanelLayout = PanelLayout.HORIZONTAL;
        } else {
            bitwigPanelLayout = PanelLayout.VERTICAL;
        }
        determinePanelLayout(orientationFollow.get());
    }
    
    private void determinePanelLayout(final OrientationFollowType followType) {
        if (followType == OrientationFollowType.FIXED_VERTICAL) {
            panelLayout.set(PanelLayout.VERTICAL);
        } else if (followType == OrientationFollowType.FIXED_HORIZONTAL) {
            panelLayout.set(PanelLayout.HORIZONTAL);
        } else {
            panelLayout.set(bitwigPanelLayout);
        }
    }
    
    public SettableBooleanValue getAltModeWithShift() {
        return altModeWithShift;
    }
    
    public ValueObject<PanelLayout> getPanelLayout() {
        return panelLayout;
    }
}
