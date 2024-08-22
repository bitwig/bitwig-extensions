package com.bitwig.extensions.controllers.mcu.layer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mcu.CursorDeviceControl;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.ViewControl;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.controllers.mcu.control.MotorSlider;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.control.RingEncoder;
import com.bitwig.extensions.controllers.mcu.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mcu.devices.SpecificDevice;
import com.bitwig.extensions.controllers.mcu.display.ControllerDisplay;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.value.Orientation;
import com.bitwig.extensions.controllers.mcu.value.TrackColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class MixerSection {
    private final Layer mainLayer;
    private final Layer emptyEncoderLayer;
    private LayerGroup activeLayerGroup;
    
    private LayerGroup deviceMenuLayer;
    private LayerGroup trackSendsMenuLayer;
    
    private final GlobalStates globalStates;
    private final ViewControl viewControl;
    private final ControllerDisplay display;
    private final DeviceModeLayer deviceModeLayer;
    private final SpecialDeviceModeLayer eqDeviceModeLayer;
    private VPotMode potMode = VPotMode.PAN;
    private final Map<ControlMode, ModeLayerGroup> layerSource = new HashMap<>();
    private final Map<ControlMode, MixerModeLayer> modeLayers = new HashMap<>();
    private final DisplayManager displayManager;
    private final TrackColor trackColor = new TrackColor();
    private ControlMode controlMode = ControlMode.PAN;
    private String[] remotePages;
    private int remoteIndex;
    private final int sectionIndex;
    
    private final MixingModeLayerCollection mainMixerLayerCollection;
    private final MixingModeLayerCollection globalMixerLayerCollection;
    private final ClipLaunchingLayer clipLaunchingLayer;
    private final TimedProcessor timedProcessor;
    
    public MixerSection(final Context diContext, final MixerSectionHardware hwElements, final MainSection mainSection,
        final int sectionIndex, final boolean isMain) {
        final Layers layers = diContext.getService(Layers.class);
        this.sectionIndex = sectionIndex;
        viewControl = diContext.getService(ViewControl.class);
        globalStates = diContext.getService(GlobalStates.class);
        timedProcessor = diContext.getService(TimedProcessor.class);
        final DeviceTypeBank deviceTypeBank = diContext.getService(DeviceTypeBank.class);
        final ControllerConfig config = diContext.getService(ControllerConfig.class);
        
        display = hwElements.getDisplay();
        displayManager = new DisplayManager(hwElements.getDisplay());
        mainLayer = new Layer(layers, "MAIN_LAYER");
        emptyEncoderLayer = new Layer(layers, "EMPTY ENCODER LAYERS");
        if (sectionIndex == 0) {
            mainSection.setupGlobalMenus(diContext, hwElements, this);
        }
        setupDeviceMenu(diContext, hwElements);
        final int trackOffset = sectionIndex * 8;
        
        deviceModeLayer = config.usesUnifiedDeviceControl()
            ? new UnifiedDeviceModeLayer(layers, ControlMode.STD_PLUGIN, this, viewControl.getCursorDeviceControl())
            : new SeparatedDeviceModeLayer(layers, ControlMode.STD_PLUGIN, this, deviceTypeBank);
        eqDeviceModeLayer =
            new SpecialDeviceModeLayer(layers, ControlMode.EQ, VPotMode.EQ, this, deviceTypeBank.getEqDevice());
        
        mainMixerLayerCollection =
            new MixingModeLayerCollection(diContext, viewControl.getMainTrackBank(), false, sectionIndex);
        globalMixerLayerCollection =
            new MixingModeLayerCollection(diContext, viewControl.getGlobalTrackBank(), true, sectionIndex);
        
        Arrays.stream(ControlMode.values()).filter(mode -> mode != ControlMode.MENU && !mode.isMixer())
            .forEach(mode -> layerSource.put(mode, new ModeLayerGroup(mode, layers, sectionIndex)));
        Arrays.stream(ControlMode.values()).filter(mode -> mode != ControlMode.MENU).forEach(mode -> {
            final MixerModeLayer modeLayer = create(mode, layers);
            modeLayers.put(mode, modeLayer);
        });
        
        mainMixerLayerCollection.bind(hwElements, displayManager, trackOffset, config.isHasDedicateVu());
        globalMixerLayerCollection.bind(hwElements, displayManager, trackOffset, config.isHasDedicateVu());
        
        bindSendsTrack(diContext, hwElements, viewControl.getCursorTrack());
        bindDevice(hwElements);
        bindRemotes(hwElements, ControlMode.TRACK_REMOTES);
        bindRemotes(hwElements, ControlMode.PROJECT_REMOTES);
        bindSpecialDevice(hwElements, deviceTypeBank.getEqDevice());
        bindEmpty(hwElements);
        hwElements.getMasterFader()
            .ifPresent(slider -> slider.bindParameter(mainLayer, viewControl.getRootTrack().volume()));
        
        clipLaunchingLayer = new ClipLaunchingLayer(diContext, hwElements, this);
        globalStates.getGlobalView().addValueObserver(this::handleGlobalStates);
        globalStates.getPotMode().addValueObserver(this::handlePotMode);
        globalStates.getFlipped().addValueObserver(this::handleFlipped);
        if (!config.hasLowerDisplay()) {
            display.getSlidersTouched().addValueObserver(this::handleTouched);
        }
        globalStates.getNameValue().addValueObserver(this::handleNameValue);
        globalStates.getClipLaunchingActive().addValueObserver(this::handleClipLaunchingState);
        
        hwElements.getBackgroundColoring()
            .ifPresent(backgroundColor -> mainLayer.bindLightState(() -> getTrackColor(trackOffset), backgroundColor));
        if (isMain && config.hasMasterVu()) {
            setUpMasterVu();
        }
        mainSection.addModeSelectListener((mode, pressed, selection) -> {
            modeLayers.get(controlMode).handleModePress(mode, pressed, selection);
        });
        mainSection.addDirectNavigationListeners(this::navigateDirect);
        mainSection.addNavigationInfoListener(this::handleInfoInvoked);
    }
    
    TrackColor getTrackColor(final int offset) {
        if (globalStates.getGlobalView().get()) {
            return viewControl.getColorGlobal(offset);
        }
        return viewControl.getColorMain(offset);
    }
    
    private void setupDeviceMenu(final Context context, final MixerSectionHardware hwElements) {
        final ViewControl viewControl = context.getService(ViewControl.class);
        final CursorDeviceControl cursorDeviceControl = viewControl.getCursorDeviceControl();
        final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();
        final MenuBuilder builder = new MenuBuilder("DEVICE_CONTROL", context, hwElements, displayManager);
        builder.addLabelMenu(new BasicStringValue("Device"), cursorDevice.name());
        builder.addToggleParameterMenu("Active", cursorDevice.isEnabled());
        builder.addToggleParameterMenu("Pinned", cursorDevice.isPinned());
        builder.addToggleParameterMenu("Expanded", cursorDevice.isExpanded());
        builder.addActionMenu("<Move", cursorDeviceControl::moveDeviceLeft);
        builder.addActionMenu("Move>", cursorDeviceControl::moveDeviceRight);
        builder.addActionMenu("Remove", cursorDevice::deleteObject);
        deviceMenuLayer = builder.getLayerGroup();
    }
    
    private MixerModeLayer create(final ControlMode mode, final Layers layers) {
        return switch (mode) {
            case STD_PLUGIN -> deviceModeLayer;
            case EQ -> eqDeviceModeLayer;
            case TRACK -> new AllSendsModeLayer(mode, this);
            case TRACK_REMOTES -> new ParameterPageLayer(layers, mode, this, "Track", viewControl.getTrackRemotes());
            case PROJECT_REMOTES ->
                new ParameterPageLayer(layers, mode, this, "Project", viewControl.getProjectRemotes());
            default -> new MixerModeLayer(mode, this);
        };
    }
    
    private void bindSendsTrack(final Context context, final MixerSectionHardware hwElements,
        final CursorTrack cursorTrack) {
        trackSendsMenuLayer = new LayerGroup(context, "ALL_SENDS");
        final ModeLayerGroup layer = layerSource.get(ControlMode.TRACK);
        for (int i = 0; i < 8; i++) {
            final MotorSlider slider = hwElements.getSlider(i);
            final RingEncoder encoder = hwElements.getRingEncoder(i);
            final Send sendItem = cursorTrack.sendBank().getItemAt(i);
            layer.bindControls(slider, encoder, RingDisplayType.FILL_LR, sendItem);
            layer.bindDisplay(displayManager, sendItem.name(), sendItem.exists(), sendItem, i);
            MixingModeLayerCollection.bindSendPrePost(trackSendsMenuLayer, hwElements, displayManager, i, sendItem);
        }
    }
    
    private void bindDevice(final MixerSectionHardware hwElements) {
        final CursorDeviceControl deviceControl = viewControl.getCursorDeviceControl();
        final CursorRemoteControlsPage remotes = deviceControl.getRemotes();
        final DeviceModeLayer deviceLayer = (DeviceModeLayer) modeLayers.get(ControlMode.STD_PLUGIN);
        for (int i = 0; i < 8; i++) {
            final RemoteControl parameter = remotes.getParameter(i);
            final RingEncoder encoder = hwElements.getRingEncoder(i);
            final MotorSlider slider = hwElements.getSlider(i);
            layerSource.get(ControlMode.STD_PLUGIN).bindControls(slider, encoder, RingDisplayType.FILL_LR, parameter);
            layerSource.get(ControlMode.STD_PLUGIN)
                .bindDisplay(displayManager, parameter.name(), parameter.exists(), parameter, i);
        }
        deviceControl.getCursorDevice().name().addValueObserver(deviceLayer::updateDeviceName);
        remotes.pageNames().addValueObserver(pages -> {
            this.remotePages = pages;
            updateRemotePages(deviceLayer);
        });
        remotes.selectedPageIndex().addValueObserver(pagesIndex -> {
            this.remoteIndex = pagesIndex;
            updateRemotePages(deviceLayer);
        });
    }
    
    private void bindRemotes(final MixerSectionHardware hwElements, final ControlMode mode) {
        final ParameterPageLayer layer = (ParameterPageLayer) modeLayers.get(mode);
        final CursorRemoteControlsPage remotes = layer.getRemotePages();
        final ModeLayerGroup modeLayerGroup = layerSource.get(mode);
        
        for (int i = 0; i < 8; i++) {
            final RemoteControl parameter = remotes.getParameter(i);
            final RingEncoder encoder = hwElements.getRingEncoder(i);
            final MotorSlider slider = hwElements.getSlider(i);
            modeLayerGroup.bindControls(slider, encoder, RingDisplayType.FILL_LR, parameter);
            modeLayerGroup.bindDisplay(displayManager, parameter.name(), parameter.exists(), parameter, i);
        }
    }
    
    private void bindSpecialDevice(final MixerSectionHardware hwElements, final SpecificDevice device) {
        final ModeLayerGroup layer = layerSource.get(ControlMode.EQ);
        for (int i = 0; i < 8; i++) {
            final RingEncoder encoder = hwElements.getRingEncoder(i);
            final MotorSlider slider = hwElements.getSlider(i);
            layer.bindControls(device, slider, encoder, i);
            layer.bindDisplay(device, displayManager, i);
        }
    }
    
    private void bindEmpty(final MixerSectionHardware hwElements) {
        for (int i = 0; i < 8; i++) {
            final RingEncoder encoder = hwElements.getRingEncoder(i);
            encoder.bindEmpty(emptyEncoderLayer);
        }
    }
    
    private void handleGlobalStates(final boolean globalStatesActive) {
        activateButtonLayer();
        modeLayers.get(controlMode).reassign();
    }
    
    public void activateButtonLayer() {
        if (globalStates.getClipLaunchingActive().get()) {
            clipLaunchingLayer.setIsActive(true);
            globalMixerLayerCollection.setIsActive(false);
            mainMixerLayerCollection.setIsActive(false);
        } else {
            clipLaunchingLayer.setIsActive(false);
            if (globalStates.getGlobalView().get()) {
                mainMixerLayerCollection.setIsActive(false);
                globalMixerLayerCollection.setIsActive(true);
            } else {
                globalMixerLayerCollection.setIsActive(false);
                mainMixerLayerCollection.setIsActive(true);
            }
        }
    }
    
    private void handlePotMode(final VPotMode oldMode, final VPotMode newMode) {
        potMode = newMode;
        deviceModeLayer.setPotMode(potMode);
        final ControlMode newControlMode = controlModeByVPotMode();
        if (newControlMode != controlMode) {
            modeLayers.get(controlMode).setIsActive(false);
            controlMode = newControlMode;
            modeLayers.get(controlMode).assign();
            modeLayers.get(controlMode).setIsActive(true);
        }
    }
    
    private void handleFlipped(final boolean flipped) {
        modeLayers.get(controlMode).reassign();
    }
    
    private void handleNameValue(final boolean value) {
        modeLayers.get(controlMode).reassign();
    }
    
    private void handleTouched(final boolean touched) {
        modeLayers.get(controlMode).reassign();
    }
    
    private void handleClipLaunchingState(final boolean active) {
        activateButtonLayer();
    }
    
    private void setUpMasterVu() {
        final Track rootTrack = viewControl.getRootTrack();
        rootTrack.addVuMeterObserver(14, 0, true, display::sendMasterVuUpdateL);
        rootTrack.addVuMeterObserver(14, 1, true, display::sendMasterVuUpdateR);
    }
    
    private void navigateDirect(final VPotMode mode, final int index, final boolean pressed) {
        if (potMode.getAssign() != VPotMode.BitwigType.CHANNEL) {
            if (pressed) {
                timedProcessor.startHoldEvent(
                    () -> modeLayers.get(controlMode).handleInfoState(true, Orientation.HORIZONTAL));
            } else {
                timedProcessor.completeHoldEvent(
                    () -> modeLayers.get(controlMode).handleInfoState(false, Orientation.HORIZONTAL));
            }
        }
    }
    
    private void handleInfoInvoked(final boolean start, final Orientation orientation) {
        if (potMode.getAssign() != VPotMode.BitwigType.CHANNEL) {
            modeLayers.get(controlMode).handleInfoState(start, orientation);
        }
    }
    
    private void updateRemotePages(final DeviceModeLayer deviceLayer) {
        if (remoteIndex == -1) {
            deviceLayer.updateParameterPage("");
        } else if (this.remotePages != null && remoteIndex < this.remotePages.length) {
            deviceLayer.updateParameterPage(remotePages[this.remoteIndex]);
        }
    }
    
    private ControlMode controlModeByVPotMode() {
        return switch (potMode) {
            case ALL_SENDS -> ControlMode.TRACK;
            case PAN -> ControlMode.PAN;
            case SEND -> ControlMode.SENDS;
            case PLUGIN, INSTRUMENT, MIDI_EFFECT, DEVICE -> ControlMode.STD_PLUGIN;
            case EQ -> ControlMode.EQ;
            case TRACK_REMOTE -> ControlMode.TRACK_REMOTES;
            case PROJECT_REMOTE -> ControlMode.PROJECT_REMOTES;
            default -> ControlMode.VOLUME;
        };
    }
    
    public int getSectionIndex() {
        return sectionIndex;
    }
    
    public Layer getEmptyEncoderLayer() {
        return emptyEncoderLayer;
    }
    
    public boolean isFlipped() {
        return globalStates.getFlipped().get();
    }
    
    public boolean isTouched() {
        return display.getSlidersTouched().get();
    }
    
    public boolean isNameValue() {
        return globalStates.getNameValue().get();
    }
    
    public boolean hasLowerDisplay() {
        return display.hasLower();
    }
    
    public DisplayManager getDisplayManager() {
        return displayManager;
    }
    
    public LayerGroup getActiveLayerGroup() {
        return activeLayerGroup;
    }
    
    public void setUpperLowerDestination(final ControlMode upper, final ControlMode lower) {
        displayManager.registerModeAssignment(upper, lower);
    }
    
    public ModeLayerGroup getLayerSource(final ControlMode mode) {
        if (mode.isMixer()) {
            if (globalStates.getGlobalView().get()) {
                return globalMixerLayerCollection.get(mode);
            }
            return mainMixerLayerCollection.get(mode);
        }
        return layerSource.get(mode);
    }
    
    public Layer getTrackDisplayLayer() {
        if (globalStates.getGlobalView().get()) {
            return globalMixerLayerCollection.getTrackDisplayLayer();
        }
        return mainMixerLayerCollection.getTrackDisplayLayer();
    }
    
    public void activate() {
        mainLayer.setIsActive(true);
        if (globalStates.getGlobalView().get()) {
            mainMixerLayerCollection.setIsActive(false);
            globalMixerLayerCollection.setIsActive(true);
        } else {
            globalMixerLayerCollection.setIsActive(false);
            mainMixerLayerCollection.setIsActive(true);
        }
        modeLayers.get(controlMode).setIsActive(true);
        display.refresh();
    }
    
    public void releaseLayer() {
        if (activeLayerGroup != null) {
            activeLayerGroup = null;
            modeLayers.get(controlMode).reassign();
        }
    }
    
    public LayerGroup getDeviceModeLayer() {
        return deviceMenuLayer;
    }
    
    public void activateSendPrePostMenu() {
        if (globalStates.trackModeActive()) {
            activateMenu(trackSendsMenuLayer);
        } else {
            final LayerGroup prePost = globalStates.getGlobalView().get()
                ? globalMixerLayerCollection.getSendsPrePostLayer()
                : mainMixerLayerCollection.getSendsPrePostLayer();
            activateMenu(prePost);
        }
    }
    
    public void activateMenu(final LayerGroup layerGroup) {
        activeLayerGroup = layerGroup;
        modeLayers.get(controlMode).reassign();
    }
    
}
