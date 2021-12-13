package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public class NoteSequenceLayer extends SequencerLayer {
   public final static String[] NOTES = {" C", "C#", " D", "D#", " E", " F", "F#", " G", "G#", " A", "A#", " B"};
   private static final int STEPS = 32;
   protected final NoteStepSlot[] assignments = new NoteStepSlot[STEPS];
   private final EditorValue noteValue = new EditorValue(60, (edit, value) -> {
      final String base = NOTES[value % 12];
      final int octave = value / 12 - 2;
      if (edit) {
         return String.format("*%s%d", base, octave);
      }
      return String.format("<%s%d>", base, octave);
   });
   private final EditorValue velocityValue = new EditorValue(100,
      (edit, value) -> edit ? "* " + value : String.format("<%3d>", value));

   private final HashMap<Integer, NoteStep> expectedTranspose = new HashMap<>();

   private MenuModeLayerConfiguration page1Menu;

   public NoteSequenceLayer(final String name, final MixControl mixControl) {
      super("NoteSeq_" + mixControl.getHwControls().getSectionIndex(), mixControl, BasicNoteOnAssignment.REC_BASE);
      for (int i = 0; i < STEPS; i++) {
         assignments[i] = new NoteStepSlot(i);
      }
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
      initNoteValues();

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


   void initNoteValues() {
      noteValue.addIntValueObserver(value -> {
         if (!heldSteps.isEmpty()) {
            RemoteConsole.out.println(">> {}", value);
         }
      });
   }

   public boolean stepState(final int step) {
      final boolean hasStep = assignments[step].hasNotes();
      final int steps = positionHandler.getAvailableSteps();
      if (hasStep) {
//         if (copyNote != null && assignments[step] != null && assignments[step].x() == copyNote.x()) {
//            return blinkTicks % 2 == 1;
//         }
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
      if (!assignments[step].hasNotes()) {
         cursorClip.setStep(step, noteValue.getIntValue(), velocityValue.getIntValue(),
            positionHandler.getGridResolution() * gatePercent);
         noteValue.setEditValue(noteValue.getIntValue());
         velocityValue.setEditValue(noteValue.getIntValue());
         addedSteps.add(step);
      }

//      if (copyNote != null) {
//         handleNoteCopyAction(step, copyNote);
//      } else if (note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain) {
//         cursorClip.setStep(step, noteValue.getIntValue(), velocity.get(),
//            positionHandler.getGridResolution() * gatePercent);
//         addedSteps.add(step);
//      } else if (note.state() == NoteStep.State.NoteOn && control.getModifier().isDuplicateSet()) {
//         copyNote = note;
//      }
   }

   public void handleStepReleased(final int step) {
      final long diff = System.currentTimeMillis() - firstDown;

      final boolean doToggle = deselectEnabled && diff < 1000 && copyNote == null;
      if (assignments[step].hasNotes() && !addedSteps.contains(step)) {
         if (doToggle) {
            cursorClip.clearStepsAtX(0, step);
         }
      }
      addedSteps.remove(step);
      heldSteps.remove(step);
//      if (note != null && note.state() == NoteStep.State.NoteOn && !addedSteps.contains(step)) {
//         if (!modifiedSteps.contains(step)) {
//            if (doToggle) {
//               cursorClip.clearStepsAtX(0, step);
//            }
//         } else {
//            modifiedSteps.remove(step);
//         }
//      }
//      addedSteps.remove(step);
   }

   @Override
   void handleSelect() {
      getHeldNotes().stream().findFirst().ifPresent(note -> {
         noteValue.setEditValue(note.y());
         velocityValue.setEditValue((int) Math.round(127 * note.velocity()));
      });
   }

   @Override
   void handleReleased() {
      noteValue.exitEdit();
      velocityValue.exitEdit();
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
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("Vel"));
         control.addEncoderIncBinding(index, this::incrementVelocityValue, true);
         control.addDisplayValueBinding(index, velocityValue);
      });

      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("Note"));
         control.addEncoderIncBinding(index, this::incrementNoteValue, true);
         control.addDisplayValueBinding(index, noteValue);
      });
      builder.bind(this::bindNoteLength);
      builder.bind(this::bindClipControl);
      builder.bind((index, control) -> bindMenuNavigate(index, control, true, false));
      builder.fillRest();
      return menu;
   }

   private void incrementVelocityValue(final int increment) {
      if (!heldSteps.isEmpty()) {
         final List<NoteStep> notes = getHeldNotes();
         notes.forEach(note -> incrementVelocity(note, increment));
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


   private void incrementNoteValue(final int increment) {
      if (!heldSteps.isEmpty()) {
         final List<NoteStep> notes = getHeldNotes();
         notes.forEach(note -> transpose(note, increment));
      } else {
         noteValue.increment(increment);
      }
   }

   private void transpose(final NoteStep note, final int amount) {
      final int origX = note.x();
      final int origY = note.y();
      final int newY = origY + amount;
      if (newY >= 0 && newY < 128) {
         final int vx = origX << 8 | newY;
         expectedTranspose.put(vx, note);
         noteValue.setEditValue(newY);
         cursorClip.clearStep(0, origX, origY);
         cursorClip.setStep(origX, newY, (int) (note.velocity() * 127), note.duration());
      }
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
         .map(idx -> assignments[idx].steps())//
         .flatMap(Collection::stream) //
         .collect(Collectors.toList());
   }

   private void handleNoteStep(final NoteStep noteStep) {
      final int newStep = noteStep.x();
      final int xyIndex = noteStep.x() << 8 | noteStep.y();
      assignments[newStep].updateNote(noteStep);

      if (isActive()) {
         updateNotesSelected();
      }
      if (expectedTranspose.containsKey(xyIndex)) {
         final NoteStep previousStep = expectedTranspose.get(xyIndex);
         expectedNoteChanges.remove(xyIndex);
         applyValues(noteStep, previousStep);
      }
   }


}
