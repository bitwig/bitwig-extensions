package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

import java.util.function.BooleanSupplier;

@Component
public class ModeHandler extends Layer {

   private final SessionLayer sessionLayer;
   private final PadLayer drumPadLayer;
   private final ShiftPadLayer shiftPadLayer;
   private final TrackLayer trackLayer;
   private Layer activeLayer;

   private enum Mode {
      LAUNCHER,
      SCENE,
      PADS(true),
      GROUP,
      KEYS(true);
      private boolean keyMode;

      private Mode(boolean keyMode) {
         this.keyMode = keyMode;
      }

      private Mode() {
         this.keyMode = false;
      }

      public boolean isKeyMode() {
         return keyMode;
      }
   }

   private Mode currentMode = Mode.LAUNCHER;
   private Mode stashedMode = currentMode;

   public ModeHandler(Layers layers, HwElements hwElements, SessionLayer sessionLayer, PadLayer drumPadLayer,
                      ShiftPadLayer shiftPadLayer, TrackLayer trackLayer, ModifierLayer modifierLayer) {
      super(layers, "SESSION_LAYER");
      this.sessionLayer = sessionLayer;
      this.drumPadLayer = drumPadLayer;
      this.shiftPadLayer = shiftPadLayer;
      this.trackLayer = trackLayer;
      bindModeButton(hwElements, CcAssignment.PATTERN, Mode.LAUNCHER);
      bindModeButton(hwElements, CcAssignment.SCENE, Mode.SCENE);
      bindKeyModeButton(hwElements, CcAssignment.KEYBOARD, Mode.KEYS,
         () -> currentMode == Mode.PADS && !drumPadLayer.getInDrumMode().get());
      bindKeyModeButton(hwElements, CcAssignment.PAD_MODE, Mode.PADS,
         () -> currentMode == Mode.PADS && drumPadLayer.getInDrumMode().get());
      bindMomentaryModeButton(hwElements, CcAssignment.GROUP, Mode.GROUP);
      modifierLayer.getShiftHeld().addValueObserver(this::handleShiftPressed);
      activeLayer = sessionLayer;
   }

   private void handleShiftPressed(boolean shiftActive) {
      shiftPadLayer.setIsActive(shiftActive);
      if (activeLayer instanceof PadLayer padLayer) {
         padLayer.setNotesActive(!shiftActive);
      }
   }

   private void bindModeButton(HwElements hwElements, CcAssignment assignment, Mode mode) {
      hwElements.getButton(assignment).bindPressed(this, () -> pressMode(mode));
      hwElements.getButton(assignment).bindRelease(this, () -> releaseMode(mode));
      hwElements.getButton(assignment).bindLight(this, () -> currentMode == mode);
   }

   private void bindMomentaryModeButton(HwElements hwElements, CcAssignment assignment, Mode mode) {
      hwElements.getButton(assignment).bindPressed(this, () -> pressModeMomentary(mode));
      hwElements.getButton(assignment).bindRelease(this, () -> releaseModeMomentary(mode));
      hwElements.getButton(assignment).bindLight(this, () -> currentMode == mode);
   }

   private void pressModeMomentary(Mode mode) {
      if (mode != currentMode) {
         stashedMode = currentMode;
         pressMode(mode);
      }
   }

   private void releaseModeMomentary(Mode mode) {
      if (stashedMode != currentMode) {
         pressMode(stashedMode);
      }
   }

   private void bindKeyModeButton(HwElements hwElements, CcAssignment assignment, Mode mode,
                                  BooleanSupplier lightSupplier) {
      hwElements.getButton(assignment).bindPressed(this, () -> pressKeyMode(mode));
      hwElements.getButton(assignment).bindRelease(this, () -> releaseMode(mode));
      hwElements.getButton(assignment).bindLight(this, lightSupplier);
   }

   private void pressKeyMode(Mode newMode) {
      if (!currentMode.isKeyMode() && newMode.isKeyMode()) {
         pressMode(newMode);
      } else if (activeLayer instanceof PadLayer padLayer) {
         if (newMode == Mode.PADS && !padLayer.getInDrumMode().get() //
            || newMode == Mode.KEYS && padLayer.getInDrumMode().get()) {
            padLayer.forceModeSwitch();
            currentMode = newMode;
         }
      }
   }

   private void pressMode(Mode newMode) {
      if (newMode != currentMode) {
         activeLayer.setIsActive(false);
         if (newMode == Mode.PADS || newMode == Mode.KEYS) {
            activeLayer = drumPadLayer;
         } else if (newMode == Mode.LAUNCHER) {
            activeLayer = sessionLayer;
         } else if (newMode == Mode.GROUP) {
            activeLayer = trackLayer;
         }
         currentMode = newMode;
         activeLayer.setIsActive(true);
      }
   }

   private void releaseMode(Mode launcher) {
   }


   @Activate
   public void activateLayer() {
      this.setIsActive(true);
      activeLayer.setIsActive(true);
   }
}
