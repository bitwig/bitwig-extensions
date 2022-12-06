package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.framework.Layer;

public class LayerState {
   private Layer faderLayer;
   private Layer encoderLayer;
   private Layer buttonLayer;
   private DisplayLayer displayLayer;
   private DisplayLayer bottomDisplayLayer;

   LayerState(final LayerStateHandler layerStateHandler) {
      final LayerConfiguration initialConfig = layerStateHandler.getCurrentConfig();

      faderLayer = initialConfig.getFaderLayer();
      encoderLayer = initialConfig.getEncoderLayer();

      buttonLayer = layerStateHandler.getButtonLayer();

      displayLayer = layerStateHandler.getActiveDisplayLayer();
      bottomDisplayLayer = layerStateHandler.getBottomDisplayLayer();
   }

   public void init() {
      if (bottomDisplayLayer != null) {
         bottomDisplayLayer.focusOnBottom();
         bottomDisplayLayer.setIsActive(true);
      }
      faderLayer.setIsActive(true);
      encoderLayer.setIsActive(true);
      buttonLayer.setIsActive(true);
      displayLayer.setIsActive(true);
   }

   public void updateState(final LayerStateHandler statHandler) {
      updateState(statHandler, false);
   }

   public void updateState(final LayerStateHandler statHandler, final boolean forceDisplayUpdate) {
      final LayerConfiguration config = statHandler.getCurrentConfig();
      final DisplayLayer displayLayer = statHandler.getActiveDisplayLayer();
      final DisplayLayer newBottomDisplayLayer = statHandler.getBottomDisplayLayer();

      final Layer newFaderLayer = config.getFaderLayer();
      final Layer newEncoderLayer = config.getEncoderLayer();

      final Layer newButtonLayer = statHandler.getButtonLayer();
      if (!newFaderLayer.equals(faderLayer)) {
         faderLayer.setIsActive(false);
         faderLayer = newFaderLayer;
         faderLayer.setIsActive(true);
      }
      if (!newEncoderLayer.equals(encoderLayer)) {
         encoderLayer.setIsActive(false);
         encoderLayer = newEncoderLayer;
         encoderLayer.setIsActive(true);
      }
      if (!newButtonLayer.equals(buttonLayer)) {
         buttonLayer.setIsActive(false);
         buttonLayer = newButtonLayer;
         buttonLayer.setIsActive(true);
      }

      if (statHandler.hasBottomDisplay()) {
         updateDisplayState(displayLayer, newBottomDisplayLayer, forceDisplayUpdate);
      } else {
         updateDisplayState(displayLayer);
      }
   }

   public void updateDisplayState(final DisplayLayer newDisplayLayer) {
      if (!newDisplayLayer.equals(displayLayer)) {
         displayLayer.setIsActive(false);
         displayLayer = newDisplayLayer;
         displayLayer.setIsActive(true);
         displayLayer.focusOnTop();
      }
   }

   private void updateDisplayState(final DisplayLayer newDisplayLayer, final DisplayLayer newBottomDisplayLayer,
                                   final boolean forceDisplayUpdate) {
      if (newBottomDisplayLayer == null || bottomDisplayLayer == newBottomDisplayLayer) {
         updateDisplayState(newDisplayLayer);
      } else {
         final DisplayLayer bottomDeactivate = bottomDisplayLayer;
         final DisplayLayer topDeactivate = !newDisplayLayer.equals(displayLayer) ? displayLayer : null;
         displayLayer = newDisplayLayer;
         bottomDisplayLayer = newBottomDisplayLayer;
         if (bottomDeactivate != null) {
            bottomDeactivate.setIsActive(false);
         }
         if (topDeactivate != null) {
            topDeactivate.setIsActive(false);
         }
         displayLayer.focusOnTop();
         bottomDisplayLayer.focusOnBottom();
         displayLayer.setIsActive(true);
         bottomDisplayLayer.setIsActive(true);
      }
   }
}
