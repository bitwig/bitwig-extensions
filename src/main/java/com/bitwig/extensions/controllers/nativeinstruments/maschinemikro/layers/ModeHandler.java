package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.CcButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.function.BooleanSupplier;

@Component
public class ModeHandler extends Layer {
   @Inject
   private SessionLayer sessionLayer;
   @Inject
   private ShiftPadLayer shiftPadLayer;
   @Inject
   private TrackLayer trackLayer;
   @Inject
   private SceneLayer sceneLayer;
   @Inject
   private EncoderLayer encoderLayer;
   @Inject
   private BrowserLayer browserLayer;
   @Inject
   private DeviceEncoderLayer deviceEncoderLayer;
   @Inject
   private StepEditor stepEditorLayer;
   
   private final PadLayer drumPadLayer;

   private Layer activeLayer;

   private enum Mode {
      LAUNCHER,
      SCENE,
      PADS(true),
      GROUP,
      PLUGIN,
      KEYS(true),
      STEP,
      NOTE_REPEAT(true);
      private boolean keyMode;

      Mode(boolean keyMode) {
         this.keyMode = keyMode;
      }

      Mode() {
         this.keyMode = false;
      }

      public boolean isKeyMode() {
         return keyMode;
      }

   }

   private Mode currentMode = Mode.LAUNCHER;
   private Mode stashedMode = currentMode;
   private EncoderMode encoderMode = EncoderMode.NONE;
   private MuteSoloMode muteSoloMode = MuteSoloMode.NONE;

   public ModeHandler(Layers layers, HwElements hwElements, PadLayer drumPadLayer, ModifierLayer modifierLayer) {
      super(layers, "SESSION_LAYER");
      this.drumPadLayer = drumPadLayer;
      bindModeButton(hwElements, CcAssignment.PATTERN, Mode.LAUNCHER);
      bindModeButton(hwElements, CcAssignment.SCENE, Mode.SCENE);
      bindModeButton(hwElements, CcAssignment.STEP, Mode.STEP);
      bindKeyModeButton(hwElements, CcAssignment.KEYBOARD, Mode.KEYS, () -> keyboardModeActive(drumPadLayer));
      bindKeyModeButton(hwElements, CcAssignment.PAD_MODE, Mode.PADS, () -> padModeActive(drumPadLayer));
      bindMomentaryModeButton(hwElements, CcAssignment.NOTE_REPEAT, Mode.NOTE_REPEAT);
      bindMomentaryModeButton(hwElements, CcAssignment.GROUP, Mode.GROUP);
      bindMomentaryModeButton(hwElements, CcAssignment.PLUGIN, Mode.PLUGIN);

      hwElements.getButton(CcAssignment.MUTE).bindPressed(this, () -> handleMutePress(true));
      hwElements.getButton(CcAssignment.MUTE).bindRelease(this, () -> handleMutePress(false));
      hwElements.getButton(CcAssignment.MUTE).bindLight(this, () -> muteSoloMode == MuteSoloMode.MUTE);

      hwElements.getButton(CcAssignment.SOLO).bindPressed(this, () -> handleSoloPress(true));
      hwElements.getButton(CcAssignment.SOLO).bindRelease(this, () -> handleSoloPress(false));
      hwElements.getButton(CcAssignment.SOLO).bindLight(this, () -> muteSoloMode == MuteSoloMode.SOLO);

      bindEncoderMode(hwElements, CcAssignment.VOLUME, EncoderMode.VOLUME);
      bindEncoderMode(hwElements, CcAssignment.SWING, EncoderMode.SWING);
      bindEncoderMode(hwElements, CcAssignment.TEMPO, EncoderMode.TEMPO);
      hwElements.getButton(CcAssignment.SEARCH).bindPressed(this, this::pressBrowser);
      hwElements.getButton(CcAssignment.SEARCH).bindLight(this, this::browsingActive);

      modifierLayer.getShiftHeld().addValueObserver(this::handleShiftPressed);
   }

   private boolean browsingActive() {
      return browserLayer.isActive();
   }

   private void pressBrowser() {
      browserLayer.handleBrowserActivation();
   }

