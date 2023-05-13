package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.controller.api.SettableEnumValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumValueSetting {

   private final List<String> values = new ArrayList<>();
   private final Map<String, String> valueToDisplay = new HashMap<>();
   private final Map<String, Integer> valueToIndex = new HashMap<>();
   private final Map<String, Integer> valueToRingValue = new HashMap<>();
   private final SettableEnumValue value;

   public EnumValueSetting(final SettableEnumValue value) {
      this.value = value;
   }

   public SettableEnumValue getValue() {
      return value;
   }

   public void increment(final SettableEnumValue value, final int inc) {
      final String current = value.get();
      final Integer index = valueToIndex.get(current);
      if (index != null) {
         final int newIndex = index + inc;
         if (newIndex >= 0 && newIndex < valueToDisplay.size()) {
            final String newEnum = values.get(newIndex);
            value.set(newEnum);
         }
      }
   }

   public EnumValueSetting add(final String enumValue, final int ringValue) {
      final int size = values.size();
      values.add(enumValue);
      valueToDisplay.put(enumValue, enumValue);
      valueToIndex.put(enumValue, size);
      valueToRingValue.put(enumValue, ringValue);
      return this;
   }

   public EnumValueSetting add(final String enumValue, final String display, final int ringValue) {
      final int size = values.size();
      values.add(enumValue);
      valueToDisplay.put(enumValue, display);
      valueToIndex.put(enumValue, size);
      valueToRingValue.put(enumValue, ringValue);
      return this;
   }

   public String getDisplayValue(final String enumValue) {
      String dv = valueToDisplay.get(enumValue);
      if (dv == null) {
         dv = valueToDisplay.get(value.get());
      }
      return dv == null ? " " : dv;
   }

   public int toIndexed(final String value) {
      final Integer ringIndex = valueToRingValue.get(value);
      if (ringIndex != null) {
         return ringIndex;
      }
      return 0;
   }
}
