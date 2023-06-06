package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchPadButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.List;

public class SceneControl {
   private final SessionLayer sessionLayer;
   private final Layer sceneControlLayer;

   SceneControl(SessionLayer sessionLayer, Layers layers) {
      sceneControlLayer = new Layer(layers, "SCENE_CONTROL");
      this.sessionLayer = sessionLayer;
   }

   public Layer getLayer() {
      return sceneControlLayer;
   }

   void initSceneControl(final SceneBank sceneBank, List<LaunchPadButton> buttons) {
      sceneBank.setIndication(true);
      for (int i = 0; i < buttons.size(); i++) {
         final int index = i;
         final Scene scene = sceneBank.getScene(index);
         LaunchPadButton sceneButton = buttons.get(index);
         scene.clipCount().markInterested();
         sceneButton.bindPressed(sceneControlLayer, pressed -> sessionLayer.handleScene(pressed, scene, index));
         if (sceneButton instanceof LabeledButton) {
            sceneButton.bindLight(sceneControlLayer, () -> sessionLayer.getSceneColorVertical(index, scene));
         } else {
            sceneButton.bindLight(sceneControlLayer, () -> sessionLayer.getSceneColorHorizontal(index, scene));
         }
      }
   }

   public void setActive(boolean active) {
      sceneControlLayer.setIsActive(active);
   }
}
