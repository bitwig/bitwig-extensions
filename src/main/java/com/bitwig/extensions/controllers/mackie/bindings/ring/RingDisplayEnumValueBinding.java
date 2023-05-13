package com.bitwig.extensions.controllers.mackie.bindings.ring;

import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.EnumValueSetting;

public class RingDisplayEnumValueBinding extends RingDisplayBinding<SettableEnumValue> {

   private final EnumValueSetting values;

   public RingDisplayEnumValueBinding(final SettableEnumValue source, final RingDisplay target,
                                      final RingDisplayType type, final EnumValueSetting values) {
      super(target, source, type);
      this.values = values;
      source.addValueObserver(this::valueChange);
   }

   private void valueChange(final String value) {
      if (isActive()) {
         getTarget().sendValue(values.toIndexed(value) + type.getOffset(), false);
      }
   }

   protected int calcValue(final String value) {
      return values.toIndexed(value) + type.getOffset();
   }

   @Override
   protected int calcValue() {
      return calcValue(getSource().get());
   }


}
