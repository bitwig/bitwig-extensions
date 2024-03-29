package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.CcButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Component
public class ModeHandler extends Layer {
   private final ModifierLayer modifierLayer;
   private final Layer noteRepeatLayer;
   @Inject
   private SessionLayer sessionLayer;
   @Inject
   private ShiftPadLayer shiftPadLayer;
   @Inject
   private TrackLayer trackLayer;
   @Inject
   private SceneLayer sceneLayer;
   @Inject
   private BrowserLayer browserLayer;
   @Inject
   private DeviceEncoderLayer deviceEncoderLayer;
   @Inject
   private StepEditor stepEditorLayer;
   @Inject
   private GridLayer gridLayer;
   @Inject
   private ChordLayer chordLayer;

   private EncoderLayer encoderLayer;

   private final PadLayer drumPadLayer;

   private Layer activeLayer;

   private Map<Mode, Runnable> modeAction = new HashMap<>();

   private enum Mode {
      NONE,
      LAUNCHER,
      SCENE,
      PADS(true),
      GROUP,
      PLUGIN,
      KEYS(true),
      STEP,
      NOTE_REPEAT(true),
      GRID,
      CHORD;
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

      public boolean isKeyboardPlayable() {
         return keyMode || this == CHORD;
      }
   }

   private Mode currentMode = Mode.LAUNCHER;
   private Mode stashedMode = Mode.NONE;
   private Layer stashedLayer = null;
   private EncoderMode encoderMode = EncoderMode.NONE;
   private MuteSoloMode muteSoloMode = MuteSoloMode.NONE;
   private Map<Mode, Layer> layerModeRegister = new HashMap<>();

