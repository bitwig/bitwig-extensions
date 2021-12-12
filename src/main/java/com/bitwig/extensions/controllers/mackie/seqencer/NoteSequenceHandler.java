package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;

import java.util.List;


public class NoteSequenceHandler extends SequencerLayer {
   private static final int STEPS = 32;

   private MenuModeLayerConfiguration page1Menu;

   public NoteSequenceHandler(final String name, final MixControl mixControl, final NoteAssignment base) {
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
            final HardwareButton button = hwControls.getButton(row + 1, col);
//            bindStepButton(button, step);
         }
      }
      page1Menu = initPage1Menu();
      currentMenu = page1Menu;
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

   @Override
   public void nextMenu() {

   }

   @Override
   public void previousMenu() {

   }

   @Override
   List<NoteStep> getHeldNotes() {
      return List.of();
//      return heldSteps.stream()//
//         .map(idx -> assignments[idx])//
//         .filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn) //
//         .collect(Collectors.toList());
   }


   private void handleNoteStep(final NoteStep noteStep) {
      final int newStep = noteStep.x();
//      assignments[newStep] = noteStep;
//      if (isActive()) {
//         updateNotesSelected();
//      }
//      if (expectedNoteChanges.containsKey(newStep)) {
//         final NoteStep previousStep = expectedNoteChanges.get(newStep);
//         expectedNoteChanges.remove(newStep);
//         applyValues(noteStep, previousStep);
//      }
   }


}
