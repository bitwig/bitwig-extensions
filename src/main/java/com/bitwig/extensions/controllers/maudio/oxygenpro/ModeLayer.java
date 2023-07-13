package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.definition.BasicMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.List;

@Component
public class ModeLayer extends Layer implements ModeHandler {

   private final SessionLayer sessionLayer;
   private final PadLayer padLayer;
   private Layer currentLayer;

   public ModeLayer(Layers layers, HwElements hwElements, SessionLayer sessionLayer, PadLayer padLayer) {
      super(layers, "MODE_LAYER");
      this.sessionLayer = sessionLayer;
      this.padLayer = padLayer;

      List<PadButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < gridButtons.size(); i++) {
         final int index = i;
         PadButton button = gridButtons.get(i);
         button.bindLight(this, () -> this.getRgbState(index));
         button.bindPressed(this, () -> this.handlePress(index));
      }
      sessionLayer.registerModeHandler(this);
      padLayer.registerModeHandler(this);
      currentLayer = sessionLayer;
   }

   @Override
   public void changeMode(BasicMode mode) {
      if (mode == BasicMode.NOTES) {
         handlePress(1);
      } else if (mode == BasicMode.CLIP_LAUNCH) {
         handlePress(0);
      }
   }

   private void handlePress(int index) {
      if (index == 0) {
         if (!sessionLayer.isActive()) {
            currentLayer.setIsActive(false);
            sessionLayer.setIsActive(true);
            currentLayer = sessionLayer;
         }
      } else if (index == 1) {
         if (!padLayer.isActive()) {
            currentLayer.setIsActive(false);
            padLayer.setIsActive(true);
            currentLayer = padLayer;
         }
      }
   }

   private InternalHardwareLightState getRgbState(int index) {
      if (index == 0) {
         return currentLayer == sessionLayer ? RgbColor.WHITE : RgbColor.BLUE;
      } else if (index == 1) {
         return currentLayer == padLayer ? RgbColor.WHITE : RgbColor.BLUE;
      }
      return RgbColor.OFF;
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      if (currentLayer == padLayer) {
         padLayer.setIsActive(false);
      }
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      if (currentLayer == padLayer) {
         padLayer.setIsActive(true);
      }
   }


}
