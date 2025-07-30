package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

import java.util.Objects;

public class RgbColorState extends InternalHardwareLightState {

   public static final RgbColorState RED_FULL = new RgbColorState(RgbColor.RED);
   public static final RgbColorState RED_DIM = new RgbColorState(RgbColor.RED_DIM);
   public static final RgbColorState RED_ORANGE_FULL = new RgbColorState(RgbColor.RED_ORANGE);
   public static final RgbColorState RED_ORANGE_DIM = new RgbColorState(RgbColor.RED_ORANGE_DIM);
   public static final RgbColorState GREEN_FULL = new RgbColorState(RgbColor.GREEN);
   public static final RgbColorState GREEN_DIM = new RgbColorState(RgbColor.GREEN_DIM);

   private final RgbColor color;

   private RgbColorState(final RgbColor color) {
      this.color = color;
   }

   public RgbColor getColor() {
      return color;
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return null;
   }

   @Override
   public boolean equals(final Object o) {
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final RgbColorState that = (RgbColorState) o;
      return Objects.equals(color, that.color);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(color);
   }
}
