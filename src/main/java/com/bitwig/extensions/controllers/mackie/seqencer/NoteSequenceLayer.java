package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;

import java.util.List;
import java.util.stream.Collectors;


public class NoteSequenceLayer extends SequencerLayer {
   private static final int STEPS = 32;
   protected final NoteStep[] assignments = new NoteStep[STEPS];
   private final NoteValue noteValue = new NoteValue(60);

   private MenuModeLayerConfiguration page1Menu;

   public NoteSequenceLayer(final String name, final MixControl mixControl) {
      super("NoteSeq_" + mixControl.getHwControls().getSectionIndex(), mixControl, BasicNoteOnAssignment.REC_BASE);
   }

   public void init() {
      final MixerSectionHardware hwControls = control.getHwControls();
      cursorClip = getCursorTrack().createLauncherCursorClip(STEPS, 127);
      positionHandler = new StepViewPosition(cursorClip, STEPS);
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


      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 8; col++) {
            final int step = row * 8 + col;
            final HardwareButton button = hwControls.getButton(row, col);
            bindStepButton(button, step);
         }
      }
      page1Menu = initPage1Menu();
      currentMenu = page1Menu;
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

   private void bindStepButton(final HardwareButton button, final int step) {
      final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();

      bindPressed(button, () -> handleStepPressed(step));
      bindReleased(button, () -> handleStepReleased(step));
      bind(() -> stepState(step), light);
   }

   public void handleStepPressed(final int step) {
      heldSteps.add(step);
      final NoteStep note = assignments[step];
      if (copyNote != null) {
         handleNoteCopyAction(step, copyNote);
      } else if (note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain) {
         cursorClip.setStep(step, noteValue.getIntValue(), velocity.get(),
            positionHandler.getGridResolution() * gatePercent);
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
               cursorClip.clearStepsAtX(0, step);
            }
         } else {
            modifiedSteps.remove(step);
         }
      }
      addedSteps.remove(step);
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
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("Note"));
         control.addEncoderIncBinding(index, inc -> {
            noteValue.increment(inc);
         }, true);
         control.addDisplayValueBinding(index, noteValue);
      });
      builder.bind(this::bindClipControl);
      builder.bind((index, control) -> bindMenuNavigate(index, control, true, false));
      builder.fillRest();
      return menu;
   }

   @Override
   public void nextMenu() {

   }

   @Override
   public void previousMenu() {

   }

   @Override
   List<NoteStep> getHeldNotes() {
      return heldSteps.stream()//
         .map(idx -> assignments[idx])//
         .filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn) //
         .collect(Collectors.toList());
   }

   @Override
   protected void updateNotesSelected() {
      super.updateNotesSelected();
      if (!heldSteps.isEmpty()) {

      }
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


}
