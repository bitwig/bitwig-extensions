package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchpadDeviceConfig;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadmini3.layers.DrumLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class LaunchpadXControllerExtension extends AbstractLaunchpadMk3Extension {
    
    private static final String DEVICE_RESPONSE = "f07e000602002029";
    private static final String MODE_CHANGE_PREFIX = "f0002029020c00";
    private DrumLayer drumLayer;
    private ViewCursorControl viewControl;
    private Transport transport;
    private BooleanValue recordButtonAsAlt;
    private Layer altModeLayer;
    private LpMode preOverviewMode;
    
    protected LaunchpadXControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host, new LaunchpadDeviceConfig("LAUNCHPAD_X_MK3", 0xC, 0xB4, 0xB5, false));
    }
    
    @Override
    public void init() {
        final Context diContext = super.initContext();
        
        drumLayer = diContext.create(DrumLayer.class);
        final Layer mainLayer = diContext.createLayer("MainLayer");
        altModeLayer = diContext.createLayer("AltModeLayer");
        viewControl = diContext.getService(ViewCursorControl.class);
        transport = diContext.getService(Transport.class);
        initPreferences();
        initModeButtons(diContext, mainLayer);
        initViewControlListeners();
        mainLayer.activate();
        diContext.activate();
        altModeLayer.setIsActive(recordButtonAsAlt.get());
        midiProcessor.sendDeviceInquiry();
    }
    
    private void initPreferences() {
        final Preferences preferences = getHost().getPreferences(); // THIS
        recordButtonAsAlt = preferences.getBooleanSetting("Use as ALT trigger modifier", "Record Button", false);
        recordButtonAsAlt.markInterested();
    }
    
    private void initModeButtons(final Context diContext, final Layer layer) {
        final LpMiniHwElements hwElements = diContext.getService(LpMiniHwElements.class);
        final LabeledButton sessionButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.SESSION);
        //sessionButton.bindPressed(layer, this::toggleMode);
        sessionButton.bindDoubleTapCombo(
            layer, this::toggleMode, this::toOverviewMode, () -> this.mode != LpMode.OVERVIEW);
        // Double Press to Overview Mode
        
        final LabeledButton keysButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.DRUMS);
        keysButton.bindPressed(layer, () -> {
            if (mode == LpMode.KEYS) {
                return;
            }
            mode = LpMode.KEYS;
            changeDrumMode();
            if (recordButtonAsAlt.get()) {
                altModeLayer.setIsActive(false);
            }
        });
        
        final LabeledButton userButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.KEYS);
        userButton.bindPressed(layer, () -> mode = LpMode.CUSTOM);
        
        transport.isPlaying().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        
        recordButtonAsAlt.addValueObserver(recAsAlt -> {
            altModeLayer.setIsActive(recAsAlt);
            if (!recAsAlt) {
                sessionLayer.setShiftHeld(false);
            }
        });
        
        final LabeledButton recButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.USER);
        recButton.bindPressed(layer, () -> viewControl.globalRecordAction(transport));
        recButton.bindLight(layer, sessionLayer::getRecordButtonColorRegular);
        
        recButton.bindPressed(altModeLayer, pressed -> sessionLayer.setShiftHeld(pressed), RgbState.WHITE,
            RgbState.pulse(2));
        
        final MultiStateHardwareLight novationLight = hwElements.getNovationLight();
        layer.bindLightState(() -> RgbState.DIM_WHITE, novationLight);
        getHost().scheduleTask(recButton::refresh, 50);
        
    }
    
    private void initViewControlListeners() {
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
            drumModeActive = hasDrumPads;
            if (mode == LpMode.KEYS) {
                changeDrumMode();
            }
        });
    }
    
    private boolean drumModeActive = false;
    
    private void changeDrumMode() {
        midiProcessor.setNoteModeLayoutX(drumModeActive ? 0x1 : 0x0);
        drumLayer.setIsActive(drumModeActive);
    }
    
    private void toOverviewMode() {
        if (mode != LpMode.OVERVIEW) {
            this.preOverviewMode = this.mode;//  == LpMode.MIXER ? LpMode.SESSION : LpMode.MIXER;
            changeMode(LpMode.OVERVIEW);
            updateSessionButtonColor();
        }
    }
    
    private void toggleMode() {
        if (mode == LpMode.OVERVIEW) {
            changeMode(this.preOverviewMode);
        } else if (mode == LpMode.SESSION) {
            changeMode(LpMode.MIXER);
        } else {
            changeMode(LpMode.SESSION);
        }
        updateSessionButtonColor();
    }
    
    private void updateSessionButtonColor() {
        midiProcessor.setButtonLed(0x14, this.mode.getButtonColor());
    }
    
    private void changeMode(final LpMode mode) {
        if (this.mode == mode) {
            return;
        }
        if (recordButtonAsAlt.get()) {
            altModeLayer.setIsActive(true);
        }
        if (this.mode == LpMode.OVERVIEW) {
            overviewLayer.setIsActive(false);
        }
        if (mode == LpMode.OVERVIEW) {
            if (this.mode == LpMode.MIXER) {
                sessionLayer.setMode(LpMode.SESSION);
            }
            sessionLayer.setIsActive(false);
            overviewLayer.setIsActive(true);
        } else {
            sessionLayer.setIsActive(true);
            sessionLayer.setMode(mode);
        }
        this.mode = mode;
    }
    
    @Override
    public void handleSysEx(final String sysExString) {
        if (sysExString.startsWith(DEVICE_RESPONSE)) {
            midiProcessor.enableDawMode(true);
            midiProcessor.toLayout(0);
            hwElements.refresh();
        } else if (sysExString.startsWith(MODE_CHANGE_PREFIX)) {
            final String modeString = sysExString.substring(MODE_CHANGE_PREFIX.length(), sysExString.length() - 2);
        }
    }
    
}
