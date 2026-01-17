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
import com.bitwig.extensions.controllers.akai.apc.common.control.ClickEncoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkCcAssignment;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.MpkMultiStateButton;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkMonoState;
import com.bitwig.extensions.controllers.akai.mpkmk4.layers.LayerCollection;
import com.bitwig.extensions.controllers.akai.mpkmk4.layers.LayerId;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

public class MpkMk4ControllerExtension extends ControllerExtension {
    
    private final String[] RECORD_QUANTIZE = {"OFF", "1/32", "1/16", "1/8", "1/4"};
    private static ControllerHost debugHost;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private ControllerHost host;
    private final Variant variant;
    private HardwareSurface surface;
    
    private Layer mainLayer;
    private Layer topEncoderLayer;
    
    private MpkMidiProcessor midiProcessor;
    private GlobalStates globalStates;
    private FocusMode recordFocusMode = FocusMode.LAUNCHER;
    
    private String recQuant;
    private boolean notifyQuant;
    private int quantIndex = 2;
    
    private LineDisplay mainDisplay;
    
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
        topEncoderLayer = layerCollection.get(LayerId.OVER_LAYER);
        surface = diContext.getService(HardwareSurface.class);
        midiProcessor = diContext.getService(MpkMidiProcessor.class);
        mainDisplay = diContext.getService(MpkHwElements.class).getMainLineDisplay();
        initTransport(diContext);
        
        midiProcessor.init();
        diContext.activate();
    }
    
    private void initTransport(final Context diContext) {
        final Transport transport = diContext.getService(Transport.class);
        final MpkHwElements hwElements = diContext.getService(MpkHwElements.class);
        final MpkButton shiftButton = hwElements.getShiftButton();
        final LayerCollection layerCollection = diContext.getService(LayerCollection.class);
        final Application application = diContext.getService(Application.class);
        final DocumentState documentState = getHost().getDocumentState();
        final MpkFocusClip focusClip = diContext.getService(MpkFocusClip.class);
        
        application.recordQuantizationGrid().addValueObserver(this::handleRecordQuant);
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
        shiftButton.bindIsPressed(mainLayer, pressed -> handleShift(pressed, shiftLayer, hwElements));
        
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
        recordButton.bindLight(mainLayer, () -> recordButtonState(transport));
        recordButton.bindPressed(mainLayer, () -> handleRecordPressed(transport, focusClip));
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
        
        final ClickEncoder encoder = hwElements.getMainEncoder();
        encoder.bind(topEncoderLayer, inc -> incrementQuantize(inc, application.recordQuantizationGrid()));
        recordButton.bindLight(shiftLayer, () -> "OFF".equals(recQuant) ? MpkMonoState.OFF : MpkMonoState.FULL_ON);
        recordButton.bindIsPressed(shiftLayer, pressed -> toggleRecMode(pressed, application.recordQuantizationGrid()));
        
    }
    
    private void handleShift(final Boolean pressed, final Layer shiftLayer, final MpkHwElements hwElements) {
        shiftLayer.setIsActive(pressed);
        globalStates.getShiftHeld().set(pressed);
        hwElements.applyShiftToEncoders(pressed);
        if (!pressed) {
            topEncoderLayer.setIsActive(false);
        }
    }
    
    private void incrementQuantize(final int inc, final SettableEnumValue quantValue) {
        final int newIndex = Math.max(1, Math.min(RECORD_QUANTIZE.length - 1, quantIndex + inc));
        if (newIndex != quantIndex) {
            quantIndex = newIndex;
            quantValue.set(RECORD_QUANTIZE[quantIndex]);
            notifyQuant = true;
        }
    }
    
    private void toggleRecMode(final boolean pressed, final SettableEnumValue quantValue) {
        if (pressed) {
            if ("OFF".equals(recQuant)) {
                quantValue.set(RECORD_QUANTIZE[quantIndex]);
            } else {
                quantValue.set("OFF");
            }
            mainDisplay.temporaryInfo(1, "Rec Quantize", recQuant);
            notifyQuant = true;
        }
        topEncoderLayer.setIsActive(pressed);
    }
    
    private void handleRecordQuant(final String recQuant) {
        this.recQuant = recQuant.toUpperCase();
        if (notifyQuant) {
            mainDisplay.temporaryInfo(1, "Rec Quantize", recQuant);
            notifyQuant = false;
        }
        
    }
    
    private MpkMonoState recordButtonState(final Transport transport) {
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
