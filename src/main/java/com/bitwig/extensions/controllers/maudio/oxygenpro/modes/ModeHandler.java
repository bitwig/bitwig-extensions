package com.bitwig.extensions.controllers.maudio.oxygenpro.modes;

import com.bitwig.extensions.controllers.maudio.oxygenpro.HwElements;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxygenCcAssignments;
import com.bitwig.extensions.controllers.maudio.oxygenpro.definition.BasicMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class ModeHandler extends Layer {

   private final SessionLayer sessionLayer;
   private final PadLayer padLayer;
   private Layer currentLayer;

   @Inject
   private DeviceControlSelectLayer deviceControlSelectLayer;

   public ModeHandler(Layers layers, HwElements hwElements, SessionLayer sessionLayer, PadLayer padLayer) {
      super(layers, "DEVICE_CONTROL_LAYER");
      this.sessionLayer = sessionLayer;
      this.padLayer = padLayer;

      sessionLayer.registerModeHandler(this);
      padLayer.registerModeHandler(this);
      currentLayer = sessionLayer;
      hwElements.getButton(OxygenCcAssignments.BACK).bindIsPressed(this, this::handleBackButton);
   }

   @Activate
   public void onActivation() {
      this.setIsActive(true);
   }

   private void handleBackButton(boolean isPressed) {
      if (currentLayer == padLayer) {
         padLayer.setBackButtonHeld(isPressed);
         padLayer.setIsActive(!isPressed);
         deviceControlSelectLayer.setIsActive(isPressed);
      } else {
         sessionLayer.setBackButtonHeld(isPressed);
      }
   }

   public void changeMode(BasicMode mode) {
      if (mode == BasicMode.NOTES) {
         if (!padLayer.isActive()) {
            currentLayer.setIsActive(false);
            padLayer.setIsActive(true);
            currentLayer = padLayer;
         }
      } else if (mode == BasicMode.CLIP_LAUNCH) {
         if (!sessionLayer.isActive()) {
            currentLayer.setIsActive(false);
            sessionLayer.setIsActive(true);
            currentLayer = sessionLayer;
         }
      }
   }
}
