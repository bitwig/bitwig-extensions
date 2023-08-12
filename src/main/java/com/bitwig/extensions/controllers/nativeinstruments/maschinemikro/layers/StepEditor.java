package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.StepViewPosition;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.TouchStrip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.Arrays;
import java.util.List;

@Component
public class StepEditor extends Layer {

   private final double[] rateTable = {0.0833333, 0.125, 0.1666666, 0.25, 0.33333, 0.5, 0.666666, 1.0};

   //private final double[] rateTable = {0.125, 0.25, 0.5, 1.0, 2.0};
   private final String[] rateDisplayValues = {"1/32T", "1/32", "1/16T", "1/16", "1/8T", "1/8", "1/4T", "1/4"};

   private final Clip clip;
   private final StepViewPosition positionHandler; // TODO Move this to commons
   private final TouchStripLayer touchStripLayer;
   private final SettableEnumValue gridResolution;
   private RgbColor clipColor;
   private int focusNote = 60;
   private final NoteStep[] assignments = new NoteStep[16];
   private final PressState[] pressStates = new PressState[16];
   private final long[] downTimes = new long[16];
   private int playingStep = -1;
   private boolean isDrumEdit = false;
   private boolean shiftDown = false;

   private PadLayer padLayer;
   private int selectedPadIndex = -1;
   private Layer stripCurrentLayer;
   private StripMode stripMode = StripMode.NONE;

   private double currentRandomValue = 1.0;
   private int currentRepeat = 0;
   private int currentStepLen = 1;
   private boolean eraseDown;
   private int currentGridRate = 1;
   private double stepLength = 0.95;

   private enum PressState {
      None,
      New,
      Modify,
      Delete;
   }

