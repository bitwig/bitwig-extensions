package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.EnumValueSetting;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;

import java.util.function.IntConsumer;

public class MenuDisplayLayerBuilder {
   private static final int MAX_SLOT_INDEX = 7;
   private final MenuModeLayerConfiguration control;
   private final DisplayLayer displayLayer;
   int currentSlot = 0;

   public MenuDisplayLayerBuilder(final MenuModeLayerConfiguration menuControl) {
      super();
      control = menuControl;
      displayLayer = menuControl.getDisplayLayer(0);
   }

   public void bindBool(final SettableBooleanValue value, final String trueString, final String falseString,
                        final ObjectProxy existSource, final String emptyString, final Runnable pressAction) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      displayLayer.bindBool(currentSlot, value, trueString, falseString, existSource, emptyString);
      control.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run());
      control.addRingBoolBinding(currentSlot, value);
      currentSlot++;
   }

   public void bindEncAction(final StringValue displayName, final IntConsumer pressAction) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      displayLayer.bindTitle(1, currentSlot, displayName);
      control.addPressEncoderBinding(currentSlot, encIndex -> pressAction.accept(encIndex));
      control.addRingFixedBinding(currentSlot);
      currentSlot++;
   }

   public void bindFixed(final String displayName, final Runnable pressAction) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      displayLayer.bindFixed(currentSlot, displayName);
      control.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run());
      control.addRingFixedBinding(currentSlot);
      currentSlot++;
   }

   public void bindBool(final String title, final SettableBooleanValue value) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addValueBinding(currentSlot, value, "< ON >", "<OFF >");
      control.addRingBoolBinding(currentSlot, value);
      control.addPressEncoderBinding(currentSlot, encIndex -> value.toggle());
      currentSlot++;
   }

   public <T> void bindValue(final String title, final ValueObject<T> enumValue) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addEncoderIncBinding(currentSlot, enumValue);
      control.addDisplayValueBinding(currentSlot, enumValue);
      currentSlot++;
   }

   public void bindValue(final String title, final IntValueObject value) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addEncoderIncBinding(currentSlot, value);
      control.addDisplayValueBinding(currentSlot, value);
      currentSlot++;
   }

   public void bindEnum(final String title, final EnumValueSetting values) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      final SettableEnumValue value = values.getValue();
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addDisplayValueBinding(currentSlot, value, values);
      control.addEncoderIncBinding(currentSlot, value, values);
      control.addRingBinding(currentSlot, value, values);
      currentSlot++;
   }

   public void bindValue(final String title, final SettableRangedValue value, final double sensitivity) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addDisplayValueBinding(currentSlot, value.displayedValue());
      control.addEncoderBinding(currentSlot, value, sensitivity);
      currentSlot++;
   }

   public void bindAction(final String title, final Runnable action) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addPressEncoderBinding(currentSlot, encIndex -> action.run());
      currentSlot++;
   }

   public void fillRest() {
      while (currentSlot < 8) {
         control.addRingFixedBinding(currentSlot);
         currentSlot++;
      }
   }

   public void insertEmpty() {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addRingFixedBinding(currentSlot);
      currentSlot++;
   }

}
