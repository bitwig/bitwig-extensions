package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ControlElements;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.control.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.DeviceMidiListener;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class DeviceControl implements DeviceMidiListener {
    
    public static final String ONLY_DEVICES = "only Devices";
    public static final String WITH_TRACK_PROJECT = "with Track/Project";
    private final MidiProcessor midiProcessor;
    private final BankControl mainBank;
    private final ControllerHost host;
    private Runnable bankUpdateTask = null;
    private final Layer navigationLayer;
    
    private final ControlElements controlElements;
    
    private boolean directActive;
    private final DirectParameterControl directParameterControl;
    private final RemotesControl deviceRemotesControl;
    private final RemotesControl projectRemotesControl;
    private final RemotesControl trackRemotesControl;
    private AbstractParameterControl currentRemotesControl;
    private final BooleanValueObject useRemotes = new BooleanValueObject();
    private BankControl.Focus deviceFocus = BankControl.Focus.DEVICE;
    
    public DeviceControl(final ControllerHost host, final MidiProcessor midiProcessor, final ViewControl viewControl,
        final Layers layers, final ControlElements controlElements) {
        initSetting(host);
        this.midiProcessor = midiProcessor;
        this.midiProcessor.addDeviceMidiListener(this);
        this.midiProcessor.addModeListener(this::changeMode);
        navigationLayer = new Layer(layers, "PARAM_NAVIGATION_LAYER");
        final Layer deviceRemoteLayer = new Layer(layers, "DEVICE");
        final Layer trackRemoteLayer = new Layer(layers, "TRACK");
        final Layer projectRemoteLayer = new Layer(layers, "PROJECT");
        final Layer directParamLayer = new Layer(layers, "DIRECT_PARAM");
        final Layer browserNavLayer = new Layer(layers, "BROWSER_NAV");
        final CursorTrack cursorTrack = viewControl.getClipSceneCursor().getCursorTrack();
        this.host = host;
        this.controlElements = controlElements;
        final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
        cursorDevice.presetName().addValueObserver(this::handlePresetName);
        this.mainBank = new BankControl(cursorDevice, this.midiProcessor, this);
        this.mainBank.getCurrentFocus().addValueObserver(this::handleFocus);
        final BrowserHandler browserHandler = new BrowserHandler(host, cursorDevice, controlElements.getShiftHeld());
        
        final CursorRemoteControlsPage deviceRemotePages = cursorDevice.createCursorRemoteControlsPage(8);
        deviceRemotesControl = new RemotesControl(deviceRemoteLayer, deviceRemotePages, controlElements, midiProcessor);
        directParameterControl =
            new DirectParameterControl(
                directParamLayer, cursorDevice, controlElements, midiProcessor,
                deviceRemotePages.pageCount());
        directParameterControl.getDirectActive().addValueObserver(this::handleDirectActive);
        
        final Track rootTrack = viewControl.getProject().getRootTrackGroup();
        //final CursorRemoteControlsPage projectRemotes = rootTrack.createCursorRemoteControlsPage(8);
        final CursorRemoteControlsPage projectRemotes =
            rootTrack.createCursorRemoteControlsPage("FIXED_PROJECT", 8, null);
        projectRemotesControl = new RemotesControl(projectRemoteLayer, projectRemotes, controlElements, midiProcessor);
        
        //final CursorRemoteControlsPage trackRemotes = viewControl.getCursorTrack().createCursorRemoteControlsPage(8);
        final CursorRemoteControlsPage trackRemotes =
            viewControl.getCursorTrack().createCursorRemoteControlsPage("FIXED_TRACK", 8, null);
        trackRemotesControl = new RemotesControl(trackRemoteLayer, trackRemotes, controlElements, midiProcessor);
        
        currentRemotesControl = deviceRemotesControl;
        cursorDevice.isPlugin().addValueObserver(deviceRemotesControl::setOnPlugin);
        
        controlElements.getShiftHeld().addValueObserver(fineTune -> currentRemotesControl.setFineTune(fineTune));
        
        
        controlElements.getTrackNavLeftButton()
            .bind(navigationLayer, this::navigateLeft, currentRemotesControl::canScrollLeft);
        controlElements.getTrackRightNavButton()
            .bind(navigationLayer, this::navigateRight, currentRemotesControl::canScrollRight);
        
        controlElements.getPreviousPresetButton()
            .bind(navigationLayer, browserHandler::navigatePrevious, browserHandler::canNavigatePrevious);
        controlElements.getNextPresetButtonButton()
            .bind(navigationLayer, browserHandler::navigateNext, browserHandler::canNavigateNext);
        
        
        useRemotes.addValueObserver(mainBank::setUsesTrackRemotes);
        
        trackRemotes.pageCount().addValueObserver(this::handleTrackPages);
        projectRemotes.pageCount().addValueObserver(this::handleProjectPages);
        
        browserHandler.isOpen().addValueObserver(browserNavLayer::setIsActive);
        final ModeButton knobPressed = controlElements.getKnobPressed();
        final ModeButton knobShiftPressed = controlElements.getKnobShiftPressed();
        final RelativeHardwareKnob fourDKnob = controlElements.getFourDKnob();
        browserNavLayer.bindPressed(knobPressed.getHwButton(), browserHandler::confirm);
        browserNavLayer.bindPressed(knobShiftPressed.getHwButton(), browserHandler::cancel);
        final RelativeHardwarControlBindable binding = midiProcessor.createIncAction(
            new ConditionalIntDecelerator(browserHandler::incrementSelection, 10, controlElements.getShiftHeld(), 10));
        browserNavLayer.bind(fourDKnob, binding);
        
        controlElements.getLeftNavButton()
            .bind(browserNavLayer, this::handleBrowserLeftNavigation, this::browserNavigationState);
        controlElements.getRightNavButton()
            .bind(browserNavLayer, this::handleBrowserRightNavigation, this::browserNavigationState);
        controlElements.getUpNavButton()
            .bind(browserNavLayer, this::handleBrowserUpNavigation, this::browserNavigationState);
        controlElements.getDownNavButton()
            .bind(browserNavLayer, this::handleBrowserDownNavigation, this::browserNavigationState);
    }
    
    private void handleBrowserLeftNavigation() {
    }
    
    private void handleBrowserRightNavigation() {
    }
    
    private void handleBrowserUpNavigation() {
    }
    
    private void handleBrowserDownNavigation() {
    }
    
    private boolean browserNavigationState() {
        return false;
    }
    
    private void handleTrackPages(final int count) {
        mainBank.setTrackRemotesPresent(count > 0);
    }
    
    private void handleProjectPages(final int count) {
        mainBank.setProjectRemotesPresent(count > 0);
    }
    
    private void navigateLeft() {
        currentRemotesControl.navigateLeft();
    }
    
    private void navigateRight() {
        currentRemotesControl.navigateRight();
    }
    
    private void handleDirectActive(final boolean isDirectActive) {
        this.directActive = isDirectActive;
        handleFocus(this.deviceFocus);
    }
    
    private void handleFocus(final BankControl.Focus focus) {
        this.deviceFocus = focus;
        this.currentRemotesControl.setActive(false);
        switch (this.deviceFocus) {
            case DEVICE -> currentRemotesControl = directActive ? directParameterControl : deviceRemotesControl;
            case TRACK -> currentRemotesControl = trackRemotesControl;
            case PROJECT -> currentRemotesControl = projectRemotesControl;
        }
        currentRemotesControl.setActive(true);
    }
    
    private void initSetting(final ControllerHost host) {
        final SettableEnumValue useTrackRemotes = host.getDocumentState().getEnumSetting(
            "Remotes", //
            "Visible", new String[] {ONLY_DEVICES, WITH_TRACK_PROJECT}, ONLY_DEVICES);
        useTrackRemotes.addValueObserver(value -> this.useRemotes.set(value.equals(WITH_TRACK_PROJECT)));
        this.useRemotes.set(useTrackRemotes.get().equals(WITH_TRACK_PROJECT));
    }
    
    private void handlePresetName(final String presetName) {
        if (currentRemotesControl.isActive()) {
            midiProcessor.sendPresetName(presetName);
        }
    }
    
    private void changeMode(final int mode) {
        if (mode == 1) {
            navigationLayer.activate();
            currentRemotesControl.setActive(true);
            controlElements.updateLights();
        } else {
            navigationLayer.deactivate();
            currentRemotesControl.setActive(false);
        }
    }
    
    //handleIsNested
    public void triggerUpdateAction() {
        if (bankUpdateTask == null) {
            bankUpdateTask = this::updateBanks;
            host.scheduleTask(bankUpdateTask, 200);
        }
    }
    
    private void updateBanks() {
        midiProcessor.sendBankUpdate(mainBank.getBankConfig());
        midiProcessor.sendSelectionIndex(mainBank.getSelectionIndex(), new int[0]);
        bankUpdateTask = null;
    }
    
    @Override
    public void handleDeviceSelect(final int[] selectionPath) {
        //KompleteKontrolExtension.println(" SELECT INDEX = %s", Arrays.toString(selectionPath));
        this.mainBank.select(selectionPath);
    }
}
