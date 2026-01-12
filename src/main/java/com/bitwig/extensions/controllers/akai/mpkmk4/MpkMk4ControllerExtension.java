package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.controllers.akai.mpkmk4.layers.LayerCollection;
import com.bitwig.extensions.controllers.akai.mpkmk4.layers.LayerId;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

public class MpkMk4ControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private ControllerHost host;
    private final Variant variant;
    private HardwareSurface surface;
    private Layer mainLayer;
    private MpkMidiProcessor midiProcessor;
    private GlobalStates globalStates;
    private FocusMode recordFocusMode = FocusMode.LAUNCHER;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    public enum Variant {
        MINI
    }
    
    public MpkMk4ControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
        final Variant variant) {
        super(definition, host);
        this.variant = variant;
    }
    
    @Override
    public void init() {
        this.host = getHost();
        debugHost = host;
        final Context diContext = new Context(this);
        globalStates = new GlobalStates(this.variant);
        diContext.registerService(GlobalStates.class, globalStates);
        
        final LayerCollection layerCollection = diContext.getService(LayerCollection.class);
        
        mainLayer = layerCollection.get(LayerId.MAIN);
        surface = diContext.getService(HardwareSurface.class);
        midiProcessor = diContext.getService(MpkMidiProcessor.class);
        initTransport(diContext);
        
        midiProcessor.init();
        diContext.activate();
    }
    
    private void initTransport(final Context diContext) {
        final Transport transport = diContext.getService(Transport.class);
        final MpkViewControl viewControl = diContext.getService(MpkViewControl.class);
        final MpkHwElements hwElements = diContext.getService(MpkHwElements.class);
        final MpkButton shiftButton = hwElements.getShiftButton();
        final LayerCollection layerCollection = diContext.getService(LayerCollection.class);
        final Application application = diContext.getService(Application.class);
        final DocumentState documentState = getHost().getDocumentState();
        final MpkFocusClip focusClip = diContext.getService(MpkFocusClip.class);
        
        final SettableEnumValue recordButtonAssignment = documentState.getEnumSetting(
            "Record Button assignment", //
            "Transport", new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            recordFocusMode.getDescriptor());
        
        recordButtonAssignment.addValueObserver(value -> recordFocusMode = FocusMode.toMode(value));
        
        transport.isClipLauncherAutomationWriteEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerAutomationWriteEnabled().markInterested();
        transport.isArrangerOverdubEnabled().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        
        final Layer shiftLayer = layerCollection.get(LayerId.SHIFT);
        shiftButton.bindIsPressed(
            mainLayer, pressed -> {
                shiftLayer.setIsActive(pressed);
                globalStates.getShiftHeld().set(pressed);
                hwElements.applyShiftToEncoders(pressed);
            });
        
        final MpkMultiStateButton playButton = hwElements.getButton(MpkCcAssignment.PLAY);
        playButton.bindLightDimmed(mainLayer, transport.isPlaying());
        playButton.bindPressed(
            mainLayer, () -> {
                final boolean wasRecording = transport.isArrangerRecordEnabled().get();
                transport.restart();
                if (wasRecording) {
                    transport.isArrangerRecordEnabled().set(true);
                }
            });
        playButton.bindLightDimmed(shiftLayer, transport.isPlaying());
        playButton.bindPressed(shiftLayer, transport.continuePlaybackAction());
        
        final MpkMultiStateButton recordButton = hwElements.getButton(MpkCcAssignment.REC);
        recordButton.bindLight(mainLayer, () -> recordButtonState(transport, focusClip));
        recordButton.bindPressed(mainLayer, () -> handleRecordPressed(transport, focusClip));
        recordButton.bindLightOff(shiftLayer);
        recordButton.bindPressed(
            shiftLayer, () -> {
                if (recordFocusMode == FocusMode.LAUNCHER) {
                    focusClip.quantize(1.0);
                } else {
                    viewControl.invokeArrangerQuantize();
                }
            });
        
        final MpkMultiStateButton loopButton = hwElements.getButton(MpkCcAssignment.LOOP);
        loopButton.bindLightDimmed(mainLayer, transport.isArrangerLoopEnabled());
        loopButton.bindPressed(mainLayer, () -> transport.isArrangerLoopEnabled().toggle());
        
        final MpkMultiStateButton overdubButton = hwElements.getButton(MpkCcAssignment.OVER);
        overdubButton.bindLightDimmed(mainLayer, () -> isOverdubActive(transport));
        overdubButton.bindPressed(mainLayer, () -> handleOverdubPressed(transport));
        overdubButton.bindLightDimmed(shiftLayer, () -> isAutomationOverdubActive(transport));
        overdubButton.bindPressed(shiftLayer, () -> handleAutomationOverdubPressed(transport));
        
        
        final MpkMultiStateButton undoButton = hwElements.getButton(MpkCcAssignment.UNDO);
        undoButton.bindLightDimmed(mainLayer, application.canUndo());
        undoButton.bindPressed(mainLayer, application.undoAction());
        undoButton.bindLightDimmed(shiftLayer, application.canRedo());
        undoButton.bindPressed(shiftLayer, application.redoAction());
        
        final MpkMultiStateButton tempoButton = hwElements.getButton(MpkCcAssignment.TAP_TEMPO);
        tempoButton.bindLightPressedOnDimmed(mainLayer);
        tempoButton.bindPressed(mainLayer, transport.tapTempoAction());
        
        tempoButton.bindLightOnOff(shiftLayer, transport.isMetronomeEnabled());
        tempoButton.bindPressed(shiftLayer, () -> transport.isMetronomeEnabled().toggle());
    }
    
    private MpkMonoState recordButtonState(final Transport transport, final MpkFocusClip focusClip) {
        if (recordFocusMode == FocusMode.LAUNCHER) {
            return transport.isClipLauncherOverdubEnabled().get() ? MpkMonoState.FULL_ON : MpkMonoState.DIMMED;
        } else {
            return transport.isArrangerRecordEnabled().get() ? MpkMonoState.FULL_ON : MpkMonoState.DIMMED;
        }
    }
    
    private void handleRecordPressed(final Transport transport, final MpkFocusClip focusClip) {
        if (recordFocusMode == FocusMode.LAUNCHER) {
            focusClip.invokeRecord();
        } else {
            transport.record();
        }
    }
    
    private boolean isOverdubActive(final Transport transport) {
        if (recordFocusMode == FocusMode.LAUNCHER) {
            return transport.isClipLauncherOverdubEnabled().get();
        } else {
            return transport.isArrangerOverdubEnabled().get();
        }
    }
    
    private void handleOverdubPressed(final Transport transport) {
        if (recordFocusMode == FocusMode.LAUNCHER) {
            transport.isClipLauncherOverdubEnabled().toggle();
        } else {
            transport.isArrangerOverdubEnabled().toggle();
        }
    }
    
    private boolean isAutomationOverdubActive(final Transport transport) {
        return transport.isArrangerAutomationWriteEnabled().get();
    }
    
    private void handleAutomationOverdubPressed(final Transport transport) {
        //        if (recordFocusMode == FocusMode.LAUNCHER) {
        //            transport.isClipLauncherAutomationWriteEnabled().toggle();
        //        }
        transport.isArrangerAutomationWriteEnabled().toggle();
    }
    
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
    @Override
    public void exit() {
        midiProcessor.exit();
    }
    
}
