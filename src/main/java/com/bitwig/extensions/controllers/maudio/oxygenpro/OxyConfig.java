package com.bitwig.extensions.controllers.maudio.oxygenpro;

public class OxyConfig {
   private final boolean hasMasterSlider;
   private int numberOfControls;
   private boolean hasSlider;

   public OxyConfig(int numberOfControls, boolean hasSlider, boolean hasMasterSlider) {
      this.numberOfControls = numberOfControls;
      this.hasSlider = hasSlider;
      this.hasMasterSlider = hasMasterSlider;
   }

   public int getNumberOfControls() {
      return numberOfControls;
   }

   public boolean hasMasterSlider() {
      return hasMasterSlider;
   }

   public boolean hasSlider() {
      return hasSlider;
   }
}