   public StepEditor(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                     MidiProcessor midiProcessor, ControllerHost host, TouchStripLayer touchStripLayer) {
      super(layers, "PAD_LAYER");
      this.touchStripLayer = touchStripLayer;
      List<RgbButton> padButtons = hwElements.getPadButtons();
      Arrays.fill(pressStates, PressState.None);
      Arrays.fill(downTimes, -1);
      this.clip = host.createLauncherCursorClip(16, 1);
      this.positionHandler = new StepViewPosition(this.clip);
      this.clip.color().addValueObserver((r, g, b) -> {
         clipColor = RgbColor.toColor(r, g, b);
      });
      viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
         isDrumEdit = hasDrumPads;
      });
      //modifierLayer.getSelectHeld().addValueObserver(this::setSelectMode);
      modifierLayer.getDuplicateHeld().addValueObserver(this::handleDuplicate);
      modifierLayer.getShiftHeld().addValueObserver(this::handleShift);
      modifierLayer.getEraseHeld().addValueObserver(this::handleErase);

      for (int i = 0; i < 16; i++) {
         final int index = (3 - i / 4) * 4 + i % 4;
         RgbButton button = padButtons.get(i);
         button.bindPressed(this, () -> this.pressStep(index));
         button.bindRelease(this, () -> this.releaseStep(index));
         button.bindLight(this, () -> getRgbState(index));
      }
      clip.addNoteStepObserver(this::handleNoteStep);
      clip.playingStep().addValueObserver(this::handlePlayingStep);

      this.clip.scrollToKey(focusNote);

      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindPressed(this, () -> handleEncoderPress(true));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindRelease(this, () -> handleEncoderPress(false));
      hwElements.getButton(CcAssignment.ENCODER_TOUCH).bindPressed(this, () -> handleEncoderTouch(true));
      hwElements.getButton(CcAssignment.ENCODER_TOUCH).bindRelease(this, () -> handleEncoderTouch(false));

      initTouchStripEditing(touchStripLayer, hwElements);
      DocumentState documentState = host.getDocumentState();
      gridResolution = documentState.getEnumSetting("Step Grid Resolution", //
         "Step Grid resolution", rateDisplayValues, rateDisplayValues[currentGridRate]);
      gridResolution.addValueObserver(mode -> this.handleArpModeChanged(mode));
   }

   private void handleArpModeChanged(final String value) {
      int index = find(rateDisplayValues, value);
      if (index != -1) {
         currentGridRate = index;
         positionHandler.setGridResolution(rateTable[currentGridRate]);
      }
   }

   private int find(String[] array, String value) {
      for (int i = 0; i < array.length; i++) {
         if (array[i].equals(value)) {
            return i;
         }
      }
      return -1;
   }

   public void setResolutionIndex(int index) {
      if (index < rateTable.length) {
         currentGridRate = index;
         gridResolution.set(rateDisplayValues[index]);
      }
   }

   public int getResolutionIndex() {
      return currentGridRate;
   }

   private void initTouchStripEditing(TouchStripLayer touchStripLayer, HwElements hwElements) {
      TouchStrip touchStrip = hwElements.getTouchStrip();

      hwElements.getButton(CcAssignment.PITCH).bindPressed(this, () -> selectMode(StripMode.PITCH));
      hwElements.getButton(CcAssignment.PITCH).bindLight(this, () -> stripMode == StripMode.PITCH);
      hwElements.getButton(CcAssignment.MOD).bindPressed(this, () -> selectMode(StripMode.MOD));
      hwElements.getButton(CcAssignment.MOD).bindLight(this, () -> stripMode == StripMode.MOD);
      hwElements.getButton(CcAssignment.PERFORM).bindPressed(this, () -> selectMode(StripMode.PARAMETER));
      hwElements.getButton(CcAssignment.PERFORM).bindLight(this, () -> stripMode == StripMode.PARAMETER);
      hwElements.getButton(CcAssignment.NOTES).bindPressed(this, () -> selectMode(StripMode.NOTE));
      hwElements.getButton(CcAssignment.NOTES).bindLight(this, () -> stripMode == StripMode.NOTE);

      Layer positionLayer = touchStripLayer.getStepLayer(StripMode.NONE);
      touchStrip.bindStripLight(positionLayer, () -> positionToLight());
      touchStrip.bindValue(positionLayer, pos -> positionHandler.setPositionAbsolute(pos));
      touchStrip.bindTouched(positionLayer, touch -> {
      });

      Layer randomLayer = touchStripLayer.getStepLayer(StripMode.MOD);
      touchStrip.bindStripLight(randomLayer, () -> (int) Math.round(currentRandomValue * 127));
      touchStrip.bindValue(randomLayer, this::applyRandomValue);
      touchStrip.bindTouched(randomLayer, touch -> {
      });

      Layer repeatLayer = touchStripLayer.getStepLayer(StripMode.PARAMETER);
      touchStrip.bindStripLight(repeatLayer, () -> Math.min(currentRepeat * 16, 127));
      touchStrip.bindValue(repeatLayer, this::applyNoteRepeats);
      touchStrip.bindTouched(repeatLayer, touch -> {
      });

      Layer lengthLayer = touchStripLayer.getStepLayer(StripMode.NOTE);
      touchStrip.bindStripLight(lengthLayer, () -> Math.min(currentStepLen * 7, 127));
      touchStrip.bindValue(lengthLayer, this::applyNoteLength);
      touchStrip.bindTouched(lengthLayer, touch -> {
      });

      Layer timbreLayer = touchStripLayer.getStepLayer(StripMode.PITCH); // TODO not implemented
      touchStrip.bindStripLight(timbreLayer, () -> 64);
      touchStrip.bindValue(timbreLayer, value -> {
      });
      touchStrip.bindTouched(timbreLayer, touch -> {
      });

      stripCurrentLayer = positionLayer;
      stripCurrentLayer.setIsActive(true);
   }

   private void applyNoteLength(int pos) {
      for (int i = 0; i < 16; i++) {
         if (assignments[i] != null && pressStates[i] != PressState.None) {
            currentStepLen = pos / 7;
            assignments[i].setDuration(positionHandler.getGridResolution() * currentStepLen);
            pressStates[i] = PressState.Modify;
         }
      }
   }

   private void applyNoteRepeats(int pos) {
      for (int i = 0; i < 16; i++) {
         if (assignments[i] != null && pressStates[i] != PressState.None) {
            currentRepeat = pos / 16;
            assignments[i].setRepeatCount(currentRepeat);
            pressStates[i] = PressState.Modify;
         }
      }
   }

   private void applyRandomValue(int pos) {
      for (int i = 0; i < 16; i++) {
         if (assignments[i] != null && pressStates[i] != PressState.None) {
            currentRandomValue = pos / 127.0;
            assignments[i].setChance(currentRandomValue);
            pressStates[i] = PressState.Modify;
         }
      }
   }

   private void selectMode(StripMode mode) {
      if (stripMode == mode) {
         stripCurrentLayer.setIsActive(false);
         stripCurrentLayer = touchStripLayer.getStepLayer(StripMode.NONE);
         stripCurrentLayer.setIsActive(true);
         stripMode = StripMode.NONE;
      } else {
         Layer nextMode = touchStripLayer.getStepLayer(mode);
         if (nextMode != null) {
            stripCurrentLayer.setIsActive(false);
            stripCurrentLayer = nextMode;
            stripCurrentLayer.setIsActive(true);
            stripMode = mode;
         }
      }
   }

   private int positionToLight() {
      return positionHandler.getNormalizedLocation();
   }

   private void handleErase(boolean down) {
      eraseDown = down;
   }

   private void handleShift(boolean down) {
      shiftDown = down;
   }

   public void performDuplicateContent() {
      clip.duplicateContent();
   }

   private void handleDuplicate(boolean down) {
      if (isActive() && down) {
         if (shiftDown) {
            clip.duplicateContent();
         } else {
            clip.duplicate();
         }
      }
   }

   private void handleEncoderTouch(boolean touch) {
      if (!isActive()) {
         return;
      }
      stripCurrentLayer.setIsActive(!touch);
   }

   private void handleEncoderPress(final boolean down) {
   }

   private void handleEncoder(final int dir) {
      if (dir > 0) {
         this.positionHandler.scrollRight();
      } else {
         this.positionHandler.scrollLeft();
      }
   }

   private void setSelectMode(final boolean stepModeActive) {
      if (stepModeActive) {
         this.setIsActive(false);
         padLayer.setIsActive(true);
         padLayer.setSelectMode(true);
      } else if (padLayer.isActive()) {
         padLayer.setIsActive(false);
         padLayer.setSelectMode(false);
         this.setIsActive(true);
      }
   }

   @Inject
   public void setPadLayer(PadLayer padLayer) {
      this.padLayer = padLayer;
   }

   private void handleClipColorChanged(float r, float g, float b) {
   }

   private void handleNoteStep(final NoteStep noteStep) {
      assignments[noteStep.x()] = noteStep;
   }

   private void handlePlayingStep(final int playingStep) {
      if (playingStep == -1) {
         this.playingStep = -1;
      }
      this.playingStep = playingStep - positionHandler.getStepOffset();
   }


   private RgbColor getRgbState(final int index) {
      if (assignments[index] == null || assignments[index].state() == NoteStep.State.Empty) {
         if (index == playingStep) {
            return RgbColor.WHITE;
         }
         return RgbColor.OFF;
      } else if (isDrumEdit) {
         RgbColor color = clipColor;
         if (padLayer != null && padLayer.getSelectedColor() != RgbColor.OFF) {
            color = padLayer.getSelectedColor();
         }
         if (index == playingStep) {
            return color.brightness(ColorBrightness.SUPERBRIGHT);
         } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
            return color.brightness(ColorBrightness.DIMMED);
         }
         return color.brightness(ColorBrightness.BRIGHT);
      } else {
         if (index == playingStep) {
            return clipColor.brightness(ColorBrightness.SUPERBRIGHT);
         } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
            return clipColor.brightness(ColorBrightness.DIMMED);
         }
         return clipColor.brightness(ColorBrightness.BRIGHT);
      }
   }

   private void releaseStep(final int index) {
      if (!isActive()) {
         return;
      }
      if (pressStates[index] == PressState.Delete && (System.currentTimeMillis() - downTimes[index]) < 500) {
         clip.toggleStep(index, 0, padLayer.getFixedVelocity());
      }
      pressStates[index] = PressState.None;
      downTimes[index] = -1;
   }

   private void pressStep(final int index) {
      if (eraseDown) {
         final NoteStep note = assignments[index];
         if (note != null) {
            clip.toggleStep(index, 0, padLayer.getFixedVelocity());
         }
         return;
      }
      final NoteStep note = assignments[index];
      if (note == null || note.state() == NoteStep.State.Empty) {
         clip.setStep(index, 0, padLayer.getFixedVelocity(), positionHandler.getGridResolution() * stepLength);
         pressStates[index] = PressState.New;
      } else if (note == null || note.state() == NoteStep.State.NoteOn) {
         pressStates[index] = PressState.Delete;
      }
      downTimes[index] = System.currentTimeMillis();
      collectCurrentParam();
   }

   private void collectCurrentParam() {
      currentRandomValue = 0;
      currentRepeat = 0;
      currentStepLen = 1;
      for (int i = 0; i < 16; i++) {
         if (pressStates[i] != PressState.None && assignments[i] != null) {
            currentRandomValue = Math.max(currentRandomValue, assignments[i].chance());
            currentRepeat = Math.max(currentRepeat, assignments[i].repeatCount());
            int len = (int) (assignments[i].duration() / positionHandler.getGridResolution());
            currentStepLen = Math.max(1, len);
         }
      }
   }

   public void setSelectedNote(int selectedNote, int padIndex) {
      if (padIndex == -1 && !isDrumEdit) {
         focusNote = selectedNote;
      } else if (selectedNote == 0 && padIndex == 0 && isDrumEdit) {
         focusNote = 36;
      } else {
         focusNote = selectedNote;
      }
      clip.scrollToKey(focusNote);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      stripCurrentLayer.setIsActive(false);
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      stripCurrentLayer.setIsActive(true);
   }
}
