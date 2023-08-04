package com.bitwig.extensions.controllers.maudio.oxygenpro;

public class OxyConfig {
   private final boolean hasMasterSlider;
   private int numberOfControls;
   private boolean hasSlider;
   private boolean hasSceneLaunchButtons;

   public OxyConfig(int numberOfControls, boolean hasSliders, boolean hasMasterSlider,  boolean hasSceneLaunchButtons) {
      this.numberOfControls = numberOfControls;
      this.hasSlider = hasSliders;
      this.hasMasterSlider = hasMasterSlider;
      this.hasSceneLaunchButtons = hasSceneLaunchButtons;
   }

   public int getNumberOfControls() {
      return numberOfControls;
   }

   public boolean hasMasterSlider() {
      return hasMasterSlider;
   }
   
   public boolean hasSceneLaunchButtons() {
      return hasSceneLaunchButtons;
   }
   
   public boolean hasSingleMasterSlider() {
      return !hasSlider && hasMasterSlider;
   }
}
