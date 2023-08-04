package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

class TouchStripLightState extends InternalHardwareLightState {

   private static final TouchStripLightState[] STATES = new TouchStripLightState[128];

   private final int value;

   static {
      for (int i = 0; i < STATES.length; i++) {
         STATES[i] = new TouchStripLightState(i);
      }
   }

   public static TouchStripLightState of(int intVal) {
      return STATES[intVal];
   }

   private TouchStripLightState(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return null;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof TouchStripLightState state) {
         return value == state.value;
      }
      return false;
   }
}
