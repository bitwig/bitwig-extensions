package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.mackie.definition.ControllerConfig;
import com.bitwig.extensions.controllers.mackie.definition.SubType;
import com.bitwig.extensions.controllers.mackie.display.MainUnitButton;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;

public class JogWheelTransportHandler {
   private static final double[] FFWD_SPEEDS_MPLUS = {4.0, 8.0, 16.0, 32.0};
   private static final double[] FFWD_SPEEDS_ALT_MPLUS = {0.25, 1.0, 4.0, 16.0};
   private static final double[] FFWD_SPEEDS = {0.0625, 0.25, 1.0, 4.0};
   private static final double[] FFWD_SPEEDS_SHIFT = {0.25, 1.0, 4.0, 16.0};
   private static final long[] FFWD_TIMES = {500, 1000, 2000, 3000, 4000};
   private static final int[] STAGE_MULTIPLIER = {1, 2, 4, 8, 8, 16, 32, 64};

   private final ModifierValueObject modifier;
   private final Transport transport;
   private final BooleanValueObject scrubActive = new BooleanValueObject();
   private HoldMenuButtonState holdAction;
   private long lastJogIncrement = 0L;
   private int lastJogDir = 0;
   private int jogWheelClickCount = 0;

   public JogWheelTransportHandler(MackieMcuProExtension driver, Transport transport, RelativeHardwareKnob fourDKnob,
                                   HoldMenuButtonState holdAction, Layer layer) {
      this.modifier = driver.getModifier();
      this.transport = transport;
      this.holdAction = holdAction;
      ControllerConfig controllerConfig = driver.getControllerConfig();
      ControllerHost host = driver.getHost();

      final HardwareActionBindable incAction;
      final HardwareActionBindable decAction;
      if (controllerConfig.getSubType() == SubType.M_PLUS) {
         incAction = host.createAction(() -> jogWheelPlayPositionMplus(1), () -> "+");
         decAction = host.createAction(() -> jogWheelPlayPositionMplus(-1), () -> "-");
      } else {
         incAction = host.createAction(() -> jogWheelPlayPosition(1), () -> "+");
         decAction = host.createAction(() -> jogWheelPlayPosition(-1), () -> "-");
      }
      layer.bind(fourDKnob, host.createRelativeHardwareControlStepTarget(incAction, decAction));

      final MainUnitButton rewindButton = new MainUnitButton(driver, BasicNoteOnAssignment.REWIND,
         controllerConfig.getSimulationLayout()).activateHoldState();
      final MainUnitButton fastForwardButton = new MainUnitButton(driver, BasicNoteOnAssignment.FFWD,
         controllerConfig.getSimulationLayout()).activateHoldState();

      if (controllerConfig.getSubType() == SubType.M_PLUS) {
         fastForwardButton.bindIsPressed(layer, pressed -> notifyHoldForwardReverseMPlatform(pressed, 1));
         rewindButton.bindIsPressed(layer, pressed -> notifyHoldForwardReverseMPlatform(pressed, -1));
      } else {
         fastForwardButton.bindIsPressed(layer, pressed -> notifyHoldForwardReverse(pressed, 1));
         rewindButton.bindIsPressed(layer, pressed -> notifyHoldForwardReverse(pressed, -1));
      }

      if (controllerConfig.getSubType() == SubType.M_PLUS) {
         MainUnitButton scrubEncoderPressButton = new MainUnitButton(driver, BasicNoteOnAssignment.SCRUB,
            controllerConfig.getSimulationLayout());
         scrubEncoderPressButton.bindPressed(layer, () -> scrubActive.toggle());
         scrubEncoderPressButton.bindLight(layer, scrubActive);
      }
   }

   private void jogWheelPlayPosition(final int dir) {
      double resolution = 0.25;
      if (modifier.isOptionSet()) {
         resolution = 4.0;
      } else if (modifier.isShiftSet()) {
         resolution = 1.0;
      }
      int stage = getStage(dir, STAGE_MULTIPLIER.length - 1, 40);

      changePlayPosition(dir, resolution * STAGE_MULTIPLIER[stage], !modifier.isOptionSet(), !modifier.isControlSet());
      lastJogDir = dir;
      lastJogIncrement = System.currentTimeMillis();
   }

   private void jogWheelPlayPositionMplus(final int dir) {
      double[] stages = scrubActive.get() ? FFWD_SPEEDS_ALT_MPLUS : FFWD_SPEEDS_MPLUS;
      int stage = getStage(dir, stages.length - 1, 80);
      changePlayPosition(dir, stages[stage], !modifier.isOptionSet(), !modifier.isControlSet());
      lastJogDir = dir;
      lastJogIncrement = System.currentTimeMillis();
   }

   private int getStage(int dir, int nrOfStages, int factor) {
      long timeLastChange = System.currentTimeMillis() - lastJogIncrement;
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

   public void notifyHoldForwardReverse(final Boolean pressed, final int dir) {
      if (pressed) {
         holdAction.start(stage -> {
            int stageIndex = Math.min(stage, FFWD_SPEEDS_SHIFT.length - 1);
            if (modifier.isShiftSet()) {
               changePlayPosition(dir, FFWD_SPEEDS_SHIFT[stageIndex], true, true);
            } else {
               changePlayPosition(dir, FFWD_SPEEDS[stageIndex], true, true);
            }
         }, FFWD_TIMES);
      } else {
         holdAction.stop();
      }
   }

   public void notifyHoldForwardReverseMPlatform(final Boolean pressed, final int dir) {
      if (pressed) {
         holdAction.start(stage -> {
            int stageIndex = Math.min(stage, FFWD_SPEEDS_MPLUS.length - 1);
            if (scrubActive.get()) {
               changePlayPosition(dir, FFWD_SPEEDS_ALT_MPLUS[stageIndex], true, true);
            } else {
               changePlayPosition(dir, FFWD_SPEEDS_MPLUS[stageIndex], true, true);
            }
         }, FFWD_TIMES);
      } else {
         holdAction.stop();
      }
   }

}
