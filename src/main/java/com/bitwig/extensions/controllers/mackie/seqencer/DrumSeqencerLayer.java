package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.layer.ButtonLayer;
import com.bitwig.extensions.controllers.mackie.layer.DrumMixerLayerGroup;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.IntSetValue;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;
import com.bitwig.extensions.controllers.mackie.value.ValueSet;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DrumSeqencerLayer extends ButtonLayer {
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

   private final ValueSet gridResolution;
   private final IntValueObject pageIndex;


   public DrumSeqencerLayer(final MixControl mixControl) {
      super("DrumSeq_" + mixControl.getHwControls().getSectionIndex(), mixControl, BasicNoteOnAssignment.REC_BASE);
      driver = mixControl.getDriver();
      control = mixControl;
      gridResolution = new ValueSet().add("1/32", 0.125).add("1/16", 0.25).add("1/8", 0.5).add("1/8", 0.5).select(1);
      pageIndex = new IntValueObject(0, 0, 1);
   }

   public void init(final DrumPadBank drumPadBank, final DrumNoteHandler noteHandler) {
      final MixerSectionHardware hwControls = control.getHwControls();
      final int sectionIndex = hwControls.getSectionIndex();
      cursorClip = driver.getCursorTrack().createLauncherCursorClip(16, 1);
      positionHandler = new StepViewPosition(cursorClip, 16);
      drumPadBank.scrollPosition().addValueObserver(offset -> {
         drumScrollOffset = offset;
      });
      pageIndex.setMax(positionHandler.getPages() - 1);

      cursorClip.addNoteStepObserver(this::handleNoteStep);
      cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
      positionHandler.addPagesChangedCallback((index, pages) -> {
         pageIndex.setMax(pages - 1);
         pageIndex.set(index);
         RemoteConsole.out.println("PG = {} {}", index, pages);
      });
      gridResolution.addValueObserver(s -> positionHandler.setGridResolution(gridResolution.getValue()));
      pageIndex.addValueObserver((oldInt, newInt) -> positionHandler.setPage(newInt));

      initSelection(sectionIndex, hwControls, drumPadBank, noteHandler);

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 8; col++) {
            final int step = row * 8 + col;
            final HardwareButton button = hwControls.getButton(row + 1, col);
            bindStepButton(button, step);
         }
      }
      clipEditMenu = initClipMenu();
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

      }, formatter);
      builder.bindValue("Offset", pageIndex);
      builder.bindValueSet("Grid", gridResolution);


      builder.fillRest();

      return menu;
   }

   public MenuModeLayerConfiguration getMenu(final DrumMixerLayerGroup.EditorMode mode) {
      return clipEditMenu;
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
         hwControls.bindButton(this, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer, () -> {
            cursorClip.scrollToKey(drumScrollOffset + index);
            control.handlePadSelection(pad);
         });
         hwControls.bindButton(this, index, MixerSectionHardware.REC_INDEX, noteHandler.isPlaying(index), () -> {
         });
      }

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
         cursorClip.setStep(step, 0, 100, positionHandler.getGridResolution() * gatePercent);
         addedSteps.add(step);
      } else {

      }
   }

   public void handleStepReleased(final int step) {
      final NoteStep note = assignments[step];
      heldSteps.remove(step);
      if (note != null && note.state() == NoteStep.State.NoteOn && !addedSteps.contains(step)) {
         if (!modifiedSteps.contains(step)) {
            cursorClip.toggleStep(step, 0, 100);
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
      RemoteConsole.out.println("NOTES {}", newStep);
      if (expectedNoteChanges.containsKey(newStep)) {
         final NoteStep previousStep = expectedNoteChanges.get(newStep);
         expectedNoteChanges.remove(newStep);
//         applyValues(noteStep, previousStep);
      }
   }

   private void handlePlayingStep(final int playingStep) {
      if (playingStep == -1) {
         this.playingStep = -1;
      }
      this.playingStep = playingStep - positionHandler.getStepOffset();
   }


}
