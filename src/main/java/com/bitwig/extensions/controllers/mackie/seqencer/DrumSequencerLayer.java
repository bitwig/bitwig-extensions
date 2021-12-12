package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.List;
import java.util.stream.Collectors;

public class DrumSequencerLayer extends SequencerLayer {

   public static final int STEPS = 16;

   private int drumScrollOffset;
   private MenuModeLayerConfiguration clipEditMenu;
   private MenuModeLayerConfiguration stepValueMenu;
   private DrumPadBank drumPadBank;
   protected final NoteStep[] assignments = new NoteStep[STEPS];

   public DrumSequencerLayer(final MixControl mixControl) {
      super("DrumSeq_" + mixControl.getHwControls().getSectionIndex(), mixControl, BasicNoteOnAssignment.REC_BASE);
   }

   public void init(final DrumPadBank drumPadBank, final DrumNoteHandler noteHandler) {
      final MixerSectionHardware hwControls = control.getHwControls();
      final int sectionIndex = hwControls.getSectionIndex();
      cursorClip = getCursorTrack().createLauncherCursorClip(STEPS, 1);
      positionHandler = new StepViewPosition(cursorClip, STEPS);
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

      clipNameValue = createClipNameValue(cursorClip);
      clipPlayStatus = createPlayingStatusValue(cursorClip);

      initSelection(sectionIndex, hwControls, drumPadBank, noteHandler);

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 8; col++) {
            final int step = row * 8 + col;
            final HardwareButton button = hwControls.getButton(row + 1, col);
            bindStepButton(button, step);
         }
      }
      clipEditMenu = initPage1Menu();
      stepValueMenu = initPage2Menu();
      stepLenValueMenu = initPage3Menu();
      currentMenu = clipEditMenu;
   }

   @Override
   List<NoteStep> getHeldNotes() {
      return heldSteps.stream()//
         .map(idx -> assignments[idx])//
         .filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn) //
         .collect(Collectors.toList());
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

   @Override
   public void nextMenu() {
      if (!isActive()) {
         return;
      }
      if (currentMenu == clipEditMenu) {
         currentMenu = stepValueMenu;
      } else if (currentMenu == stepValueMenu) {
         currentMenu = stepLenValueMenu;
      } else {
         currentMenu = clipEditMenu;
      }
      control.applyUpdate();
   }

   @Override
   public void previousMenu() {
      if (!isActive()) {
         return;
      }
      if (currentMenu == clipEditMenu) {
         currentMenu = stepLenValueMenu;
      } else if (currentMenu == stepValueMenu) {
         currentMenu = clipEditMenu;
      } else {
         currentMenu = stepValueMenu;
      }
      control.applyUpdate();
   }

   private void bindStepButton(final HardwareButton button, final int step) {
      final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();

      bindPressed(button, () -> handleStepPressed(step));
      bindReleased(button, () -> handleStepReleased(step));
      bind(() -> stepState(step), light);
   }

   private MenuModeLayerConfiguration initPage1Menu() {
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
      builder.bind((index, control) -> bindMenuNavigate(index, control, true, false));
      builder.fillRest();

      return menu;
   }

   private MenuModeLayerConfiguration initPage2Menu() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("STEP_CLIP_MENU", control);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bind((index, control) -> bindMenuNavigate(index, control, false, false));
      builder.bind(this::bindClipControl);
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
      builder.bind((index, control) -> bindMenuNavigate(index, control, true, true));

      builder.fillRest();
      return menu;
   }

   private MenuModeLayerConfiguration initPage3Menu() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("STEP_CLIP_MENU_2", control);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bind((index, control) -> bindMenuNavigate(index, control, false, false));
      builder.bind(this::bindClipControl);
      builder.bind((index, control) -> {
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
      });
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
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("Rpeat"));
         control.addDisplayValueBinding(index, repeat);
         control.addEncoderIncBinding(index, inc -> {
            repeat.increment(inc);
            deselectEnabled = false;
         }, true);
         control.addPressEncoderBinding(index, idx -> repeat.set(1));
         control.addRingBinding(index, recurrence);
      });
      builder.bind((index, control) -> bindStepValue(index, control, "Rp.Crv", repeatCurve));
      builder.bind((index, control) -> bindStepValue(index, control, "Rp.Vel", repeatVelocity));
      builder.bind((index, control) -> bindStepValue(index, control, "RpVEnd", repeatVelocityEnd));

      builder.fillRest();
      return menu;
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
            () -> handleSelection(index, pad));
         hwControls.bindButton(this, index, MixerSectionHardware.REC_INDEX, noteHandler.isPlaying(index), () -> {
         });
         final int mask = 0x1 << index;
         hwControls.bindButton(recurrenceLayer, index, MixerSectionHardware.REC_INDEX, () -> maskLighting(mask, index),
            () -> editMask(mask));
      }
   }

   private void handleSelection(final int index, final DrumPad pad) {
      if (control.getModifier().isClearSet()) {
         if (selectedPadIndex == index) {
            cursorClip.clearStepsAtY(0, 0);
         } else {
            cursorClip.scrollToKey(index + drumScrollOffset);
            cursorClip.clearStepsAtY(0, 0);
            cursorClip.scrollToKey(index + selectedPadIndex);
         }
      } else {
         control.handlePadSelection(pad);
      }
   }

   public boolean stepState(final int step) {
      final NoteStep.State state = assignments[step] == null ? NoteStep.State.Empty : assignments[step].state();
      final int steps = positionHandler.getAvailableSteps();
      //if (step < steps) {
      if (state == NoteStep.State.NoteOn) {
         if (copyNote != null && assignments[step] != null && assignments[step].x() == copyNote.x()) {
            return blinkTicks % 2 == 1;
         }
         return playingStep != step;
      } else {
         return playingStep == step;
      }
   }

   public void handleStepPressed(final int step) {
      heldSteps.add(step);
      final NoteStep note = assignments[step];
      if (copyNote != null) {
         RemoteConsole.out.println(" DUPLICATE ");
         handleNoteCopyAction(step, copyNote);
      } else if (note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain) {
         cursorClip.setStep(step, 0, velocity.get(), positionHandler.getGridResolution() * gatePercent);
         addedSteps.add(step);
      } else if (note.state() == NoteStep.State.NoteOn && control.getModifier().isDuplicateSet()) {
         copyNote = note;
      }
   }

   public void handleStepReleased(final int step) {
      final long diff = System.currentTimeMillis() - firstDown;
      final NoteStep note = assignments[step];
      final boolean doToggle = deselectEnabled && diff < 1000 && copyNote == null;
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

   private void handleNoteStep(final NoteStep noteStep) {
      final int newStep = noteStep.x();
      assignments[newStep] = noteStep;
      if (isActive()) {
         updateNotesSelected();
      }
      if (expectedNoteChanges.containsKey(newStep)) {
         final NoteStep previousStep = expectedNoteChanges.get(newStep);
         expectedNoteChanges.remove(newStep);
         applyValues(noteStep, previousStep);
      }
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