   public ModeHandler(Layers layers, HwElements hwElements, PadLayer drumPadLayer, ModifierLayer modifierLayer,
                      MidiProcessor midiProcessor) {
      super(layers, "SESSION_LAYER");
      this.drumPadLayer = drumPadLayer;
      this.modifierLayer = modifierLayer;
      this.noteRepeatLayer = new Layer(layers, "NOTE_REPEAT_LAYER");
      Layer subLayer = layers.getLayers()
         .stream()
         .filter(l -> l.getName().equals("LOW_PRIORITY_LAYER"))
         .findFirst()
         .orElse(this);

      bindModeButton(hwElements, CcAssignment.PATTERN, Mode.LAUNCHER);
      bindMomentaryModeButton(hwElements, CcAssignment.SCENE, Mode.SCENE);
      bindModeButton(hwElements, CcAssignment.STEP, Mode.STEP);
      bindModeButton(hwElements, CcAssignment.CHORD, Mode.CHORD);
      bindKeyModeButton(hwElements, CcAssignment.KEYBOARD, Mode.KEYS, () -> keyboardModeActive(drumPadLayer));
      bindKeyModeButton(hwElements, CcAssignment.PAD_MODE, Mode.PADS, () -> padModeActive(drumPadLayer));

      CcButton button = hwElements.getButton(CcAssignment.NOTE_REPEAT);
      button.bindIsPressedTimed(this, this::handleNoteRepeat);
      button.bindLight(this, () -> drumPadLayer.isNoteRepeatActive());

      bindMomentaryModeButton(hwElements, CcAssignment.GROUP, Mode.GROUP);
      bindMomentaryModeButton(hwElements, CcAssignment.PLUGIN, Mode.PLUGIN);
      bindMomentaryModeButton(hwElements, CcAssignment.FOLLOW, Mode.GRID);
      modeAction.put(Mode.PLUGIN, this::exitEncoderModes);

      hwElements.getButton(CcAssignment.MUTE).bindIsPressed(subLayer, this::handleMuteArmPress);
      hwElements.getButton(CcAssignment.MUTE)
         .bindLight(subLayer,
            () -> muteSoloMode == MuteSoloMode.ARM ? midiProcessor.blinkMid() : muteSoloMode == MuteSoloMode.MUTE);

      hwElements.getButton(CcAssignment.SOLO).bindIsPressed(subLayer, this::handleSoloPress);
      hwElements.getButton(CcAssignment.SOLO)
         .bindLight(subLayer,
            () -> muteSoloMode == MuteSoloMode.SOLO_EXCLUSIVE ? midiProcessor.blinkMid() : muteSoloMode == MuteSoloMode.SOLO);

      bindEncoderMode(hwElements, CcAssignment.VOLUME, EncoderMode.VOLUME);
      bindEncoderMode(hwElements, CcAssignment.SWING, EncoderMode.SWING);
      bindEncoderMode(hwElements, CcAssignment.TEMPO, EncoderMode.TEMPO);
      hwElements.getButton(CcAssignment.SEARCH).bindPressed(this, this::pressBrowser);
      hwElements.getButton(CcAssignment.SEARCH).bindLight(this, this::browsingActive);

      modifierLayer.getShiftHeld().addValueObserver(this::handleShiftPressed);
      modifierLayer.getSelectHeld().addValueObserver(this::handleSelectPressed);

      hwElements.bindEncoder(noteRepeatLayer, hwElements.getMainEncoder(), dir -> handleArpEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindIsPressed(noteRepeatLayer, this::handleArpEncoderPress);
   }

   public void handleArpEncoder(int dir) {
      drumPadLayer.modifyArpRate(dir);
      nrValueChanged = true;
   }

   public void handleArpEncoderPress(boolean pressed) {
      // Do nothing now
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
      layerModeRegister.put(Mode.GROUP, trackLayer);
      layerModeRegister.put(Mode.LAUNCHER, sessionLayer);
      layerModeRegister.put(Mode.CHORD, chordLayer);
      layerModeRegister.put(Mode.SCENE, sceneLayer);
      layerModeRegister.put(Mode.STEP, stepEditorLayer);
      layerModeRegister.put(Mode.PLUGIN, deviceEncoderLayer);
      layerModeRegister.put(Mode.GRID, gridLayer);
      this.setIsActive(true);
      activeLayer.setIsActive(true);
   }

   public void exitEncoderModes() {
   }

   private void handleMuteArmPress(boolean press) {
      if (!press) {
         return;
      }
      if (modifierLayer.getShiftHeld().get()) {
         handleArmPress(true);
      } else {
         handleMutePress(true);
      }
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
      if (!press) {
         return;
      }
      if (modifierLayer.getShiftHeld().get()) {
         if (muteSoloMode == MuteSoloMode.SOLO_EXCLUSIVE) {
            muteSoloMode = MuteSoloMode.SOLO;
         } else {
            muteSoloMode = MuteSoloMode.SOLO_EXCLUSIVE;
         }
      } else {
         if (muteSoloMode == MuteSoloMode.SOLO || muteSoloMode == MuteSoloMode.SOLO_EXCLUSIVE) {
            muteSoloMode = MuteSoloMode.NONE;
         } else {
            muteSoloMode = MuteSoloMode.SOLO;
         }
      }
      drumPadLayer.setMutSoloMode(muteSoloMode);
      trackLayer.setMutSoloMode(muteSoloMode);
   }

   private void handleArmPress(boolean press) {
      if (press) {
         if (muteSoloMode == MuteSoloMode.ARM) {
            muteSoloMode = MuteSoloMode.MUTE;
         } else {
            muteSoloMode = MuteSoloMode.ARM;
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

   private void handleSelectPressed(boolean pressed) {
      if (activeLayer instanceof StepEditor stepEditor) {
         if (pressed) {
            stashedMode = currentMode;
            pressMode(Mode.PADS, false);
            drumPadLayer.setSelectMode(true);
         }
      } else if (activeLayer instanceof PadLayer padLayer) {
         if (!pressed && stashedMode == Mode.STEP) {
            padLayer.setSelectMode(false);
            pressMode(Mode.STEP, true);
            stashedMode = Mode.PADS;
         }
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

      if (encoderMode != EncoderMode.NONE) {
         deviceEncoderLayer.deactivateTouch();
         trackLayer.deactivateTouch();
      }
   }

   private boolean nrActivatedOnPress = false;
   private boolean nrValueChanged = false;

   private void handleNoteRepeat(boolean pressed, long downtime) {
      boolean nrActive = drumPadLayer.isNoteRepeatActive();
      if (pressed) {
         if (!currentMode.isKeyboardPlayable()) {
            stashedMode = currentMode;
            pressMode(Mode.PADS, false);
            drumPadLayer.enableNoteRepeat(true);
            nrActivatedOnPress = true;
         } else if (!nrActive) {
            drumPadLayer.enableNoteRepeat(true);
            nrActivatedOnPress = true;
         }
      } else {
         if (currentMode.isKeyboardPlayable()) {
            if (stashedMode != currentMode && stashedMode != Mode.NONE) {
               if (downtime > 300) {
                  pressMode(stashedMode, true);
                  stashedMode = Mode.NONE;
                  drumPadLayer.enableNoteRepeat(false);
               } else {
                  stashedMode = Mode.NONE;
               }
            } else if (downtime <= 300) {
               if (!nrActivatedOnPress) {
                  drumPadLayer.enableNoteRepeat(false);
               }
            } else if (!nrValueChanged) {
               drumPadLayer.enableNoteRepeat(false);
            }
         } else if (!nrActive) {
            drumPadLayer.enableNoteRepeat(false);
         }
         nrActivatedOnPress = false;
      }
      noteRepeatLayer.setIsActive(pressed);
      nrValueChanged = false;
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
         if (newMode == Mode.PADS || newMode == Mode.KEYS) {
            activeLayer = drumPadLayer;
         } else {
            Layer nextLayer = layerModeRegister.get(newMode);
            if (nextLayer != null) {
               activeLayer = nextLayer;
            }
         }
         currentMode = newMode;
         Runnable afterSetAction = modeAction.get(currentMode);
         if (afterSetAction != null) {
            afterSetAction.run();
         }
         activeLayer.setIsActive(true);
      }
   }

   private void releaseMode(Mode launcher) {
   }


}
