package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModifierValueObject {

   public static final int SHIFT = 0x1;
   public static final int OPTION = 0x2;
   public static final int CONTROL = 0x4;
   public static final int ALT = 0x8;

   private int value = 0;
   private final List<Consumer<ModifierValueObject>> callbacks = new ArrayList<>(); // TODO send it self is better
   private final List<Consumer<Boolean>> shiftCallbacks = new ArrayList<>();
   private final List<Consumer<Boolean>> optionCallbacks = new ArrayList<>();
   private boolean useClearDuplicateModifiers;

   public void addValueObserver(final Consumer<ModifierValueObject> callback) {
      callbacks.add(callback);
   }

   public void addShiftValueObserver(final Consumer<Boolean> callback) {
      shiftCallbacks.add(callback);
   }

   public void addOptionValueObserver(final Consumer<Boolean> callback) {
      optionCallbacks.add(callback);
   }

   public int get() {
      return value;
   }

   public void setUsesDuplicateClear(final boolean useClearDuplicateModifiers) {
      this.useClearDuplicateModifiers = useClearDuplicateModifiers;
   }

   public boolean notSet() {
      return value == 0;
   }

   private void notifyValueChanged() {
      callbacks.forEach(callback -> callback.accept(this));
   }

   private void notifyShiftChanged(final boolean newShiftValue) {
      shiftCallbacks.forEach(callback -> callback.accept(newShiftValue));
   }

   private void notifyOptionChanged(final boolean newOptionValue) {
      optionCallbacks.forEach(callback -> callback.accept(newOptionValue));
   }

   public boolean isSet(final int value) {
      return this.value == value;
   }

   public boolean isSet(final int... values) {
      for (final int v : values) {
         if ((v & value) == 0) {
            return false;
         }
      }
      return true;
   }

   /**
    * @return if the shift button is held down
    */
   public boolean isShiftSet() {
      return (value & SHIFT) != 0;
   }

   /**
    * @return if exactly the shift button is held down
    */
   public boolean isShift() {
      return value == SHIFT;
   }

   public void setShift(final boolean shift) {
      if (shift && !isShiftSet()) {
         value |= SHIFT;
         notifyValueChanged();
         notifyShiftChanged(true);
      } else if (!shift && isShiftSet()) {
         value &= ~SHIFT;
         notifyValueChanged();
         notifyShiftChanged(false);
      }
   }

   /**
    * @return if the option button is held down
    */
   public boolean isOptionSet() {
      return (value & OPTION) != 0;
   }

   /**
    * @return if exactly the option button is held down
    */
   public boolean isOption() {
      return value == OPTION;
   }

   public void setOption(final boolean option) {
      if (option && !isOptionSet()) {
         value |= OPTION;
         notifyValueChanged();
         notifyOptionChanged(true);
      } else if (!option && isOptionSet()) {
         value &= ~OPTION;
         notifyValueChanged();
         notifyOptionChanged(false);
      }
   }

   /**
    * @return if the control button is held down
    */
   public boolean isControlSet() {
      if (useClearDuplicateModifiers) {
         return false;
      }
      return (value & CONTROL) != 0;
   }

   /**
    * @return if exactly only the control button is held down
    */
   public boolean isControl() {
      if (useClearDuplicateModifiers) {
         return false;
      }
      return value == CONTROL;
   }

   public void setControl(final boolean control) {
      if (control && (value & CONTROL) == 0) {
         value |= CONTROL;
         notifyValueChanged();
      } else if (!control && (value & CONTROL) != 0) {
         value &= ~CONTROL;
         notifyValueChanged();
      }
   }

   public boolean isAltSet() {
      if (useClearDuplicateModifiers) {
         return false;
      }
      return (value & ALT) != 0;
   }

   public boolean isDuplicateSet() {
      return (value & ALT) != 0;
   }

   public boolean isClearSet() {
      return (value & CONTROL) != 0;
   }

   public boolean isAlt() {
      if (useClearDuplicateModifiers) {
         return false;
      }
      return value == ALT;
   }

   public void setAlt(final boolean alt) {
      if (alt && (value & ALT) == 0) {
         value |= ALT;
         notifyValueChanged();
      } else if (!alt && (value & ALT) != 0) {
         value &= ~ALT;
         notifyValueChanged();
      }
   }

}