   @Activate
   public void doActivateLayer() {
      activeLayer = sessionLayer;
      this.setIsActive(true);
      activeLayer.setIsActive(true);
   }

   private void handleMutePress(boolean press) {
      if (press) {
         if (muteSoloMode == MuteSoloMode.MUTE) {
            muteSoloMode = MuteSoloMode.NONE;
         } else {
            muteSoloMode = MuteSoloMode.MUTE;
         }
         drumPadLayer.setMutSoloMode(muteSoloMode);
         trackLayer.setMutSoloMode(muteSoloMode);
      }
   }

   private void handleSoloPress(boolean press) {
      if (press) {
         if (muteSoloMode == MuteSoloMode.SOLO) {
            muteSoloMode = MuteSoloMode.NONE;
         } else {
            muteSoloMode = MuteSoloMode.SOLO;
         }
         drumPadLayer.setMutSoloMode(muteSoloMode);
         trackLayer.setMutSoloMode(muteSoloMode);
      }
   }

   public void setEncoderLayer(EncoderLayer encoderLayer) {
      this.encoderLayer = encoderLayer;
   }

   private boolean keyboardModeActive(PadLayer drumPadLayer) {
      return !drumPadLayer.getInDrumMode().get() && currentMode.isKeyMode();
   }

   private boolean padModeActive(PadLayer drumPadLayer) {
      return drumPadLayer.getInDrumMode().get() && currentMode.isKeyMode();
   }

   private void handleShiftPressed(boolean shiftActive) {
      shiftPadLayer.setIsActive(shiftActive);
      if (activeLayer instanceof PadLayer padLayer) {
         padLayer.setNotesActive(!shiftActive);
      }
   }

   private void bindModeButton(HwElements hwElements, CcAssignment assignment, Mode mode) {
      hwElements.getButton(assignment).bindPressed(this, () -> pressMode(mode, false));
      hwElements.getButton(assignment).bindRelease(this, () -> releaseMode(mode));
      hwElements.getButton(assignment).bindLight(this, () -> currentMode == mode);
   }

   private void bindMomentaryModeButton(HwElements hwElements, CcAssignment assignment, Mode mode) {
      CcButton button = hwElements.getButton(assignment);
      button.bindIsPressedTimed(this, (pressed, downtime) -> handleMomentary(mode, pressed, downtime));
      button.bindLight(this, () -> currentMode == mode);
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

   private void handleMomentary(Mode mode, boolean pressed, long downtime) {
      if (pressed) {
         if (mode != currentMode) {
            stashedMode = currentMode;
            pressMode(mode, false);
         } else if (stashedMode != currentMode) {
            pressMode(stashedMode, true);
         }
      } else {
         if (stashedMode != currentMode && downtime > 300) {
            pressMode(stashedMode, true);
         }
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
         pressMode(newMode, false);
      } else if (activeLayer instanceof PadLayer padLayer) {
         if (newMode == Mode.PADS && !padLayer.getInDrumMode().get() //
            || newMode == Mode.KEYS && padLayer.getInDrumMode().get()) {
            padLayer.forceModeSwitch();
            currentMode = newMode;
         }
      }
   }

   private void pressMode(Mode newMode, boolean fromStash) {
      if (newMode != currentMode) {
         activeLayer.setIsActive(false);
         if (newMode == Mode.PADS || newMode == Mode.KEYS || newMode == Mode.NOTE_REPEAT) {
            activeLayer = drumPadLayer;
            drumPadLayer.enableNoteRepeat(newMode == Mode.NOTE_REPEAT);
         } else if (newMode == Mode.LAUNCHER) {
            activeLayer = sessionLayer;
         } else if (newMode == Mode.GROUP) {
            activeLayer = trackLayer;
         } else if (newMode == Mode.PLUGIN) {
            activeLayer = deviceEncoderLayer;
         } else if (newMode == Mode.SCENE) {
            activeLayer = sceneLayer;
         }else if (newMode == Mode.STEP) {
            activeLayer = stepEditorLayer;
         }
         currentMode = newMode;
         activeLayer.setIsActive(true);
      }
   }

   private void releaseMode(Mode launcher) {
   }


}
