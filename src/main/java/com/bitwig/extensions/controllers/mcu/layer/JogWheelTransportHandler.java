package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.TimedProcessor;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.controllers.mcu.control.MainHardwareSection;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class JogWheelTransportHandler {
    private static final double[] FFWD_SPEEDS_MPLUS = {4.0, 8.0, 16.0, 32.0};
    private static final double[] FFWD_SPEEDS_ALT_MPLUS = {0.25, 1.0, 4.0, 16.0};
    private static final double[] FFWD_SPEEDS = {0.0625, 0.25, 1.0, 4.0};
    private static final double[] FFWD_SPEEDS_SHIFT = {0.25, 1.0, 4.0, 16.0};
    private static final long[] FFWD_TIMES = {500, 1000, 2000, 3000, 4000};
    private static final int[] STAGE_MULTIPLIER = {1, 2, 4, 8, 8, 16, 32, 64};
    private final MainSection mainSection;
    private final TimedProcessor timedProcessor;
    private final GlobalStates states;
    private final Transport transport;
    private long lastJogIncrement = 0L;
    private int lastJogDir = 0;
    private int jogWheelClickCount = 0;
    private long timer;
    private MarkerAction pendingMarkerAction = MarkerAction.NONE;
    public static final int STAGE_HOLD_FACTOR = 20;
    
    private enum MarkerAction {
        NEXT,
        PREVIOUS,
        NONE
    }
    
    public JogWheelTransportHandler(final MainSection mainSection, final Context context,
        final MainHardwareSection hwElements) {
        final Layer layer = mainSection.getMainLayer();
        states = context.getService(GlobalStates.class);
        transport = context.getService(Transport.class);
        final Arranger arranger = context.getService(Arranger.class);
        transport.playStartPosition().markInterested();
        this.mainSection = mainSection;
        this.timedProcessor = context.getService(TimedProcessor.class);
        
        hwElements.bindJogWheel(layer, this::jogWheelPlayPosition);
        hwElements.getButton(McuFunction.FAST_FORWARD).ifPresent(fastForwardButton -> {
            fastForwardButton.bindHeldLight(layer);
            fastForwardButton.bindRepeatHold(layer,
                count -> movePlayPos(1, Math.min(count / STAGE_HOLD_FACTOR, FFWD_SPEEDS_SHIFT.length - 1)),
                () -> pendingMarkerAction = MarkerAction.NEXT);
        });
        hwElements.getButton(McuFunction.FAST_REVERSE).ifPresent(fastReverseButton -> {
            fastReverseButton.bindHeldLight(layer);
            fastReverseButton.bindRepeatHold(layer,  //
                count -> movePlayPos(-1, Math.min(count / STAGE_HOLD_FACTOR, FFWD_SPEEDS_SHIFT.length - 1)),
                () -> pendingMarkerAction = MarkerAction.PREVIOUS);
        });
        hwElements.getButton(McuFunction.CUE_MARKER)
            .ifPresent(cueButton -> cueButton.bindIsPressed(layer, pressed -> handleMarkerPressed(pressed)));
    }
    
    private void jogWheelPlayPosition(final int dir) {
        double resolution = 0.25;
        if (states.isOptionSet()) {
            resolution = 4.0;
        } else if (states.isShiftSet()) {
            resolution = 0.125 / 4;
        }
        final int stage = getStage(dir, STAGE_MULTIPLIER.length - 1, 40);
        
        changePlayPosition(dir, resolution * STAGE_MULTIPLIER[stage], !states.isOptionSet(), !states.isControlSet());
        lastJogDir = dir;
        lastJogIncrement = System.currentTimeMillis();
    }
    
    private void movePlayPos(final int dir, final int stageIndex) {
        if (states.isShiftSet()) {
            changePlayPosition(dir, FFWD_SPEEDS_SHIFT[stageIndex], true, true);
        } else {
            changePlayPosition(dir, FFWD_SPEEDS[stageIndex], true, true);
        }
    }
    
    private void handleMarkerPressed(final boolean pressed) {
        if (pressed) {
            timer = System.currentTimeMillis();
            timedProcessor.delayTask(() -> {
                if (timer != -1) {
                    mainSection.invokeCueMenu();
                }
            }, 5);
        } else if (pendingMarkerAction != MarkerAction.NONE) {
            if (pendingMarkerAction == MarkerAction.NEXT) {
                transport.jumpToNextCueMarker();
            } else {
                transport.jumpToPreviousCueMarker();
            }
            pendingMarkerAction = MarkerAction.NONE;
            timer = -1;
            mainSection.releaseMenu();
        } else {
            timer = -1;
            mainSection.releaseMenu();
        }
    }
    
    private int getStage(final int dir, final int nrOfStages, final int factor) {
        final long timeLastChange = System.currentTimeMillis() - lastJogIncrement;
        int stage = 0;
        if (lastJogDir == dir && timeLastChange < 200) {
            jogWheelClickCount++;
            stage = Math.min(nrOfStages, jogWheelClickCount / factor);
        } else {
            jogWheelClickCount = 0;
        }
        return stage;
    }
    
    private void changePlayPosition(final int inc, final double resolution, final boolean restrictToStart,
        final boolean quantize) {
        
        final double position = transport.playStartPosition().get();
        double newPos = position + resolution * inc;
        
        if (restrictToStart && newPos < 0) {
            newPos = 0;
        }
        
        if (position != newPos) {
            if (quantize) {
                final double intPosition = Math.floor(newPos / resolution);
                newPos = intPosition * resolution;
            }
            transport.playStartPosition().set(newPos);
            if (transport.isPlaying().get()) {
                transport.jumpToPlayStartPosition();
            }
        }
    }
    
}
