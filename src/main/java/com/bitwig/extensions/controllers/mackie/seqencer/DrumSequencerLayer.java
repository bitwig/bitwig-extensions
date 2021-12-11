package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.layer.ButtonLayer;
import com.bitwig.extensions.controllers.mackie.layer.DrumMixerLayerGroup;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DrumSequencerLayer extends ButtonLayer {
   private boolean deselectEnabled = true;
   private final NoteStep[] assignments = new NoteStep[32];
   private final IntSetValue heldSteps = new IntSetValue();
   private final Set<Integer> addedSteps = new HashSet<>();
   private final Set<Integer> modifiedSteps = new HashSet<>();
   private final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();

   private final MackieMcuProExtension driver;
   private final MixControl control;
   private PinnableCursorClip cursorClip;
   private StepViewPosition positionHandler;
   private int drumScrollOffset;
   private int blinkTicks;
   private int playingStep;
   private final double gatePercent = 0.98;
   private MenuModeLayerConfiguration clipEditMenu;
   private MenuModeLayerConfiguration stepValueMenu;
   private MenuModeLayerConfiguration currentMenu;
   private final Layer recurrenceLayer;

   private final IntValueObject velocity = new IntValueObject(100, 0, 127);
   private final ValueSet gridResolution;
   private final IntValueObject pageIndex;
   private int selectedPadIndex = -1;
   private DrumPadBank drumPadBank;
   private long firstDown = -1;
   private final IntValueObject applyVelocity = new IntValueObject(-1, 0, 127, v -> v == -1 ? "<--->" : " " + v);
   private final IntValueObject recurrence = new IntValueObject(-1, 1, 8,
      v -> v == -1 ? "<--->" : (v == 1 ? " OFF " : " " + v));
   private final IntValueObject recurrenceMask = new IntValueObject(-1, 0, 255,
      v -> v == -1 ? "<--->" : (v == 1 ? " OFF " : " " + v));
   private final StepValue timbre = new StepValue(-1, 1, 0);
   private final StepValue chance = new StepValue(0, 1, 1);
   private final StepValue pressure = new StepValue(0, 1, 0);
   private final StepValue velSpread = new StepValue(0, 1, 0);


   public DrumSequencerLayer(final MixControl mixControl) {
      super("DrumSeq_" + mixControl.getHwControls().getSectionIndex(), mixControl, BasicNoteOnAssignment.REC_BASE);
      driver = mixControl.getDriver();
      control = mixControl;
      recurrenceLayer = new Layer(mixControl.getDriver().getLayers(), "Recurrence Editor");
      gridResolution = new ValueSet().add("1/32", 0.125).add("1/16", 0.25).add("1/8", 0.5).add("1/4", 1.0).select(1);
      pageIndex = new IntValueObject(0, 0, 1, v -> StringUtil.toBarBeats(v * gridResolution.getValue() * 4));
      applyVelocity.addValueObserver(value -> {
         if (!heldSteps.isEmpty()) {
            getHeldNotes().forEach(noteStep -> noteStep.setVelocity(value / 127.0));
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
            if (recurrenceLayer.isActive()) {
               activateRecurrence(false);
            }
         }
      });
   }

   private void updateNotesSelected() {
      if (!heldSteps.isEmpty()) {
         getHeldNotes().stream().findFirst().ifPresent(noteStep -> {
            applyVelocity.set((int) Math.round(noteStep.velocity() * 127));
            timbre.set(noteStep.timbre());
            chance.set(noteStep.chance());
            pressure.set(noteStep.pressure());
            velSpread.set(noteStep.velocitySpread());
            RemoteConsole.out.println(" UPDATE NOTE SEL msk={}", noteStep.recurrenceMask());

            recurrenceMask.set(noteStep.recurrenceMask());
            recurrence.set(noteStep.recurrenceLength());
         });
      }
   }

   public void scrollStepsBy(final int direction) {
      if (!isActive()) {
         return;
      }
      if (control.getModifier().isShiftSet()) {
         nextMenu();
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

   public void init(final DrumPadBank drumPadBank, final DrumNoteHandler noteHandler) {
      final MixerSectionHardware hwControls = control.getHwControls();
      final int sectionIndex = hwControls.getSectionIndex();
      cursorClip = driver.getCursorTrack().createLauncherCursorClip(16, 1);
      positionHandler = new StepViewPosition(cursorClip, 16);
      this.drumPadBank = drumPadBank;
      drumPadBank.scrollPosition().addValueObserver(offset -> drumScrollOffset = offset);
      pageIndex.setMax(positionHandler.getPages() - 1);

      cursorClip.addNoteStepObserver(this::handleNoteStep);
      cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
      positionHandler.addPagesChangedCallback((index, pages) -> {
         pageIndex.setMax(pages - 1);
         pageIndex.set(index);
      });
      gridResolution.addValueObserver(s -> positionHandler.setGridResolution(gridResolution.getValue()));
      pageIndex.addValueObserver(val -> positionHandler.setPage(val));

      initSelection(sectionIndex, hwControls, drumPadBank, noteHandler);

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 8; col++) {
            final int step = row * 8 + col;
            final HardwareButton button = hwControls.getButton(row + 1, col);
            bindStepButton(button, step);
         }
      }
      clipEditMenu = initClipMenu();
      stepValueMenu = initStepValueMenu();
      currentMenu = clipEditMenu;
   }

   public void nextMenu() {
      if (!isActive()) {
         return;
      }
      if (currentMenu == clipEditMenu) {
         currentMenu = stepValueMenu;
      } else {
         currentMenu = clipEditMenu;
      }
      control.applyUpdate();
   }

   private void bindStepButton(final HardwareButton button, final int step) {
      final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();

      bindPressed(button, () -> handleStepPressed(step));
      bindReleased(button, () -> handleStepReleased(step));
      bind(() -> stepState(step), light);
   }

   private MenuModeLayerConfiguration initClipMenu() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("STEP_CLIP_MENU", control);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      final SettableBeatTimeValue clipLength = cursorClip.getLoopLength();
      final BeatTimeFormatter formatter = control.getDriver().getHost().createBeatTimeFormatter(":", 2, 1, 1, 0);
      builder.bindValue("Length", clipLength, v -> {

      }, formatter, 4.0, 1.0);
      builder.bindValue("Offset", pageIndex, false);
      builder.bindValueSet("Grid", gridResolution);
      builder.bindValue("In.Vel", velocity, true);
      builder.bindAction("Dbl.Cnt", "<EXE>", () -> cursorClip.duplicateContent());
      builder.bindAction("Duplicate", "<EXE>", () -> cursorClip.duplicate());
      builder.bindAction("Clear", "<EXE>", () -> cursorClip.clearSteps());
      builder.bindPlaying(cursorClip);

      builder.fillRest();

      return menu;
   }

   private MenuModeLayerConfiguration initStepValueMenu() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("STEP_CLIP_MENU", control);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bindPlaying(cursorClip);
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("<Sel>"));
         control.addDisplayValueBinding(index, heldSteps);
         control.addPressEncoderBinding(index, v -> deselectEnabled = false, true);
      });
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("N.Vel"));
         control.addDisplayValueBinding(index, applyVelocity);
         control.addEncoderIncBinding(index, inc -> {
            applyVelocity.increment(inc);
            deselectEnabled = false;
         }, true);
         control.addRingBinding(index, applyVelocity);
      });
      builder.bind((index, control) -> bindStepValue(index, control, "Timbre", timbre));
      builder.bind((index, control) -> bindStepValue(index, control, "Press", pressure));
      builder.bind((index, control) -> bindStepValue(index, control, "Vl.Spr", velSpread));
      builder.bind((index, control) -> bindStepValue(index, control, "Chance", chance));
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("Recur"));
         control.addDisplayValueBinding(index, recurrence);
         control.addEncoderIncBinding(index, inc -> {
            recurrence.increment(inc);
            activateRecurrence(recurrence.get() > 1);
            deselectEnabled = false;
         }, true);
         control.addPressEncoderBinding(index, idx -> activateRecurrence(!recurrenceLayer.isActive()));
         control.addRingBinding(index, recurrence);
      });

      builder.fillRest();
      return menu;
   }

   private void bindStepValue(final int index, final MenuModeLayerConfiguration control, final String title,
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

   List<NoteStep> getHeldNotes() {
      return heldSteps.stream()//
         .map(idx -> assignments[idx])//
         .filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn) //
         .collect(Collectors.toList());
   }

   public MenuModeLayerConfiguration getMenu(final DrumMixerLayerGroup.EditorMode mode) {
      return currentMenu;
   }

   private void initSelection(final int sectionIndex, final MixerSectionHardware hwControls,
                              final DrumPadBank drumPadBank, final DrumNoteHandler noteHandler) {
      setNoteHandler(noteHandler);
      for (int i = 0; i < 8; i++) {
         final int index = i;
         final int trackIndex = i + sectionIndex * 8;
         final DrumPad pad = drumPadBank.getItemAt(trackIndex);
         final BooleanValueObject selectedInMixer = new BooleanValueObject();
         pad.addIsSelectedInMixerObserver(selectedInMixer::set);
         pad.addIsSelectedInMixerObserver(v -> {
            if (v) {
               selectedPadIndex = index;
               cursorClip.scrollToKey(selectedPadIndex + drumScrollOffset);
            }
         });
         hwControls.bindButton(this, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer,
            () -> control.handlePadSelection(pad));
         hwControls.bindButton(this, index, MixerSectionHardware.REC_INDEX, noteHandler.isPlaying(index), () -> {
         });
         final int mask = 0x1 << index;
         hwControls.bindButton(recurrenceLayer, index, MixerSectionHardware.REC_INDEX, () -> maskLighting(mask, index),
            () -> editMask(mask));
      }
   }

   private boolean maskLighting(final int mask, final int index) {
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

   private void editMask(final int mask) {
      int value = recurrenceMask.get();
      RemoteConsole.out.println("Edit {} v={}", mask, value);
      if ((mask & value) != 0) {
         value &= ~mask;
      } else {
         value |= mask;
      }
      recurrenceMask.set(value);
   }

   public boolean stepState(final int step) {
      final NoteStep.State state = assignments[step] == null ? NoteStep.State.Empty : assignments[step].state();
      final int steps = positionHandler.getAvailableSteps();
      //if (step < steps) {
      if (state == NoteStep.State.NoteOn) {
         return playingStep != step;
      } else {
         return playingStep == step;
      }

      // }
      //return false;
   }

   public void handleStepPressed(final int step) {
      heldSteps.add(step);
      final NoteStep note = assignments[step];
      if (note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain) {
         cursorClip.setStep(step, 0, velocity.get(), positionHandler.getGridResolution() * gatePercent);
         addedSteps.add(step);
      }
   }

   public void handleStepReleased(final int step) {
      final long diff = System.currentTimeMillis() - firstDown;
      final NoteStep note = assignments[step];
      final boolean doToggle = deselectEnabled && diff < 1000;
      heldSteps.remove(step);
      if (note != null && note.state() == NoteStep.State.NoteOn && !addedSteps.contains(step)) {
         if (!modifiedSteps.contains(step)) {
            if (doToggle) {
               cursorClip.toggleStep(step, 0, velocity.get());
            }
         } else {
            modifiedSteps.remove(step);
         }
      }
      addedSteps.remove(step);
   }

   public void notifyBlink(final int ticks) {
      blinkTicks = ticks;
   }

   private void handleNoteStep(final NoteStep noteStep) {
      final int newStep = noteStep.x();
      assignments[newStep] = noteStep;
      if (isActive()) {
         updateNotesSelected();
      }
      if (expectedNoteChanges.containsKey(newStep)) {
         final NoteStep previousStep = expectedNoteChanges.get(newStep);
         expectedNoteChanges.remove(newStep);
      }
   }

   private void handlePlayingStep(final int playingStep) {
      if (playingStep == -1) {
         this.playingStep = -1;
      }
      this.playingStep = playingStep - positionHandler.getStepOffset();
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      if (selectedPadIndex != -1) {
         cursorClip.scrollToKey(selectedPadIndex + drumScrollOffset);
      } else {
         drumPadBank.getItemAt(0).selectInMixer();
      }

   }
}
