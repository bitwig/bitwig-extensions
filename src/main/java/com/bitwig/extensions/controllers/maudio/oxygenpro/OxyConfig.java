package com.bitwig.extensions.controllers.maudio.oxygenpro;

public class OxyConfig {
   private final boolean hasMasterSlider;
   private int numberOfControls;
   private boolean hasSlider;

   public OxyConfig(int numberOfControls, boolean hasSliders, boolean hasMasterSlider) {
      this.numberOfControls = numberOfControls;
      this.hasSlider = hasSliders;
      this.hasMasterSlider = hasMasterSlider;
   }

   public int getNumberOfControls() {
      return numberOfControls;
   }

   public boolean hasMasterSlider() {
      return hasMasterSlider;
   }

   public boolean hasSliders() {
      return hasSlider;
   }

   public boolean hasSingleMasterSlider() {
      return !hasSlider && hasMasterSlider;
   }
}
