package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.OrientationFollowType;
import com.bitwig.extensions.controllers.akai.apc.common.PanelLayout;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.FocusMode;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class ApcPreferences {

    private final ValueObject<OrientationFollowType> orientationFollow;
    private final ValueObject<PanelLayout> panelLayout = new ValueObject<>(PanelLayout.VERTICAL);
    private final SettableBooleanValue altModeWithShift;
    private final SettableEnumValue recordButtonAssignment;
    private final SettableEnumValue gridLayoutSettings;
    private PanelLayout bitwigPanelLayout;
    private FocusMode recordFocusMode = FocusMode.LAUNCHER;

    public ApcPreferences(final ControllerHost host, final Application application) {
        final Preferences preferences = host.getPreferences(); // THIS
        orientationFollow = new ValueObject<>(OrientationFollowType.AUTOMATIC);
        gridLayoutSettings = preferences.getEnumSetting("Orientation determined by", "Grid Layout",
                new String[]{OrientationFollowType.AUTOMATIC.getLabel(), //
                        OrientationFollowType.FIXED_VERTICAL.getLabel(), //
                        OrientationFollowType.FIXED_HORIZONTAL.getLabel()}, //
                OrientationFollowType.FIXED_VERTICAL.getLabel());
        gridLayoutSettings.addValueObserver(newValue -> orientationFollow.set(OrientationFollowType.toType(newValue)));
        application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);
        altModeWithShift = preferences.getBooleanSetting("Use as ALT trigger modifier", "Shift Button", true);
        altModeWithShift.markInterested();
        orientationFollow.addValueObserver(((oldValue, newValue) -> {
            determinePanelLayout(orientationFollow.get());
        }));
        final DocumentState documentState = host.getDocumentState(); // THIS
        recordButtonAssignment = documentState.getEnumSetting("Record Button assignment", //
                "Transport", new String[]{FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
                recordFocusMode.getDescriptor());
        recordButtonAssignment.addValueObserver(value -> {
            recordFocusMode = FocusMode.toMode(value);
        });
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

    public SettableEnumValue getGridLayoutSettings() {
        return gridLayoutSettings;
    }

    public SettableBooleanValue getAltModeWithShift() {
        return altModeWithShift;
    }

    public boolean useShiftForAltMode() {
        return altModeWithShift.get();
    }

    public FocusMode getRecordFocusMode() {
        return recordFocusMode;
    }

    public ValueObject<PanelLayout> getPanelLayout() {
        return panelLayout;
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

}
