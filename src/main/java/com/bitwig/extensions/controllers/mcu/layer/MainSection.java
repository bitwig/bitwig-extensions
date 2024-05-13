package com.bitwig.extensions.controllers.mcu.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.McuExtension;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.ViewControl;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.controllers.mcu.control.MainHardwareSection;
import com.bitwig.extensions.controllers.mcu.control.McuButton;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.controllers.mcu.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.display.TimeCodeLed;
import com.bitwig.extensions.controllers.mcu.value.Orientation;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.values.LayoutType;

public class MainSection {
    
    private static final int SCROLL_REPEAT_INTERVAL = 100;
    private static final long SCROLL_START_TIME = 600;
    private final GlobalStates states;
    private final Layer mainLayer;
    private final Layer shiftLayer;
    private final Layer zoomLayer;
    private final ViewControl viewControl;
    private final DeviceTypeBank deviceTypeBank;
    private final List<ModeChangeListener> listeners = new ArrayList<>();
    private final List<DirectNavigationListener> directNavigationListeners = new ArrayList<>();
    private final List<NavigationInfoListener> navigationInfoListeners = new ArrayList<>();
    private String autoMode = "latch";
    private LayerGroup metroMenuLayer;
    private LayerGroup tempoMenuLayer;
    private LayerGroup cueMarkerMenuLayer;
    private LayerGroup sslDeviceMenuLayer;
    private LayerGroup loopMenuLayer;
    private LayerGroup groveMenuLayer;
    private LayerGroup zoomMenuLayer;
    private MixerSection attachedMixerLayer;
    private final Transport transport;
    private final CueMarkerBank cueMarkerBank;
    private final BeatTimeFormatter formatter;
    private final Application application;
    private final TimedProcessor timedProcessor;
    private TimeRepeatEvent scrollEvent = null;
    private LayoutType currentLayoutType;
    public static final int DECELERATION_THRESHOLD_MS = 100;
    long lastNavMessage = -1;
    
    
    @FunctionalInterface
    public interface ModeChangeListener {
        void handleModeSelected(VPotMode potMode, boolean pressed, boolean selection);
    }
    
    @FunctionalInterface
    public interface DirectNavigationListener {
        void handDirectNavigation(VPotMode mode, int index, boolean pressed);
    }
    
    @FunctionalInterface
    public interface NavigationInfoListener {
        void handleNavigationInfo(boolean start, Orientation orientation);
    }
    
    public MainSection(final Context context, final MainHardwareSection hwElements) {
        mainLayer = context.createLayer("MAIN_CONTROL");
        shiftLayer = context.createLayer("MAIN_CONTROL_SHIFT");
        zoomLayer = context.createLayer("ZOOM");
        
        this.states = context.getService(GlobalStates.class);
        timedProcessor = context.getService(TimedProcessor.class);
        this.viewControl = context.getService(ViewControl.class);
        this.deviceTypeBank = context.getService(DeviceTypeBank.class);
        this.application = context.getService(Application.class);
        final ControllerHost host = context.getService(ControllerHost.class);
        this.transport = context.getService(Transport.class);
        transport.getPosition().markInterested();
        cueMarkerBank = viewControl.getArranger().createCueMarkerBank(8);
        formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);
        final ControllerConfig config = context.getService(ControllerConfig.class);
        initModifiers(hwElements);
        initSendSelector(hwElements);
        
        hwElements.getButton(McuFunction.FLIP)
            .ifPresent(button -> button.bindToggle(mainLayer, this.states.getFlipped()));
        hwElements.getButton(McuFunction.NAME_VALUE)
            .ifPresent(button -> button.bindMomentary(mainLayer, this.states.getNameValue()));
        
