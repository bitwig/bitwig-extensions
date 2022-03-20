package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.layer.ButtonLayer;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.value.*;
import com.bitwig.extensions.framework.Layer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SequencerLayer extends ButtonLayer {
   protected final MackieMcuProExtension driver;
   protected final MixControl control;
   protected final ValueSet gridResolution;
   protected final IntValueObject pageIndex;

   protected PinnableCursorClip cursorClip;
   protected StepViewPosition positionHandler;
   protected boolean deselectEnabled = true;
   protected final IntSetValue heldSteps = new IntSetValue();
   protected final Set<Integer> addedSteps = new HashSet<>();
   protected final Set<Integer> modifiedSteps = new HashSet<>();
   protected final IntValueObject menuPageIndex = new IntValueObject(0, 0, 2);

   protected int blinkTicks;
   protected int playingStep;
   protected final double gatePercent = 0.98;
   protected int selectedPadIndex = -1;
   protected final Layer recurrenceLayer;
   protected long firstDown = -1;

   protected final EditorValue velocityValue = new EditorValue(100,
      (edit, value) -> edit ? "* " + value : String.format("<%3d>", value));

   protected final IntValueObject recurrence = new IntValueObject(-1, 1, 8,
      v -> v == -1 ? "<--->" : (v == 1 ? " OFF " : " " + v));
   protected final IntValueObject recurrenceMask = new IntValueObject(-1, 0, 255,
      v -> v == -1 ? "<--->" : (v == 1 ? " OFF " : " " + v));
   protected final IntValueObject repeat = new IntValueObject(-1, 0, 32,
      v -> v == -1 ? "<--->" : (v == 0 ? " OFF" : " " + (v + 1)));
   protected final StepValue timbre = new StepValue(-1, 1, 0);
   protected final StepValue chance = new StepValue(0, 1, 1);
   protected final StepValue pressure = new StepValue(0, 1, 0);
   protected final StepValue velSpread = new StepValue(0, 1, 0);
   protected final StepValue duration = new StepValue(0.02, 16, 0);
   protected final StepValue repeatCurve = new StepValue(-1, 1, 0);
   protected final StepValue repeatVelocity = new StepValue(0, 1, 0);
   protected final StepValue repeatVelocityEnd = new StepValue(-1, 1, 0);
   protected final EnumConfigValue<NoteOccurrence> occurrence = new EnumConfigValue<NoteOccurrence>() //
      .add(NoteOccurrence.ALWAYS, "Always")
      .add(NoteOccurrence.FIRST, "First")
      .add(NoteOccurrence.NOT_FIRST, "N.Frst")
      .add(NoteOccurrence.PREV, "Prev")
      .add(NoteOccurrence.NOT_PREV, "N.Prev")
      .add(NoteOccurrence.PREV_KEY, "PrvKey")
      .add(NoteOccurrence.NOT_PREV_KEY, "N.PrKY")
      .add(NoteOccurrence.PREV_CHANNEL, "PrvCH")
      .add(NoteOccurrence.NOT_PREV_CHANNEL, "N.PrCH")
      .add(NoteOccurrence.FILL, "Fill")
      .add(NoteOccurrence.NOT_FILL, "NotFll")
      .init(NoteOccurrence.ALWAYS);
   protected DerivedStringValueObject clipNameValue;
   protected DerivedStringValueObject clipPlayStatus;
   protected MenuModeLayerConfiguration stepLenValueMenu;
   protected MenuModeLayerConfiguration currentMenu;

   public SequencerLayer(final String name, final MixControl mixControl, final NoteAssignment base) {
      super(name, mixControl, base);
      driver = mixControl.getDriver();
      control = mixControl;
      recurrenceLayer = new Layer(mixControl.getDriver().getLayers(), "Recurrence Editor");
      gridResolution = new ValueSet().add("1/32", 0.125).add("1/16", 0.25).add("1/8", 0.5).add("1/4", 1.0).select(1);
      pageIndex = new IntValueObject(0, 0, 1, v -> StringUtil.toBarBeats(v * gridResolution.getValue() * 4));

      initStepValues();
   }

   public void scrollStepsBy(final int direction) {
      if (!isActive()) {
         return;
      }
      if (control.getModifier().isShiftSet()) {
         if (direction < 0) {
            previousMenu();
         } else {
            nextMenu();
         }
      } else {
         if (direction < 0) {
            positionHandler.scrollLeft();
         } else {
            positionHandler.scrollRight();
         }
      }
   }

   public void navigateVertically(final int direction) {
      if (!isActive()) {
         return;
      }

      if (direction > 0) {
         cursorClip.selectPrevious();
      } else {
         cursorClip.selectNext();
      }
   }

   public abstract void nextMenu();

   public abstract void previousMenu();

   abstract List<NoteStep> getHeldNotes();

   public MenuModeLayerConfiguration getMenu() {
      return currentMenu;
   }

   void initStepValues() {
      occurrence.addEnumValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setOccurrence(v));
         }
      });
      repeat.addValueObserver(v -> {
         if (!heldSteps.isEmpty() && v != -1) {
            getHeldNotes().forEach(noteStep -> noteStep.setRepeatCount(v));
         }
      });
      recurrence.addValueObserver(value -> {
         if (!heldSteps.isEmpty() && value != -1) {
            final int recValue = recurrenceMask.get() == -1 ? 0 : recurrenceMask.get();
            getHeldNotes().forEach(noteStep -> noteStep.setRecurrence(value, recValue));
         }
      });
      recurrenceMask.addValueObserver(value -> {
         if (!heldSteps.isEmpty() && value != -1 && recurrence.get() != -1) {
            getHeldNotes().forEach(noteStep -> noteStep.setRecurrence(recurrence.get(), value));
         }
      });
      timbre.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setTimbre(v));
         }
      });
      repeatCurve.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setRepeatCurve(v));
         }
      });
      repeatVelocity.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setRepeatVelocityCurve(v));
         }
      });
      repeatVelocityEnd.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setRepeatVelocityEnd(v));
         }
      });
      duration.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setDuration(v));
         }
      });
      duration.setConverter(v -> String.format("%2.1f", v / gridResolution.getValue()));
      pressure.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setPressure(v));
         }
      });
      chance.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setChance(v));
         }
      });
      velSpread.addDoubleValueObserver(v -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setVelocitySpread(v));
         }
      });
      heldSteps.addSizeValueListener((oldSize, size) -> {
         if (oldSize == 0 && size > 0) {
            updateNotesSelected();
            firstDown = System.currentTimeMillis();
            handleSelect();
         } else if (size == 0) {
            deselectEnabled = true;
            velocityValue.exitEdit();
            timbre.unset();
            chance.unset();
            pressure.unset();
            velSpread.unset();
            recurrence.setDisabled();
            recurrenceMask.setDisabled();
            repeat.setDisabled();
            duration.unset();
            repeatCurve.unset();
            repeatVelocityEnd.unset();
            repeatVelocity.unset();
            occurrence.unset();
            if (recurrenceLayer.isActive()) {
               activateRecurrence(false);
            }
            handleReleased();
         }
      });
   }

   void handleSelect() {
   }

   void handleReleased() {

   }

   protected CursorTrack getCursorTrack() {
      return driver.getCursorTrack();
   }

   public void activateRecurrence(final boolean activate) {
      if (activate) {
         if (!recurrenceLayer.isActive()) {
            deactivateNotePlaying();
            recurrenceLayer.activate();
         }
      } else {
         if (recurrenceLayer.isActive()) {
            activateNotePlaying();
            recurrenceLayer.deactivate();
         }
      }
   }

   protected void updateNotesSelected() {
      if (!heldSteps.isEmpty()) {
         getHeldNotes().stream().findFirst().ifPresent(noteStep -> {
            velocityValue.setEditValue((int) Math.round(127 * noteStep.velocity()));
            timbre.set(noteStep.timbre());
            chance.set(noteStep.chance());
            pressure.set(noteStep.pressure());
            velSpread.set(noteStep.velocitySpread());

            recurrenceMask.set(noteStep.recurrenceMask());
            recurrence.set(noteStep.recurrenceLength());
            repeat.set(noteStep.repeatCount());
            repeatCurve.set(noteStep.repeatCurve());
            repeatVelocity.set(noteStep.repeatVelocityCurve());
            repeatVelocityEnd.set(noteStep.repeatVelocityEnd());
            duration.set(noteStep.duration());
            occurrence.set(noteStep.occurrence());
         });
      }
   }

   void bindVelocityValue(final Integer index, final MenuModeLayerConfiguration control) {
      control.addNameBinding(index, new BasicStringValue("Vel"));
      control.addEncoderIncBinding(index, this::incrementVelocityValue, true);
      control.addDisplayValueBinding(index, velocityValue);
      control.addRingBinding(index, velocityValue);
   }

   private void incrementVelocityValue(final int increment) {
      if (!heldSteps.isEmpty()) {
         final List<NoteStep> notes = getHeldNotes();
         notes.forEach(note -> incrementVelocity(note, increment));
         deselectEnabled = false;
      } else {
         velocityValue.increment(increment);
      }
   }

   private void incrementVelocity(final NoteStep note, final int amount) {
      final int vel = (int) Math.round(note.velocity() * 127);
      final int newVel = Math.max(0, Math.min(127, vel + amount));
      if (newVel != vel) {
         velocityValue.setEditValue(newVel);
         note.setVelocity(newVel / 127.0);
      }
   }

   protected void bindOccurrence(final Integer index, final MenuModeLayerConfiguration control) {
      control.addNameBinding(index, new BasicStringValue("Occur"));
      control.addDisplayValueBinding(index, occurrence);
      control.addEncoderIncBinding(index, inc -> {
         occurrence.increment(inc);
         deselectEnabled = false;
      }, false);
      control.addRingBinding(index, occurrence);
      control.addPressEncoderBinding(index, which -> occurrence.reset(), false);
   }

   protected void bindRecurrence(final Integer index, final MenuModeLayerConfiguration control) {
      control.addNameBinding(index, new BasicStringValue("Recur"));
      control.addDisplayValueBinding(index, recurrence);
      control.addEncoderIncBinding(index, inc -> {
         if (recurrence.get() != -1) {
            recurrence.increment(inc);
            activateRecurrence(recurrence.get() > 1);
            deselectEnabled = false;
         }
      }, true);
      control.addPressEncoderBinding(index, idx -> activateRecurrence(!recurrenceLayer.isActive()));
      control.addRingBinding(index, recurrence);
   }

   void bindStepValue(final int index, final MenuModeLayerConfiguration control, final String title,
                      final StepValue value) {
      control.addNameBinding(index, new BasicStringValue(title));
      control.addDisplayValueBinding(index, value);
      control.addEncoderIncBinding(index, inc -> {
         value.increment(inc);
         deselectEnabled = false;
      }, true);
      control.addRingBinding(index, value);
      control.addPressEncoderBinding(index, which -> value.resetOnUnset(), false);
   }

   protected void bindClipControl(final Integer index, final MenuModeLayerConfiguration control) {
      control.addNameBinding(index, clipNameValue);
      control.addDisplayValueBinding(index, clipPlayStatus);
      control.addPressEncoderBinding(index, encIndex -> activate(cursorClip, control.getModifier()), false);
      control.addRingBoolBinding(index, cursorClip.clipLauncherSlot().isPlaying());
   }

   void bindRepeatValue(final Integer index, final MenuModeLayerConfiguration control) {
      control.addNameBinding(index, new BasicStringValue("Rpeat"));
      control.addDisplayValueBinding(index, repeat);
      control.addEncoderIncBinding(index, inc -> {
         repeat.increment(inc);
         deselectEnabled = false;
      }, true);
      control.addPressEncoderBinding(index, idx -> repeat.set(1));
      control.addRingBinding(index, repeat);
   }

   void bindNoteLength(final Integer index, final MenuModeLayerConfiguration control) {
      final BooleanValueObject encoderHold = new BooleanValueObject();
      control.addNameBinding(index, new BasicStringValue("Len"));
      control.addDisplayValueBinding(index, duration);
      control.addEncoderIncBinding(index, inc -> {
         if (encoderHold.get()) {
            duration.increment(gridResolution.getValue() * inc * 0.1);
         } else {
            duration.increment(gridResolution.getValue() * inc);
         }
         deselectEnabled = false;
      }, true);
      control.addRingBinding(index, duration);
      control.addPressEncoderBinding(index, which -> {
         if (control.getModifier().isClearSet()) {
            duration.set(gridResolution.getValue());
         } else {
            encoderHold.set(true);
         }
      });
      control.addReleaseEncoderBinding(index, which -> encoderHold.set(false));
   }


   void bindMenuNavigate(final Integer index, final MenuModeLayerConfiguration control, final boolean forward,
                         final boolean showHeldSteps) {
      bindMenuNavigate(index, control, forward, showHeldSteps, null);
   }

   void bindMenuNavigate(final Integer index, final MenuModeLayerConfiguration control, final boolean forward,
                         final boolean showHeldSteps, final Runnable shiftFuntion) {
      control.addDisplayValueBinding(index, forward ? new BasicStringValue(" -->") : new BasicStringValue(" <--"));
      control.addNameBinding(index, showHeldSteps ? heldSteps : new BasicStringValue(" "));
      if (shiftFuntion != null) {
         control.addPressEncoderBinding(index, which -> {
            if (control.getModifier().isShiftSet()) {
               shiftFuntion.run();
            } else if (forward) {
               nextMenu();
            } else {
               previousMenu();
            }
         }, true);
      } else {
         control.addPressEncoderBinding(index, forward ? which -> nextMenu() : which -> previousMenu(), true);
      }
   }


   protected void activate(final PinnableCursorClip clip, final ModifierValueObject modifier) {
      final ClipLauncherSlot slot = clip.clipLauncherSlot();
      final Track track = clip.getTrack();
      if (modifier.isShiftSet() && modifier.isDuplicateSet()) {
         clip.duplicate();
      } else if (modifier.isClearSet()) {
         clip.clearSteps();
      } else if (modifier.isDuplicateSet()) {
         clip.duplicateContent();
         driver.getActionSet().zoomToFitEditor();
      } else if (modifier.isShiftSet()) {
         track.stop();
      } else {
         slot.launch();
      }
   }

   void editMask(final int mask) {
      int value = recurrenceMask.get();
      if ((mask & value) != 0) {
         value &= ~mask;
      } else {
         value |= mask;
      }
      recurrenceMask.set(value);
   }

   boolean maskLighting(final int mask, final int index) {
      if (recurrenceMask.get() < 1 || recurrence.get() < 2) {
         return false;
      }
      if (index >= recurrence.get()) {
         return false;
      }
      if ((mask & recurrenceMask.get()) != 0) {
         return true;
      }
      return blinkTicks % 4 == 1;
   }


   public void notifyBlink(final int ticks) {
      blinkTicks = ticks;
   }

   void handlePlayingStep(final int playingStep) {
      if (playingStep == -1) {
         this.playingStep = -1;
      }
      this.playingStep = playingStep - positionHandler.getStepOffset();
   }


   void applyValues(final NoteStep dest, final NoteStep src) {
      if (src.chance() != dest.chance()) {
         dest.setChance(src.chance());
      }
      if (src.timbre() != dest.timbre()) {
         dest.setTimbre(src.timbre());
      }
      if (src.pressure() != dest.pressure()) {
         dest.setPressure(src.pressure());
      }
      if (src.velocitySpread() == dest.velocitySpread()) {
         dest.setVelocitySpread(src.velocitySpread());
      }
      if (src.repeatCount() != dest.repeatCount()) {
         dest.setRepeatCount(src.repeatCount());
      }
      if (src.repeatVelocityCurve() != dest.repeatVelocityCurve()) {
         dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
      }
      if (src.repeatVelocityEnd() != dest.repeatVelocityEnd()) {
         dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
      }
      if (src.repeatCurve() != dest.repeatCurve()) {
         dest.setRepeatCurve(src.repeatCurve());
      }
      if (src.pan() != dest.pan()) {
         dest.setPan(src.pan());
      }
      if (dest.recurrenceLength() != src.recurrenceLength() && dest.recurrenceMask() != src.recurrenceMask()) {
         dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
      }
      if (src.occurrence() != dest.occurrence()) {
         dest.setOccurrence(src.occurrence());
      }
   }

   DerivedStringValueObject createClipNameValue(final PinnableCursorClip clip) {
      final ClipLauncherSlot clipLauncherSlot = clip.clipLauncherSlot();
      clipLauncherSlot.sceneIndex().markInterested();
      return new DerivedStringValueObject() {
         @Override
         public void init() {
            clipLauncherSlot.name()
               .addValueObserver(name -> fireChanged(
                  toString(name, clipLauncherSlot.exists().get(), clipLauncherSlot.sceneIndex().get())));
            clipLauncherSlot.exists()
               .addValueObserver(exists -> fireChanged(
                  toString(clipLauncherSlot.name().get(), exists, clipLauncherSlot.sceneIndex().get())));
            clipLauncherSlot.sceneIndex()
               .addValueObserver(index -> fireChanged(
                  toString(clipLauncherSlot.name().get(), clipLauncherSlot.exists().get(), index)));
         }

         private String toString(final String name, final boolean exists, final int sceneIndex) {
            if (!exists) {
               return "[---]";
            }
            if (name.isEmpty()) {
               return String.format("[C:%d]", sceneIndex);
            }
            return "[" + StringUtil.toAsciiDisplay(name, 4) + "]";
         }

         @Override
         public String get() {
            return toString(clipLauncherSlot.name().get(), clipLauncherSlot.exists().get(),
               clipLauncherSlot.sceneIndex().get());
         }
      };
   }

   DerivedStringValueObject createPlayingStatusValue(final PinnableCursorClip clip,
                                                     final ModifierValueObject modifier) {
      final ClipLauncherSlot csSlot = clip.clipLauncherSlot();
      csSlot.sceneIndex().markInterested();
      return new DerivedStringValueObject() {
         @Override
         public void init() {
            csSlot.isPlaying()
               .addValueObserver(playing -> fireChanged(
                  toString(playing, csSlot.isPlaybackQueued().get(), csSlot.isStopQueued().get(),
                     csSlot.isRecording().get())));
            csSlot.isPlaybackQueued()
               .addValueObserver(queued -> fireChanged(
                  toString(csSlot.isPlaying().get(), queued, csSlot.isStopQueued().get(), csSlot.isRecording().get())));
            csSlot.isStopQueued()
               .addValueObserver(queued -> fireChanged(
                  toString(csSlot.isPlaying().get(), csSlot.isPlaybackQueued().get(), queued,
                     csSlot.isRecording().get())));
            csSlot.isRecording()
               .addValueObserver(recording -> fireChanged(
                  toString(csSlot.isPlaying().get(), csSlot.isPlaybackQueued().get(), csSlot.isStopQueued().get(),
                     recording)));
            modifier.addValueObserver(value -> fireChanged(
               toString(csSlot.isPlaying().get(), csSlot.isPlaybackQueued().get(), csSlot.isStopQueued().get(),
                  csSlot.isRecording().get())));
         }

         private String toString(final boolean playing, final boolean queued, final boolean stopQueued,
                                 final boolean recording) {
            if (modifier.isShiftSet() && modifier.isDuplicateSet()) {
               return ">Duplicate";
            }
            if (modifier.isClearSet()) {
               return ">Clear";
            }
            if (modifier.isDuplicateSet()) {
               return ">Dbl";
            }
            if (modifier.isShiftSet()) {
               return ">Stop";
            }
            if (stopQueued || queued) {
               return "[Qued]";
            }
            if (recording) {
               return "[REC ]";
            }
            return playing ? "[Play]" : "[Stop]";
         }

         @Override
         public String get() {
            return toString(csSlot.isPlaying().get(), csSlot.isPlaybackQueued().get(), csSlot.isStopQueued().get(),
               csSlot.isRecording().get());
         }
      };
   }

}
