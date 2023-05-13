package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;

/**
 * Special binding for the encoder ring display, that also responds to being
 * enabled or not.
 */
public class RingParameterBankDisplayBinding extends RingDisplayBinding<ParameterPage> {

   private int lastValue;
   private int lastEnableValue;

   public RingParameterBankDisplayBinding(final ParameterPage source, final RingDisplay target) {
      super(target, source, RingDisplayType.FILL_LR_0);
      source.addIntValueObserver(v -> valueChange(source.getRingDisplayType().getOffset() + v));
      lastValue = source.getRingDisplayType().getOffset() + source.getIntValue();
   }

   public void handleEnabled(final int enableValue) {
      lastEnableValue = enableValue;
      if (isActive()) {
         update();
      }
   }

   public void update() {
      if (isActive()) {
         lastValue = getSource().getIntValue() + getSource().getRingDisplayType().getOffset();
         getTarget().sendValue(lastValue * lastEnableValue, false);
      }
   }

   private void valueChange(final int value) {
      lastValue = value;
      if (isActive()) {
         getTarget().sendValue(value * lastEnableValue, false);
      }
   }

   @Override
   protected void activate() {
      lastValue = getSource().getRingDisplayType().getOffset() + getSource().getIntValue();
      getTarget().sendValue(lastValue * lastEnableValue, false);
   }

   @Override
   protected int calcValue() {
      return 0;
   }

}
