package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.value.*;

import java.util.function.BiConsumer;
import java.util.function.Function;
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
      control.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run(), true);
      control.addRingBoolBinding(currentSlot, value);
      currentSlot++;
   }

   public void bindEncAction(final StringValue displayName, final IntConsumer pressAction) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      displayLayer.bindTitle(1, currentSlot, displayName);
      control.addPressEncoderBinding(currentSlot, pressAction, true);
      control.addRingFixedBinding(currentSlot);
      currentSlot++;
   }

   public void bindFixed(final String displayName, final Runnable pressAction) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      displayLayer.bindFixed(currentSlot, displayName);
      control.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run(), true);
      control.addRingFixedBinding(currentSlot);
      currentSlot++;
   }

   public void bindBool(final String title, final Parameter value) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addValueBinding(currentSlot, value.value(), v -> v == 0 ? "<OFF>" : "<ON>");
      control.addRingBoolBinding(currentSlot, value);
      control.addPressEncoderBinding(currentSlot, encIndex -> {
         if (value.get() == 0) {
            value.set(1);
         } else {
            value.set(0);
         }
      }, false);
      currentSlot++;
   }

   public void bind(final BiConsumer<Integer, MenuModeLayerConfiguration> binder) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      binder.accept(currentSlot, control);
      currentSlot++;
   }

   public void bindValue(final String title, final SettableBeatTimeValue value, final IntConsumer pressAction,
                         final BeatTimeFormatter formatter, final double increment, final double shiftIncrement) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      value.markInterested();
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addValueBinding(currentSlot, value, v -> value.getFormatted(formatter));
      control.addRingFixedBindingActive(currentSlot);
      control.addEncoderIncBinding(currentSlot, value, increment, shiftIncrement);
      control.addPressEncoderBinding(currentSlot, pressAction, false);

      currentSlot++;
   }

   public void bindBool(final String title, final SettableBooleanValue value) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addValueBinding(currentSlot, value, "< ON >", "<OFF >");
      control.addRingBoolBinding(currentSlot, value);
      control.addPressEncoderBinding(currentSlot, encIndex -> value.toggle(), false);
      currentSlot++;
   }

   public <T> void bindValue(final String title, final ValueObject<T> enumValue) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addEncoderIncBinding(currentSlot, enumValue, false);
      control.addDisplayValueBinding(currentSlot, enumValue);
      currentSlot++;
   }

   public void bindValue(final String title, final IntValueObject value, final boolean accelerated) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addEncoderIncBinding(currentSlot, value, accelerated);
      control.addDisplayValueBinding(currentSlot, value);
      control.addRingBinding(currentSlot, value);
      currentSlot++;
   }

   public void bindValue(final String title, final IntValueObject value, final Function<Integer, String> converter,
                         final boolean accelerated) {
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addEncoderIncBinding(currentSlot, value, accelerated);
      control.addDisplayValueBinding(currentSlot, value);

      currentSlot++;
   }

   public void bindValueSet(final String title, final ValueSet value) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addDisplayValueBinding(currentSlot, value);
      control.addEncoderIncBinding(currentSlot, value, false);
      // TODO Press action
      control.addRingBinding(currentSlot, value);
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

   public void bindInc(final String title, final SettableRangedValue value, final IntConsumer encoderAction) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addDisplayValueBinding(currentSlot, value.displayedValue());
      control.addEncoderIncBinding(currentSlot, encoderAction);
      currentSlot++;
   }

   public void bindValue(final String title, final SettableRangedValue value, final double sensitivity,
                         final double resetValue) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addDisplayValueBinding(currentSlot, value.displayedValue());
      control.addEncoderBinding(currentSlot, value, sensitivity);
      control.addRingBinding(currentSlot, value);
      control.addPressEncoderBinding(currentSlot, v -> value.setImmediately(resetValue));
      currentSlot++;
   }

   public void bindValue(final String title, final SettableRangedValue value, final int range) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      value.markInterested();
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      control.addDisplayValueBinding(currentSlot, value.displayedValue());
      control.addEncoderIncBinding(currentSlot, inc -> {
         final double newValue = value.getRaw() + inc;
         if (newValue >= 0 && newValue < range) {
            value.setRaw(newValue);
         }
      });
      control.addRingBinding(currentSlot, value);
      currentSlot++;
   }

   public void bindAction(final String title, final String subTitle, final Runnable action) {
      if (currentSlot > MAX_SLOT_INDEX) {
         return;
      }
      control.addNameBinding(currentSlot, new BasicStringValue(title));
      if (subTitle != null) {
         control.addDisplayValueBinding(currentSlot, new BasicStringValue(subTitle));
      }
      control.addPressEncoderBinding(currentSlot, encIndex -> action.run(), true);
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
