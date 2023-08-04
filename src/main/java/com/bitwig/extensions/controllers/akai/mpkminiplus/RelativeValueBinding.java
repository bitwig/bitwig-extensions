package com.bitwig.extensions.controllers.akai.mpkminiplus;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;

public class RelativeValueBinding extends Binding<RelativeHardwareKnob, SettableRangedValue> {

   private HardwareBinding hwBinding;

   public RelativeValueBinding(final RelativeHardwareKnob source, final SettableRangedValue target) {
      super(source, source, target);
   }

   protected RelativeHardwareControlBinding getHardwareBinding() {
      return getTarget().addBinding(getSource());
   }

   public void reset() {
      if (!isActive()) {
         return;
      }
      if (hwBinding != null) {
         hwBinding.removeBinding();
      }
      hwBinding = getHardwareBinding();
   }

   @Override
   protected void deactivate() {
      if (hwBinding != null) {
         hwBinding.removeBinding();
         hwBinding = null;
      }
   }

   @Override
   protected void activate() {
      if (hwBinding != null) {
         hwBinding.removeBinding();
      }
      hwBinding = getHardwareBinding();
   }

}
