package com.bitwig.extensions.controllers.akai.apc64.layer;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.apc.common.PanelLayout;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.controllers.akai.apc64.Apc64CcAssignments;
import com.bitwig.extensions.controllers.akai.apc64.Apc64MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc64.ApcPreferences;
import com.bitwig.extensions.controllers.akai.apc64.DeviceControl;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.ModifierStates;
import com.bitwig.extensions.controllers.akai.apc64.PadMode;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class NavigationLayer {
    
    @Inject
    private PadLayer padLayer;
    
    private final Layer sessionNavigationVertical;
    private final Layer sessionNavigationHorizontal;
    private final Layer padNavigation;
    private final Layer deviceNavLayer;
    private final Layer sendsNavLayer;
    private final ViewControl viewControl;
    private final ModifierStates modifierState;
    private final TrackBank trackBank;
    private final ValueObject<PanelLayout> panelLayout;
    private PadMode currentMode = PadMode.SESSION;
    
    public NavigationLayer(final Layers layers, final HardwareElements hwElement, final ViewControl viewControl,
        final ModifierStates modifierStates, final ApcPreferences preferences, final Apc64MidiProcessor midiProcessor) {
        sessionNavigationVertical = new Layer(layers, "SESSION_NAVIGATION_VERTICAL");
        sessionNavigationHorizontal = new Layer(layers, "SESSION_NAVIGATION_HORIZONTAL");
        padNavigation = new Layer(layers, "PAD_LAYER_NAVIGATION");
        this.deviceNavLayer = new Layer(layers, "DEVICE_NAVIGATION");
        this.sendsNavLayer = new Layer(layers, "SENDS_NAVIGATION");
        this.viewControl = viewControl;
        this.modifierState = modifierStates;
        this.trackBank = viewControl.getTrackBank();
        this.panelLayout = preferences.getPanelLayout();
        this.panelLayout.addValueObserver(newValue -> {
            this.sessionNavigationVertical.setIsActive(newValue == PanelLayout.VERTICAL);
            this.sessionNavigationHorizontal.setIsActive(newValue == PanelLayout.HORIZONTAL);
        });
        midiProcessor.addModeChangeListener(this::handleModeChange);
        
        initSessionNavigation(sessionNavigationVertical, hwElement, Apc64CcAssignments.NAV_DOWN,
            Apc64CcAssignments.NAV_UP, Apc64CcAssignments.NAV_LEFT, Apc64CcAssignments.NAV_RIGHT);
        initSessionNavigation(sessionNavigationHorizontal, hwElement, Apc64CcAssignments.NAV_LEFT,
            Apc64CcAssignments.NAV_RIGHT, Apc64CcAssignments.NAV_DOWN, Apc64CcAssignments.NAV_UP);
        initPadLayerNavigation(padNavigation, hwElement);
        initDeviceNavigation(deviceNavLayer, hwElement);
        initSendsNavigation(sendsNavLayer, hwElement);
    }
    
    private void handleModeChange(final PadMode mode) {
        this.currentMode = mode;
        activateSessionNavigation(true);
    }
    
    @Activate
    public void activateLayer() {
        this.sessionNavigationVertical.setIsActive(panelLayout.get() == PanelLayout.VERTICAL);
        this.sessionNavigationHorizontal.setIsActive(panelLayout.get() == PanelLayout.HORIZONTAL);
    }
    
    private void initSessionNavigation(final Layer layer, final HardwareElements hwElements,
        final Apc64CcAssignments downButton, final Apc64CcAssignments upButton, final Apc64CcAssignments leftButton,
        final Apc64CcAssignments rightButton) {
        final SingleLedButton navDown = hwElements.getButton(downButton);
        navDown.bindRepeatHold(layer, () -> handleSessionVertical(-1));
        navDown.bindLightPressed(layer, pressed -> canNavigateVertical(pressed, -1));
        
        final SingleLedButton navUp = hwElements.getButton(upButton);
        navUp.bindRepeatHold(layer, () -> handleSessionVertical(1));
        navUp.bindLightPressed(layer, pressed -> canNavigateVertical(pressed, 1));
        
        final SingleLedButton navLeft = hwElements.getButton(leftButton);
        navLeft.bindRepeatHold(layer, () -> handleSessionHorizontal(-1));
        navLeft.bindLightPressed(layer, pressed -> canNavigateHorizontal(pressed, -1));
        
        final SingleLedButton navRight = hwElements.getButton(rightButton);
        navRight.bindRepeatHold(layer, () -> handleSessionHorizontal(1));
        navRight.bindLightPressed(layer, pressed -> canNavigateHorizontal(pressed, 1));
    }
    
    private void initPadLayerNavigation(final Layer layer, final HardwareElements hwElements) {
        final SingleLedButton navDown = hwElements.getButton(Apc64CcAssignments.NAV_DOWN);
        navDown.bindRepeatHold(layer, () -> handlePadModeNavigation(-1));
        navDown.bindLightPressed(layer, pressed -> canNavigatePadMode(pressed, -1));
        
        final SingleLedButton navUp = hwElements.getButton(Apc64CcAssignments.NAV_UP);
        navUp.bindRepeatHold(layer, () -> handlePadModeNavigation(1));
        navUp.bindLightPressed(layer, pressed -> canNavigatePadMode(pressed, 1));
        
        final SingleLedButton navLeft = hwElements.getButton(Apc64CcAssignments.NAV_LEFT);
        navLeft.bindIsPressed(layer, pressed -> {
        });
        navLeft.bindLight(layer, () -> VarSingleLedState.OFF);
        
        final SingleLedButton navRight = hwElements.getButton(Apc64CcAssignments.NAV_RIGHT);
        navRight.bindIsPressed(layer, pressed -> {
        });
        navRight.bindLight(layer, () -> VarSingleLedState.OFF);
    }
    
    private void handlePadModeNavigation(final int dir) {
        final int amount = modifierState.isShift() ? dir * 16 : dir * 4;
        padLayer.navigateBy(amount);
    }
    
    public VarSingleLedState canNavigatePadMode(final boolean pressedState, final int dir) {
        final int amount = modifierState.isShift() ? dir * 8 : dir * 4;
        if (padLayer.canNavigateBy(amount)) {
            return pressedState ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_25;
        }
        return VarSingleLedState.OFF;
    }
    
    public void handleSessionVertical(final int dir) {
        final int amount = modifierState.isShift() ? dir * 8 : dir;
        trackBank.sceneBank().scrollBy(amount);
    }
    
    public void handleSessionHorizontal(final int dir) {
        final int amount = modifierState.isShift() ? dir * 8 : dir;
        trackBank.scrollBy(amount);
    }
    
    public VarSingleLedState canNavigateVertical(final boolean pressedState, final int dir) {
        final int amount = modifierState.isShift() ? dir * 8 : dir;
        if (viewControl.canScrollVertical(amount)) {
            return pressedState ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_25;
        }
        return VarSingleLedState.OFF;
    }
    
    public VarSingleLedState canNavigateHorizontal(final boolean pressedState, final int dir) {
        final int amount = modifierState.isShift() ? dir * 8 : dir;
        if (viewControl.canScrollHorizontal(amount)) {
            return pressedState ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_25;
        }
        return VarSingleLedState.OFF;
    }
    
    private void initDeviceNavigation(final Layer layer, final HardwareElements hwElements) {
        final DeviceControl deviceControl = viewControl.getDeviceControl();
        final SingleLedButton leftNav = hwElements.getButton(Apc64CcAssignments.NAV_LEFT);
        final SingleLedButton rightNav = hwElements.getButton(Apc64CcAssignments.NAV_RIGHT);
        final SingleLedButton upNav = hwElements.getButton(Apc64CcAssignments.NAV_UP);
        final SingleLedButton downNav = hwElements.getButton(Apc64CcAssignments.NAV_DOWN);
        
        rightNav.bindPressed(layer, () -> deviceControl.selectDevice(1));
        rightNav.bindLightPressed(layer, pressed -> canNavigate(pressed, () -> deviceControl.canScrollDevices(1)));
        leftNav.bindPressed(layer, () -> deviceControl.selectDevice(-1));
        rightNav.bindLightPressed(layer, pressed -> canNavigate(pressed, () -> deviceControl.canScrollDevices(-1)));
        
        upNav.bindPressed(layer, () -> navigateDeviceVertical(deviceControl, 1));
        upNav.bindLightPressed(layer, pressed -> canNavigateVertical(pressed, deviceControl, 1));
        downNav.bindPressed(layer, () -> navigateDeviceVertical(deviceControl, -1));
        downNav.bindLightPressed(layer, pressed -> canNavigateVertical(pressed, deviceControl, -1));
    }
    
    private void navigateDeviceVertical(final DeviceControl deviceControl, final int dir) {
        if (modifierState.isShift()) {
            deviceControl.navigateVertical(dir);
        } else {
            deviceControl.selectParameterPage(dir);
        }
    }
    
    private VarSingleLedState canNavigateVertical(final boolean pressed, final DeviceControl deviceControl,
        final int dir) {
        if (modifierState.isShift()) {
            if (deviceControl.canNavigateIntoDevice(dir)) {
                return pressed ? VarSingleLedState.FULL : VarSingleLedState.PULSE_2;
            }
        } else {
            if (deviceControl.canScrollParameterPages(dir)) {
                return pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_25;
            }
        }
        return VarSingleLedState.OFF;
    }
    
    
    private void initSendsNavigation(final Layer layer, final HardwareElements hwElements) {
        final SingleLedButton leftNav = hwElements.getButton(Apc64CcAssignments.NAV_LEFT);
        final SingleLedButton rightNav = hwElements.getButton(Apc64CcAssignments.NAV_RIGHT);
        final SingleLedButton upNav = hwElements.getButton(Apc64CcAssignments.NAV_UP);
        final SingleLedButton downNav = hwElements.getButton(Apc64CcAssignments.NAV_DOWN);
        for (int i = 0; i < viewControl.getTrackBank().getSizeOfBank(); i++) {
            final SendBank sendsBank = viewControl.getTrackBank().getItemAt(i).sendBank();
            sendsBank.canScrollBackwards().markInterested();
            sendsBank.canScrollForwards().markInterested();
            sendsBank.scrollPosition().markInterested();
        }
        final SendBank sendsBank = viewControl.getTrackBank().getItemAt(0).sendBank();
        rightNav.bindPressed(layer, this::scrollSendsBackward);
        rightNav.bindLightPressed(layer, pressed -> canNavigate(pressed, sendsBank.canScrollBackwards()));
        leftNav.bindPressed(layer, this::scrollSendsForward);
        leftNav.bindLightPressed(layer, pressed -> canNavigate(pressed, sendsBank.canScrollForwards()));
        
        downNav.bindPressed(layer, () -> {
        });
        downNav.bindLightPressed(layer, pressed -> VarSingleLedState.OFF);
        upNav.bindPressed(layer, () -> {
        });
        upNav.bindLightPressed(layer, pressed -> VarSingleLedState.OFF);
    }
    
    private void scrollSendsForward() {
        final TrackBank bank = viewControl.getTrackBank();
        for (int i = 0; i < bank.getSizeOfBank(); i++) {
            bank.getItemAt(i).sendBank().scrollForwards();
        }
    }
    
    private void scrollSendsBackward() {
        final TrackBank bank = viewControl.getTrackBank();
        for (int i = 0; i < bank.getSizeOfBank(); i++) {
            bank.getItemAt(i).sendBank().scrollBackwards();
        }
    }
    
    
    public void navigateSends() {
        final TrackBank bank = viewControl.getTrackBank();
        
        for (int i = 0; i < bank.getSizeOfBank(); i++) {
            scrollRoundRobin(bank.getItemAt(i).sendBank());
        }
        
    }
    
    private void scrollRoundRobin(final SendBank sendBank) {
        if (sendBank.canScrollForwards().get()) {
            sendBank.scrollForwards();
        } else {
            sendBank.scrollPosition().set(0);
        }
    }
    
    
    private VarSingleLedState canNavigate(final boolean pressed, final BooleanValue value) {
        if (value.get()) {
            return pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_25;
        }
        return VarSingleLedState.OFF;
    }
    
    private VarSingleLedState canNavigate(final boolean pressed, final BooleanSupplier value) {
        if (value.getAsBoolean()) {
            return pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_25;
        }
        return VarSingleLedState.OFF;
    }
    
    public void setDeviceNavigationActive(final boolean active) {
        sendsNavLayer.setIsActive(false);
        deviceNavLayer.setIsActive(active);
        activateSessionNavigation(!active);
    }
    
    public void setSendsNavigationActive(final boolean active) {
        deviceNavLayer.setIsActive(false);
        sendsNavLayer.setIsActive(active);
        activateSessionNavigation(!active);
    }
    
    public void activateSessionNavigation(final boolean active) {
        if (active) {
            if (currentMode == PadMode.SESSION || currentMode == PadMode.OVERVIEW) {
                this.sessionNavigationVertical.setIsActive(panelLayout.get() == PanelLayout.VERTICAL);
                this.sessionNavigationHorizontal.setIsActive(panelLayout.get() == PanelLayout.HORIZONTAL);
                this.padNavigation.setIsActive(false);
            } else if (currentMode == PadMode.DRUM) {
                sessionNavigationVertical.setIsActive(false);
                sessionNavigationHorizontal.setIsActive(false);
                this.padNavigation.setIsActive(true);
            }
        } else {
            sessionNavigationVertical.setIsActive(false);
            sessionNavigationHorizontal.setIsActive(false);
            padNavigation.setIsActive(false);
        }
    }
    
}