        if (config.isNoDedicatedZoom()) {
            hwElements.getButton(McuFunction.ZOOM)
                .ifPresent(button -> button.bindPressed(mainLayer, () -> this.states.getZoomMode().toggle()));
        } else {
            hwElements.getButton(McuFunction.ZOOM)
                .ifPresent(button -> button.bindToggle(mainLayer, this.states.getZoomMode()));
        }
        this.states.getZoomMode().addValueObserver(zoomActive -> {
            zoomLayer.setIsActive(zoomActive);
        });
        potMode(hwElements);
        initTransport(hwElements, context.getService(Transport.class), context.getService(Application.class));
        initNavigation(hwElements, config);
        final JogWheelTransportHandler jogWheelTransportHandler =
            new JogWheelTransportHandler(this, context, hwElements);
    }
    
    private void initModifiers(final MainHardwareSection hwElements) {
        hwElements.getButton(McuFunction.GLOBAL_VIEW)
            .ifPresent(button -> button.bindToggle(mainLayer, this.states.getGlobalView()));
        hwElements.getButton(McuFunction.SHIFT).ifPresent(button -> {
            button.bindMomentary(mainLayer, this.states.getShift());
            this.states.getShift().addValueObserver(shiftLayer::setIsActive);
        });
        hwElements.getButton(McuFunction.CONTROL)
            .ifPresent(button -> button.bindMomentary(mainLayer, this.states.getControl()));
        hwElements.getButton(McuFunction.OPTION)
            .ifPresent(button -> button.bindMomentary(mainLayer, this.states.getOption()));
        hwElements.getButton(McuFunction.CLIP_LAUNCHER_MODE_2).ifPresent(button -> {
            button.bindToggle(mainLayer, states.getClipLaunchingActive());
        });
        hwElements.getButton(McuFunction.CLEAR)
            .ifPresent(button -> button.bindMomentary(mainLayer, this.states.getClearHeld()));
        hwElements.getButton(McuFunction.DUPLICATE)
            .ifPresent(button -> button.bindMomentary(mainLayer, this.states.getDuplicateHeld()));
        hwElements.getButton(McuFunction.CLIP_LAUNCHER_MODE_4).ifPresent(button -> {
            button.bindToggle(mainLayer, states.getClipLaunchingActive());
            button.bindIsPressed(mainLayer, pressed -> McuExtension.println(" >> %s", pressed));
        });
    }
    
    private void initSendSelector(final MainHardwareSection hwElements) {
        if (hwElements.getButton(McuFunction.SEND_SELECT_1).isPresent()) {
            hwElements.getButton(McuFunction.SEND_SELECT_1)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(0, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_2)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(1, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_3)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(2, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_4)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(3, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_5)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(4, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_6)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(5, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_7)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(6, pressed)));
            hwElements.getButton(McuFunction.SEND_SELECT_8)
                .ifPresent(button -> button.bindIsPressed(mainLayer, pressed -> directSendSelect(7, pressed)));
        }
        hwElements.getButton(McuFunction.TRACK_MODE).ifPresent(button -> {
            button.bindPressed(mainLayer, states::toggleTrackMode);
            button.bindLight(mainLayer, states::trackModeActive);
        });
        
    }
    
    public void potMode(final MainHardwareSection hwElements) {
        //viewControl.getCursorDeviceControl().
        if (hwElements.getButton(McuFunction.TRACK_MODE).isPresent()) {
            hwElements.getButton(McuFunction.MODE_SEND).ifPresent(this::bindSendsModeDual);
        } else {
            hwElements.getButton(McuFunction.MODE_SEND).ifPresent(button -> bindMode(button, VPotMode.SEND));
            hwElements.getButton(McuFunction.MODE_ALL_SENDS).ifPresent(button -> bindMode(button, VPotMode.ALL_SENDS));
        }
        hwElements.getButton(McuFunction.MODE_PAN).ifPresent(button -> bindMode(button, VPotMode.PAN));
        hwElements.getButton(McuFunction.MODE_DEVICE).ifPresent(button -> bindMode(button, VPotMode.DEVICE));
        
        hwElements.getButton(McuFunction.MODE_EQ).ifPresent(button -> bindMode(button, VPotMode.EQ));
        hwElements.getButton(McuFunction.MODE_TRACK_REMOTE)
            .ifPresent(button -> bindMode(button, VPotMode.TRACK_REMOTE));
        hwElements.getButton(McuFunction.MODE_PROJECT_REMOTE)
            .ifPresent(button -> bindMode(button, VPotMode.PROJECT_REMOTE));
    }
    
    public void initTransport(final MainHardwareSection hwElements, final Transport transport,
        final Application application) {
        transport.automationWriteMode().addValueObserver(v -> this.autoMode = v);
        
        hwElements.getButton(McuFunction.AUTO_LATCH)
            .ifPresent(button -> bindAutoMode(button, transport.automationWriteMode(), "latch"));
        hwElements.getButton(McuFunction.AUTO_WRITE)
            .ifPresent(button -> bindAutoMode(button, transport.automationWriteMode(), "write"));
        hwElements.getButton(McuFunction.AUTO_TOUCH)
            .ifPresent(button -> bindAutoMode(button, transport.automationWriteMode(), "touch"));
        hwElements.getButton(McuFunction.AUTO_READ)
            .ifPresent(button -> button.bindToggle(mainLayer, transport.isArrangerAutomationWriteEnabled()));
        hwElements.getButton(McuFunction.PUNCH_IN)
            .ifPresent(button -> button.bindToggle(mainLayer, transport.isPunchInEnabled()));
        hwElements.getButton(McuFunction.PUNCH_OUT)
            .ifPresent(button -> button.bindToggle(mainLayer, transport.isPunchOutEnabled()));
        
        hwElements.getButton(McuFunction.PLAY).ifPresent(button -> {
            button.bindLight(mainLayer, transport.isPlaying());
            button.bindPressed(mainLayer, transport.playAction());
        });
        
        hwElements.getButton(McuFunction.STOP).ifPresent(stopButton -> {
            stopButton.bindHeldLight(mainLayer);
            stopButton.bindPressed(mainLayer, transport.stopAction());
        });
        hwElements.getButton(McuFunction.RECORD).ifPresent(recordButton -> {
            recordButton.bindToggle(mainLayer, transport.isArrangerRecordEnabled());
            recordButton.bindToggle(shiftLayer, transport.isArrangerOverdubEnabled());
        });
        hwElements.getButton(McuFunction.OVERDUB).ifPresent(overdubButton -> {
            overdubButton.bindToggle(mainLayer, transport.isArrangerOverdubEnabled());
            overdubButton.bindToggle(shiftLayer, transport.isClipLauncherOverdubEnabled());
        });
        hwElements.getButton(McuFunction.AUTOMATION_LAUNCHER).ifPresent(button -> {
            button.bindToggle(mainLayer, transport.isClipLauncherAutomationWriteEnabled());
        });
        hwElements.getButton(McuFunction.RESTORE_AUTOMATION).ifPresent(restoreAutoButton -> {
            restoreAutoButton.bindPressed(mainLayer, () -> transport.resetAutomationOverrides());
            restoreAutoButton.bindLight(mainLayer, transport.isAutomationOverrideActive());
        });
        hwElements.getButton(McuFunction.METRO).ifPresent(metroButton -> {
            metroButton.bindClickAltMenu(mainLayer, () -> transport.isMetronomeEnabled().toggle(),
                pressed -> handleMenuPressed(pressed, metroMenuLayer));
            metroButton.bindLight(mainLayer, transport.isMetronomeEnabled());
        });
        hwElements.getButton(McuFunction.TEMPO).ifPresent(
            tempoButton -> tempoButton.bindIsPressed(mainLayer, pressed -> handleMenuPressed(pressed, tempoMenuLayer)));
        hwElements.getButton(McuFunction.GROOVE_MENU).ifPresent(grooveButton -> grooveButton.bindIsPressed(mainLayer,
            pressed -> handleMenuPressed(pressed, groveMenuLayer)));
        hwElements.getButton(McuFunction.ZOOM_MENU).ifPresent(
            zoomMenu -> zoomMenu.bindIsPressed(mainLayer, pressed -> handleMenuPressed(pressed, zoomMenuLayer)));
        hwElements.getButton(McuFunction.UNDO).ifPresent(undoButton -> {
            undoButton.bindPressed(mainLayer, application.undoAction());
            undoButton.bindLight(mainLayer, application.canUndo());
            undoButton.bindPressed(shiftLayer, application.redoAction());
            undoButton.bindLight(shiftLayer, application.canRedo());
        });
        hwElements.getButton(McuFunction.SSL_PLUGINS_MENU).ifPresent(
            sslMenuButton -> sslMenuButton.bindIsPressed(mainLayer,
                pressed -> handleMenuPressed(pressed, sslDeviceMenuLayer)));
        
        hwElements.getButton(McuFunction.LOOP).ifPresent(loopButton -> {
            loopButton.bindClickAltMenu(mainLayer, () -> transport.isArrangerLoopEnabled().toggle(),
                pressed -> handleMenuPressed(pressed, loopMenuLayer));
            loopButton.bindLight(mainLayer, transport.isArrangerLoopEnabled());
        });
        application.panelLayout().addValueObserver(v -> currentLayoutType = LayoutType.toType(v));
        hwElements.getButton(McuFunction.ARRANGER).ifPresent(button -> {
            button.bindPressed(mainLayer, () -> {
                this.application.setPanelLayout(currentLayoutType.other().getName());
            });
            button.bindLight(mainLayer, () -> currentLayoutType == LayoutType.ARRANGER);
        });
        
        hwElements.getTimeCodeLed().ifPresent(timeCodeLed -> assignTimeCodeDisplay(transport, timeCodeLed, hwElements));
    }
    
    private void initNavigation(final MainHardwareSection hwElements, final ControllerConfig config) {
        final boolean withHold = !config.hasNavigationWithJogWheel();
        hwElements.getButton(McuFunction.ZOOM_IN).ifPresent(button -> bindZoom(button, -1));
        hwElements.getButton(McuFunction.ZOOM_OUT).ifPresent(button -> bindZoom(button, 1));
        hwElements.getButton(McuFunction.PAGE_LEFT).ifPresent(button -> bindPageNav(button, -1));
        hwElements.getButton(McuFunction.PAGE_RIGHT).ifPresent(button -> bindPageNav(button, 1));
        hwElements.getButton(McuFunction.CHANNEL_LEFT).ifPresent(button -> bindChannelNav(button, -1, withHold));
        hwElements.getButton(McuFunction.CHANNEL_RIGHT).ifPresent(button -> bindChannelNav(button, 1, withHold));
        hwElements.getButton(McuFunction.BANK_LEFT).ifPresent(button -> bindChannelNav(button, -8, withHold));
        hwElements.getButton(McuFunction.BANK_RIGHT).ifPresent(button -> bindChannelNav(button, 8, withHold));
        
        if (config.isDecelerateJogWheel()) {
            hwElements.getButton(McuFunction.NAV_LEFT).ifPresent(button -> bindDeceleratedHorizontalNav(button, -1));
            hwElements.getButton(McuFunction.NAV_RIGHT).ifPresent(button -> bindDeceleratedHorizontalNav(button, 1));
            hwElements.getButton(McuFunction.NAV_UP).ifPresent(button -> bindDeceleratedVerticalNav(button, -1));
            hwElements.getButton(McuFunction.NAV_DOWN).ifPresent(button -> bindDeceleratedVerticalNav(button, 1));
        } else {
            hwElements.getButton(McuFunction.NAV_LEFT).ifPresent(button -> bindHorizontalNav(button, -1, withHold));
            hwElements.getButton(McuFunction.NAV_RIGHT).ifPresent(button -> bindHorizontalNav(button, 1, withHold));
            hwElements.getButton(McuFunction.NAV_UP).ifPresent(button -> bindVerticalNav(button, 1, withHold));
            hwElements.getButton(McuFunction.NAV_DOWN).ifPresent(button -> bindVerticalNav(button, -1, withHold));
        }
    }
    
    private void directSendSelect(final int index, final boolean pressed) {
        if (pressed) {
            switch (states.getPotMode().get()) {
                case SEND -> viewControl.navigateToSends(index);
                case DEVICE, PLUGIN, INSTRUMENT, MIDI_EFFECT -> {
                    if (states.isShiftSet()) {
                        viewControl.getCursorDeviceControl().navigateToPage(index);
                    } else {
                        viewControl.getCursorDeviceControl().navigateToDeviceInChain(index);
                    }
                }
                case TRACK_REMOTE -> viewControl.navigateToTrackRemotePage(index);
                case PROJECT_REMOTE -> viewControl.navigateToProjectRemotePage(index);
                case EQ -> deviceTypeBank.getEqDevice().navigateToDeviceParameters(index);
            }
        }
        directNavigationListeners.forEach(
            listener -> listener.handDirectNavigation(states.getPotMode().get(), index, pressed));
    }
    
    private void bindSendsModeDual(final McuButton button) {
        button.bindMode(mainLayer, this::handleSendsVPotModePressed, //
            () -> states.getPotMode().get() == VPotMode.SEND
                || states.getPotMode().get() == VPotMode.ALL_SENDS); // Maybe re propagate on release
    }
    
    private void bindMode(final McuButton button, final VPotMode potMode) {
        button.bindMode(mainLayer, pressed -> handleVPotModePressed(potMode, pressed),
            () -> states.getPotMode().get() == potMode); // Maybe re propagate on release
    }
    
    private void bindAutoMode(final McuButton button, final SettableEnumValue value, final String mode) {
        button.bindLight(mainLayer, () -> this.autoMode.equals(mode));
        button.bindPressed(mainLayer, () -> value.set(mode));
    }
    
    private void handleMenuPressed(final boolean pressed, final LayerGroup menuLayer) {
        if (pressed) {
            attachedMixerLayer.activateMenu(menuLayer);
        } else {
            attachedMixerLayer.releaseLayer();
        }
    }
    
    private void assignTimeCodeDisplay(final Transport transport, final TimeCodeLed timeCodeLed,
        final MainHardwareSection hwElements) {
        timeCodeLed.refreshMode();
        transport.timeSignature().addValueObserver(timeCodeLed::setDivision);
        transport.playPosition().addValueObserver(timeCodeLed::updatePosition);
        transport.playPositionInSeconds().addValueObserver(timeCodeLed::updateTime);
        hwElements.getButton(McuFunction.DISPLAY_SMPTE).ifPresent(button -> {
            button.bindLight(mainLayer, () -> timeCodeLed.getMode() == TimeCodeLed.Mode.BEATS);
            button.bindPressed(mainLayer, timeCodeLed::toggleMode);
        });
        states.getTwoSegmentText().addValueObserver(v -> timeCodeLed.setAssignment(v));
    }
    
    private void bindZoom(final McuButton button, final int dir) {
        button.bindPressed(mainLayer, () -> zoomLeftRight(dir));
    }
    
    private void bindPageNav(final McuButton button, final int dir) {
        button.bindPressed(mainLayer, () -> handlePageNavigation(dir));
    }
    
    private void bindChannelNav(final McuButton button, final int dir, final boolean withHoldFunction) {
        if (withHoldFunction) {
            button.bindRepeatHold(mainLayer, () -> viewControl.navigateChannels(dir));
        } else {
            button.bindPressed(mainLayer, () -> viewControl.navigateChannels(dir));
        }
    }
    
    private void bindDeceleratedHorizontalNav(final McuButton button, final int dir) {
        button.bindPressed(mainLayer, () -> {
            if (needsToDecelerate()) {
                return;
            }
            handleNavigationHorizontal(dir);
            lastNavMessage = System.currentTimeMillis();
        });
        button.bindPressed(zoomLayer, () -> {
            if (needsToDecelerate()) {
                return;
            }
            zoomLeftRight(dir);
            lastNavMessage = System.currentTimeMillis();
        });
    }
    
    private void bindHorizontalNav(final McuButton button, final int dir, final boolean withHold) {
        if (withHold) {
            button.bindDelayedAction(mainLayer, start -> handleNavigationHorizontal(dir, start), //
                () -> notifyInfo(true, Orientation.HORIZONTAL), //
                () -> notifyInfo(false, Orientation.HORIZONTAL), //
                600);
        } else {
            button.bindPressed(mainLayer, () -> handleNavigationHorizontal(dir));
        }
        button.bindRepeatHold(zoomLayer, () -> zoomLeftRight(dir));
    }
    
    private void bindDeceleratedVerticalNav(final McuButton button, final int dir) {
        button.bindPressed(mainLayer, () -> {
            if (needsToDecelerate()) {
                return;
            }
            handleNavigationVertical(-dir);
            lastNavMessage = System.currentTimeMillis();
        });
        button.bindPressed(zoomLayer, () -> {
            if (needsToDecelerate()) {
                return;
            }
            zoomUpDown(-dir);
            lastNavMessage = System.currentTimeMillis();
        });
    }
    
    private void bindVerticalNav(final McuButton button, final int dir, final boolean withHold) {
        if (withHold) {
            button.bindDelayedAction(mainLayer, start -> handleNavigationVertical(dir, start), //
                () -> notifyInfo(true, Orientation.VERTICAL), //
                () -> notifyInfo(false, Orientation.VERTICAL), //
                600);
        } else {
            button.bindPressed(mainLayer, () -> handleNavigationVertical(dir));
        }
        button.bindRepeatHold(zoomLayer, () -> zoomUpDown(dir));
    }
    
    private void handleSendsVPotModePressed(final boolean pressed) {
        final VPotMode current = states.getPotMode().get();
        final boolean changeByPress = current != VPotMode.ALL_SENDS && current != VPotMode.SEND;
        if (pressed && changeByPress) {
            states.getPotMode().set(states.getLastSendsMode());
        }
        notifyMode(states.getPotMode().get(), pressed, changeByPress);
    }
    
    private void handleVPotModePressed(final VPotMode potMode, final boolean pressed) {
        final boolean selection = states.getPotMode().get() != potMode;
        if (pressed) {
            states.getPotMode().set(potMode);
        }
        notifyMode(states.getPotMode().get(), pressed, selection);
        
        if (Objects.requireNonNull(states.getPotMode().get()) == VPotMode.EQ) {
            if (pressed && !selection && !deviceTypeBank.getEqDevice().isSpecificDevicePresent()) {
                deviceTypeBank.getEqDevice().insertDevice();
            }
        }
    }
    
    private void handlePageNavigation(final int dir) {
        switch (states.getPotMode().get()) {
            case PLUGIN, INSTRUMENT, MIDI_EFFECT, DEVICE ->
                selectRemotes(viewControl.getCursorDeviceControl().getRemotes(), dir);
            case TRACK_REMOTE -> selectRemotes(viewControl.getTrackRemotes(), dir);
            case PROJECT_REMOTE -> selectRemotes(viewControl.getProjectRemotes(), dir);
            case EQ -> deviceTypeBank.getEqDevice().navigateDeviceParameters(dir);
            case SEND -> viewControl.navigateSends(dir);
            case PAN -> selectTracks(dir);
        }
    }
    
    private void handleNavigationHorizontal(final int dir) {
        switch (states.getPotMode().get()) {
            case PLUGIN, INSTRUMENT, MIDI_EFFECT, DEVICE -> selectDeviceInChain(dir);
            case PAN, SEND, TRACK_REMOTE, PROJECT_REMOTE, EQ, ALL_SENDS -> selectTracks(dir);
        }
    }
    
    private void handleNavigationHorizontal(final int dir, final boolean start) {
        switch (states.getPotMode().get()) {
            case PLUGIN, INSTRUMENT, MIDI_EFFECT, DEVICE -> {
                if (start) {
                    selectDeviceInChain(dir);
                }
            }
            case PAN, SEND, TRACK_REMOTE, PROJECT_REMOTE, EQ, ALL_SENDS -> repeatEvent(() -> selectTracks(dir), start);
        }
    }
    
    public void handleNavigationVertical(final int dir, final boolean start) {
        if (needsToDecelerate()) {
            return;
        }
        if (states.getClipLaunchingActive().get()) {
            repeatEvent(() -> viewControl.navigateClipVertical(dir), start);
        } else if (states.getPotMode().get() == VPotMode.PAN || states.getPotMode().get() == VPotMode.ALL_SENDS) {
            repeatEvent(() -> selectTracks(-dir), start);
        } else if (start) {
            handleNavigationVertical(dir);
        }
    }
    
    private boolean needsToDecelerate() {
        final long diff = System.currentTimeMillis() - lastNavMessage;
        if (diff < DECELERATION_THRESHOLD_MS) {
            if (diff == 0) {
                lastNavMessage = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }
    
    public void handleNavigationVertical(final int dir) {
        if (states.getClipLaunchingActive().get()) {
            viewControl.navigateClipVertical(dir);
        } else {
            switch (states.getPotMode().get()) {
                case PLUGIN, INSTRUMENT, MIDI_EFFECT, DEVICE ->
                    selectRemotes(viewControl.getCursorDeviceControl().getRemotes(), -dir);
                case EQ -> deviceTypeBank.getEqDevice().navigateDeviceParameters(-dir);
                case TRACK_REMOTE -> selectRemotes(viewControl.getTrackRemotes(), -dir);
                case PROJECT_REMOTE -> selectRemotes(viewControl.getProjectRemotes(), -dir);
                case PAN, ALL_SENDS -> selectTracks(-dir);
                case SEND -> viewControl.navigateSends(-dir);
            }
        }
    }
    
    private void notifyInfo(final boolean start, final Orientation orientation) {
        navigationInfoListeners.forEach(listener -> listener.handleNavigationInfo(start, orientation));
    }
    
    private void zoomLeftRight(final int dir) {
        if (states.isShiftSet()) {
            if (dir > 0) {
                viewControl.getDetailEditor().zoomIn();
            } else {
                viewControl.getDetailEditor().zoomOut();
            }
        } else {
            if (dir > 0) {
                viewControl.getArranger().zoomIn();
            } else {
                viewControl.getArranger().zoomOut();
            }
        }
    }
    
    private void zoomUpDown(final int dir) {
        final Arranger arranger = viewControl.getArranger();
        if (states.isShiftSet()) {
            if (dir > 0) {
                arranger.zoomOutLaneHeightsSelected();
            } else {
                arranger.zoomInLaneHeightsSelected();
            }
        } else {
            if (dir > 0) {
                arranger.zoomOutLaneHeightsAll();
            } else {
                arranger.zoomInLaneHeightsAll();
            }
        }
    }
    
    void notifyMode(final VPotMode mode, final boolean pressed, final boolean selection) {
        listeners.forEach(listener -> listener.handleModeSelected(mode, pressed, selection));
    }
    
    private void selectRemotes(final CursorRemoteControlsPage remotes, final int dir) {
        if (dir > 0) {
            remotes.selectNext();
        } else {
            remotes.selectPrevious();
        }
    }
    
    private void repeatEvent(final Runnable action, final boolean start) {
        if (scrollEvent != null) {
            scrollEvent.cancel();
        }
        if (start) {
            scrollEvent = new TimeRepeatEvent(action, SCROLL_START_TIME, SCROLL_REPEAT_INTERVAL);
            action.run();
            timedProcessor.queueEvent(scrollEvent);
        }
    }
    
    private void selectTracks(final int dir) {
        if (dir > 0) {
            viewControl.getCursorTrack().selectNext();
        } else {
            viewControl.getCursorTrack().selectPrevious();
        }
    }
    
    private void selectDeviceInChain(final int dir) {
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDeviceControl().getCursorDevice();
        if (dir > 0) {
            cursorDevice.selectNext();
        } else {
            cursorDevice.selectPrevious();
        }
    }
    
    public Layer getMainLayer() {
        return mainLayer;
    }
    
    void addModeSelectListener(final ModeChangeListener listener) {
        listeners.add(listener);
    }
    
    void addDirectNavigationListeners(final DirectNavigationListener listener) {
        directNavigationListeners.add(listener);
    }
    
    void addNavigationInfoListener(final NavigationInfoListener listener) {
        navigationInfoListeners.add(listener);
    }
    
    public void activate() {
        mainLayer.setIsActive(true);
    }
    
    public void setupGlobalMenus(final Context context, final MixerSectionHardware hwElements,
        final MixerSection attachedMixerLayer) {
        final DisplayManager displayManager = attachedMixerLayer.getDisplayManager();
        final ControllerConfig config = context.getService(ControllerConfig.class);
        this.attachedMixerLayer = attachedMixerLayer;
        final MenuConfigure menuBuilder = new MenuConfigure(context, hwElements, displayManager);
        metroMenuLayer = menuBuilder.createMetroMenu();
        tempoMenuLayer = menuBuilder.createTempoMenu();
        cueMarkerMenuLayer = menuBuilder.createCueMarkerMenu(cueMarkerBank);
        loopMenuLayer = menuBuilder.createLoopMenu();
        groveMenuLayer = menuBuilder.createGrooveMenu();
        zoomMenuLayer = menuBuilder.createViewZoomMenu();
        if (config.hasAssignment(McuFunction.SSL_PLUGINS_MENU)) {
            sslDeviceMenuLayer = menuBuilder.createSslMenu();
        }
    }
    
    
    public void invokeCueMenu() {
        attachedMixerLayer.activateMenu(cueMarkerMenuLayer);
    }
    
    public void releaseMenu() {
        attachedMixerLayer.releaseLayer();
    }
}
