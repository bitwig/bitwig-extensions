package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.DebugOutMk;
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
   private EncoderLayer encoderLayer;
   private Layer activeLayer;

   private enum Mode {
      LAUNCHER,
      SCENE,
      PADS(true),
      GROUP,
      KEYS(true),
      NOTE_REPEAT(true);
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
   private EncoderMode encoderMode = EncoderMode.NONE;

   public ModeHandler(Layers layers, HwElements hwElements, SessionLayer sessionLayer, PadLayer drumPadLayer,
                      ShiftPadLayer shiftPadLayer, TrackLayer trackLayer, ModifierLayer modifierLayer) {
      super(layers, "SESSION_LAYER");
      this.sessionLayer = sessionLayer;
      this.drumPadLayer = drumPadLayer;
      this.shiftPadLayer = shiftPadLayer;
      this.trackLayer = trackLayer;
      bindModeButton(hwElements, CcAssignment.PATTERN, Mode.LAUNCHER);
      bindModeButton(hwElements, CcAssignment.SCENE, Mode.SCENE);
      bindKeyModeButton(hwElements, CcAssignment.KEYBOARD, Mode.KEYS, () -> keyboardModeActive(drumPadLayer));
      bindKeyModeButton(hwElements, CcAssignment.PAD_MODE, Mode.PADS, () -> padModeActive(drumPadLayer));
      bindMomentaryModeButton(hwElements, CcAssignment.NOTE_REPEAT, Mode.NOTE_REPEAT);
      bindMomentaryModeButton(hwElements, CcAssignment.GROUP, Mode.GROUP);

      bindEncoderMode(hwElements, CcAssignment.VOLUME, EncoderMode.VOLUME);
      bindEncoderMode(hwElements, CcAssignment.SWING, EncoderMode.SWING);
      bindEncoderMode(hwElements, CcAssignment.TEMPO, EncoderMode.TEMPO);
      modifierLayer.getShiftHeld().addValueObserver(this::handleShiftPressed);
      activeLayer = sessionLayer;
   }

   public void setEncoderLayer(EncoderLayer encoderLayer) {
      DebugOutMk.println(" Setting Encoder Layer");
      this.encoderLayer = encoderLayer;
   }

   private boolean keyboardModeActive(PadLayer drumPadLayer) {
      return (currentMode == Mode.PADS || currentMode == Mode.NOTE_REPEAT) && !drumPadLayer.getInDrumMode().get();
   }

   private boolean padModeActive(PadLayer drumPadLayer) {
      return (currentMode == Mode.PADS || currentMode == Mode.NOTE_REPEAT) && drumPadLayer.getInDrumMode().get();
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

   private void bindEncoderMode(HwElements hwElements, CcAssignment assignment, EncoderMode mode) {
      hwElements.getButton(assignment).bindPressed(this, () -> pressEncoderMode(mode));
      hwElements.getButton(assignment).bindRelease(this, () -> releaseEncoderMode(mode));
      hwElements.getButton(assignment).bindLight(this, () -> encoderMode == mode);
   }

   private void releaseEncoderMode(EncoderMode mode) {
   }

   private void pressEncoderMode(EncoderMode mode) {
      if (encoderMode == EncoderMode.NONE) {
         encoderMode = mode;
      } else {
         if (mode == encoderMode) {
            encoderMode = EncoderMode.NONE;
         } else {
            encoderMode = mode;
         }
      }
      encoderLayer.setMode(encoderMode);
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
         if (newMode == Mode.PADS || newMode == Mode.KEYS || newMode == Mode.NOTE_REPEAT) {
            activeLayer = drumPadLayer;
            drumPadLayer.enableNoteRepeat(newMode == Mode.NOTE_REPEAT);
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
