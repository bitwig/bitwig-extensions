package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.framework.Layer;

import java.util.Arrays;

public class EncoderLayer extends Layer {

   private final MixControl control;
   private final EncoderMode[] encoderModes = new EncoderMode[8];
   private int stepDivisor;

   public EncoderLayer(final MixControl mixControl, final String name) {
      super(mixControl.getDriver().getLayers(), name);
      control = mixControl;
      Arrays.fill(encoderModes, EncoderMode.ACCELERATED);
      stepDivisor = 64;
   }

   public void setEncoderMode(final EncoderMode encoderMode) {
      Arrays.fill(encoderModes, encoderMode);
   }

   public void setEncoderMode(final int index, final EncoderMode mode) {
      if (index >= 0 && index < encoderModes.length) {
         encoderModes[index] = mode;
      }
   }

   public void setStepDivisor(final int stepDivisor) {
      this.stepDivisor = stepDivisor;
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      control.getHwControls().setEncoderBehavior(encoderModes, stepDivisor);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
   }
}
