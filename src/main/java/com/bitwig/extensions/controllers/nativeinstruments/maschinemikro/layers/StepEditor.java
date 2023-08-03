package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.StepViewPosition;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.StepMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.List;

@Component
public class StepEditor extends Layer {

   private final Clip clip;
   private final StepViewPosition positionHandler; // TODO Move this to commons
   private RgbColor clipColor;
   private int focusNote = 60;
   private final NoteStep[] assignments = new NoteStep[16];
   private final PressState[] pressStates = new PressState[16];
   private int playingStep = -1;
   private boolean isDrumEdit = false;
   
   private PadLayer padLayer;
   
   private enum PressState {
      None,
      New,
      Modify,
      Delete;
   }
   
   public StepEditor(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                     MidiProcessor midiProcessor, ControllerHost host) {
      super(layers, "PAD_LAYER");
      List<RgbButton> padButtons = hwElements.getPadButtons();
      this.clip = host.createLauncherCursorClip(16, 1);
      this.positionHandler = new StepViewPosition(this.clip);
      this.clip.color().addValueObserver((r, g, b) -> {
         clipColor = RgbColor.toColor(r, g, b);
      });
      viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
         isDrumEdit = hasDrumPads;
      });
      modifierLayer.getSelectHeld().addValueObserver(this::setSelectMode);

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
   }
   
   private void handleEncoderPress(final boolean down) {
   }
   
   private void handleEncoder(final int dir) {
      if(dir>0) {
         this.positionHandler.scrollRight();
      } else {
         this.positionHandler.scrollLeft();
      }
   }
   
   private void setSelectMode(final boolean active) {
      if(active) {
         this.setIsActive(false);
         padLayer.setIsActive(true);
         padLayer.setSelectMode(true);
      } else {
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
      // Which grid step contains Notes
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
         if (index == playingStep) {
            return clipColor.brightness(ColorBrightness.BRIGHT);
         } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
            return clipColor;
         }
         return clipColor.brightness(ColorBrightness.DIMMED);
      } else {
         if (index == playingStep) {
            return clipColor.brightness(ColorBrightness.BRIGHT);
         } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
            return clipColor;
         }
         return clipColor.brightness(ColorBrightness.DIMMED);
      }
   }

/*    private InternalHardwareLightState computeGridLedState(final int index) {
        if (assignments[index] == null || assignments[index].state() == NoteStep.State.Empty) {
            if (index == playingStep) {
                return RgbLed.WHITE_BRIGHT;
            }
            return RgbLed.OFF;
        } else if (isDrumEdit) {
            if (index == playingStep) {
                return RgbLed.of(padColor + 3);
            } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
                return RgbLed.of(padColor);
            }
            return RgbLed.of(padColor + 2);
        } else {
            if (index == playingStep) {
                return RgbLed.of(clipColor + 3);
            } else if (assignments[index].state() == NoteStep.State.NoteSustain) {
                return RgbLed.of(clipColor);
            }
            return RgbLed.of(clipColor + 2);
        }
    }
    */

   private void releaseStep(final int index) {
         if (!isActive()) {
            return;
         }
         if (pressStates[index] == PressState.Delete) {
            clip.toggleStep(index, 0, padLayer.getFixedVelocity());
         }
         pressStates[index] = PressState.None;
      
   }

   private void pressStep(final int index) {
      final NoteStep note = assignments[index];
      if (note == null || note.state() == NoteStep.State.Empty) {
         clip.setStep(index, 0, padLayer.getFixedVelocity(), positionHandler.getGridResolution());
         pressStates[index] = PressState.New;
      } else if (note == null || note.state() == NoteStep.State.NoteOn) {
         pressStates[index] = PressState.Delete;
      }
   }

   public void setSelectedNote(int selectedNote) {
      clip.scrollToKey(selectedNote);
      focusNote = selectedNote;
      DebugOutMk.println(" SET selected Note %d", selectedNote);
   }
}
