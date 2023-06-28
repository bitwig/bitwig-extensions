package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.SessionLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ModeHandler extends Layer {

   private enum Mode {
      LAUNCHER,
      SCENE,
      PADS,
      GROUP,
      KEYS;
   }

   private Mode currentMode = Mode.LAUNCHER;

   public ModeHandler(Layers layers, HwElements hwElements, SessionLayer sessionLayer) {
      super(layers, "SESSION_LAYER");
      bindModeButton(hwElements, CcAssignment.PATTERN, Mode.LAUNCHER);
      bindModeButton(hwElements, CcAssignment.SCENE, Mode.SCENE);
      bindModeButton(hwElements, CcAssignment.KEYBOARD, Mode.KEYS);
      bindModeButton(hwElements, CcAssignment.PAD_MODE, Mode.PADS);
      bindModeButton(hwElements, CcAssignment.GROUP, Mode.GROUP);
   }

   private void bindModeButton(HwElements hwElements, CcAssignment assignment, Mode mode) {
      hwElements.getButton(assignment).bindPressed(this, () -> pressMode(mode));
      hwElements.getButton(assignment).bindRelease(this, () -> releaseMode(mode));
      hwElements.getButton(assignment).bindLight(this, () -> currentMode == mode);
   }

   private void pressMode(Mode newMode) {
      if (newMode != currentMode) {
         currentMode = newMode;
      }
   }

   private void releaseMode(Mode launcher) {
   }


   @Activate
   public void activateLayer() {
      this.setIsActive(true);
   }
}
