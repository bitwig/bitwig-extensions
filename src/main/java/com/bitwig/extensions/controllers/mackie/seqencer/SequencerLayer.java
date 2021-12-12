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
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SequencerLayer extends ButtonLayer {
   private final MackieMcuProExtension driver;
   protected final MixControl control;
   protected final IntValueObject velocity = new IntValueObject(100, 0, 127);
   protected final ValueSet gridResolution;
   protected final IntValueObject pageIndex;

   protected PinnableCursorClip cursorClip;
   protected StepViewPosition positionHandler;
   protected boolean deselectEnabled = true;
   protected final IntSetValue heldSteps = new IntSetValue();
   protected final Set<Integer> addedSteps = new HashSet<>();
   protected final Set<Integer> modifiedSteps = new HashSet<>();
   protected final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();

   protected int blinkTicks;
   protected int playingStep;
   protected final double gatePercent = 0.98;
   protected int selectedPadIndex = -1;
   protected final Layer recurrenceLayer;
   protected NoteStep copyNote = null;
   protected long firstDown = -1;
   protected final IntValueObject applyVelocity = new IntValueObject(-1, 0, 127, v -> v == -1 ? "<--->" : " " + v);
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
   protected final StepValue duration = new StepValue(0, 16, 0);
   protected final StepValue repeatCurve = new StepValue(-1, 1, 0);
   protected final StepValue repeatVelocity = new StepValue(0, 1, 0);
   protected final StepValue repeatVelocityEnd = new StepValue(-1, 1, 0);
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
      control.getModifier().addValueObserver(modifierValueObject -> {
         if (!modifierValueObject.isDuplicateSet() && copyNote != null) {
            copyNote = null;
         }
      });
   }

   public abstract void nextMenu();

   public abstract void previousMenu();

   abstract List<NoteStep> getHeldNotes();

   public MenuModeLayerConfiguration getMenu() {
      return currentMenu;
   }

   private void initStepValues() {
      applyVelocity.addValueObserver(value -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setVelocity(value / 127.0));
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
         } else if (size == 0) {
            deselectEnabled = true;
            applyVelocity.setDisabled();
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
            if (recurrenceLayer.isActive()) {
               activateRecurrence(false);
            }
         }
      });
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
            applyVelocity.set((int) Math.round(noteStep.velocity() * 127));
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
         });
      }
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
      control.addPressEncoderBinding(index, which -> value.reset(), false);
   }

   protected void bindClipControl(final Integer index, final MenuModeLayerConfiguration control) {
      control.addNameBinding(index, clipNameValue);
      control.addDisplayValueBinding(index, clipPlayStatus);
      control.addPressEncoderBinding(index, encIndex -> activate(cursorClip, control.getModifier()), false);
      control.addRingBoolBinding(index, cursorClip.clipLauncherSlot().isPlaying());
   }

   void bindMenuNavigate(final Integer index, final MenuModeLayerConfiguration control, final boolean forward,
                         final boolean showHeldSteps) {
      if (forward) {
         control.addDisplayValueBinding(index, new BasicStringValue("====>"));
      } else {
         control.addDisplayValueBinding(index, new BasicStringValue("<===="));
      }
      if (showHeldSteps) {
         control.addNameBinding(index, heldSteps);
      } else {
         control.addNameBinding(index, new BasicStringValue(" "));
      }
      if (forward) {
         control.addPressEncoderBinding(index, which -> nextMenu(), true);
      } else {
         control.addPressEncoderBinding(index, which -> previousMenu(), true);
      }
   }

   protected void activate(final PinnableCursorClip clip, final ModifierValueObject modifier) {
      final ClipLauncherSlot slot = clip.clipLauncherSlot();
      final Track track = clip.getTrack();
      if (modifier.isShiftSet()) {
         track.stop();
      } else {
         slot.launch();
      }
   }

   void editMask(final int mask) {
      int value = recurrenceMask.get();
      RemoteConsole.out.println("Edit {} v={}", mask, value);
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

   void handleNoteCopyAction(final int destinationIndex, final NoteStep note) {
      if (copyNote != null) {
         if (destinationIndex == copyNote.x()) {
            return;
         }
         final int vel = (int) Math.round(copyNote.velocity() * 127);
         final double duration = copyNote.duration();
         expectedNoteChanges.put(destinationIndex, copyNote);
         cursorClip.setStep(destinationIndex, 0, vel, duration);
      } else if (note != null && note.state() == NoteStep.State.NoteOn) {
         copyNote = note;
      }
   }

   void applyValues(final NoteStep dest, final NoteStep src) {
      dest.setChance(src.chance());
      dest.setTimbre(src.timbre());
      dest.setPressure(src.pressure());
      dest.setVelocitySpread(src.velocitySpread());
      dest.setRepeatCount(src.repeatCount());
      dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
      dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
      dest.setRepeatCurve(src.repeatCurve());
      dest.setPan(src.pan());
      dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
      dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
      dest.setOccurrence(src.occurrence());
   }

   DerivedStringValueObject createClipNameValue(final PinnableCursorClip clip) {
      final ClipLauncherSlot clipLauncherSlot = clip.clipLauncherSlot();
      clipLauncherSlot.sceneIndex().markInterested();
      return new DerivedStringValueObject() {
         @Override
         public void init() {
            clipLauncherSlot.name()
               .addValueObserver(name -> fireChanged(toString(name, clipLauncherSlot.exists().get())));
            clipLauncherSlot.exists()
               .addValueObserver(exists -> fireChanged(toString(clipLauncherSlot.name().get(), exists)));
         }

         private String toString(final String name, final boolean exists) {
            if (!exists) {
               return "[---]";
            }
            if (name.isEmpty()) {
               return String.format("[C:%d]", clipLauncherSlot.sceneIndex().get());
            }
            return "[" + StringUtil.toAsciiDisplay(name, 4) + "]";
         }

         @Override
         public String get() {
            return toString(clipLauncherSlot.name().get(), clipLauncherSlot.exists().get());
         }
      };
   }

   DerivedStringValueObject createPlayingStatusValue(final PinnableCursorClip clip) {
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
         }

         private String toString(final boolean playing, final boolean queued, final boolean stopQueued,
                                 final boolean recording) {
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
