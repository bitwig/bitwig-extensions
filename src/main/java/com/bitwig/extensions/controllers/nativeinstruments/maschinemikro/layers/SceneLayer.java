package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.List;

@Component
public class SceneLayer extends Layer {
   private final RgbColor[] sceneColors = new RgbColor[16];
   private final SceneBank sceneBank;
   private final BooleanValueObject altHeld;
   private final BooleanValueObject deleteHeld;
   private final BooleanValueObject duplicateHeld;
   private final Application application;

   public SceneLayer(Layers layers, HwElements hwElements, ControllerHost host, ViewControl viewControl,
                     Transport transport, MidiProcessor midiProcessor, ModifierLayer modifierLayer,
                     Application application) {
      super(layers, "SCENE");
      this.altHeld = modifierLayer.getVariationHeld();
      this.deleteHeld = modifierLayer.getEraseHeld();
      this.duplicateHeld = modifierLayer.getDuplicateHeld();
      this.application = application;
      sceneBank = host.createSceneBank(16);
      List<RgbButton> padButtons = hwElements.getPadButtons();
      sceneBank.setIndication(true);

      for (int i = 0; i < 16; i++) {
         final int index = i;
         RgbButton button = padButtons.get(i);
         Scene scene = sceneBank.getScene(i);
         scene.exists().markInterested();
         scene.clipCount().markInterested();
         scene.color().addValueObserver((r, g, b) -> sceneColors[index] = RgbColor.toColor(r, g, b));
         button.bindPressed(this, () -> this.pressScene(scene));
         button.bindRelease(this, () -> this.releaseScene(scene));
         button.bindLight(this, () -> getRgbState(scene, index));
      }
   }

   private void releaseScene(Scene scene) {
      if (altHeld.get()) {
         scene.launchReleaseAlt();
      } else {
         scene.launchRelease();
      }
   }

   private void pressScene(Scene scene) {
      if (deleteHeld.get()) {
         scene.deleteObject();
      } else if (duplicateHeld.get()) {
         scene.replaceInsertionPoint().copySlotsOrScenes(scene);
      } else if (altHeld.get()) {
         scene.launchAlt();
      } else {
         scene.launch();
      }
   }

   private InternalHardwareLightState getRgbState(Scene scene, int sceneIndex) {
      if (scene.exists().get()) {
         if (scene.clipCount().get() == 0) {
            return sceneColors[sceneIndex].brightness(ColorBrightness.DARKENED);
         }
         return sceneColors[sceneIndex].brightness(ColorBrightness.BRIGHT);
      }
      return RgbColor.OFF;
   }
}
