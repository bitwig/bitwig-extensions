package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.NotePlayingSetup;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

import java.util.*;
import java.util.stream.Collectors;


public class NoteSequenceLayer extends SequencerLayer {
   public final static String[] NOTES = {" C", "C#", " D", "D#", " E", " F", "F#", " G", "G#", " A", "A#", " B"};
   private static final int STEPS = 16;
   protected final NoteStepSlot[] assignments = new NoteStepSlot[STEPS];
   private final EditorValue noteValue = new EditorValue(60, (edit, value) -> {
      final String base = NOTES[value % 12];
      final int octave = value / 12 - 2;
      if (edit) {
         return String.format("*%s%d", base, octave);
      }
      return String.format("<%s%d>", base, octave);
   });

   private final HashMap<Integer, NoteStep> expectedNoteChange = new HashMap<>();

   NoteStepSlot copyNote = null;
   private MenuModeLayerConfiguration page1Menu;
   private MenuModeLayerConfiguration page2Menu;
   private NotePlayingSetup notePlayingSetup;
   private TransposeMode mode = TransposeMode.SCALE;
   private final ChordHandler chordHandler = new ChordHandler();
   private final ChordBank chordBank = new ChordBank();

   private MenuModeLayerConfiguration page3Menu;
   private boolean prehearSteps = false;
   private Chord chordCopy;

   private enum TransposeMode {
      SCALE("s"),
      CHROMATIC("c"),
      OCTAVE("o");
      private final String symb;

      TransposeMode(final String symb) {
         this.symb = symb;
      }

      public String getSymb() {
         return symb;
      }

      public TransposeMode next() {
         switch (this) {
            case SCALE:
               return TransposeMode.CHROMATIC;
            case CHROMATIC:
               return TransposeMode.OCTAVE;
            case OCTAVE:
               return TransposeMode.SCALE;
         }
         return TransposeMode.SCALE;
      }
   }

   public NoteSequenceLayer(final String name, final MixControl mixControl) {
      super("NoteSeq_" + mixControl.getHwControls().getSectionIndex(), mixControl, BasicNoteOnAssignment.REC_BASE);
      for (int i = 0; i < STEPS; i++) {
         assignments[i] = new NoteStepSlot(i);
      }
      chordBank.init();
      control.getModifier().addValueObserver(this::handleModifierChanged);
      velocityValue.addIntValueObserver(vel -> {
         if (!velocityValue.isEdit()) {
            chordBank.get().setVelocity(vel);
         }
      });
   }

   private void handleModifierChanged(final ModifierValueObject modifierValueObject) {
      if (copyNote != null) {
         if (!modifierValueObject.isDuplicateSet()) {
            copyNote = null;
         }
      } else if (modifierValueObject.isDuplicateSet() && !heldSteps.isEmpty()) {
         getHeldNotes().stream()
            .findFirst()
            .map(noteStep -> assignments[noteStep.x()])
            .filter(NoteStepSlot::hasNotes)
            .ifPresent(slot -> copyNote = slot.copy());
      }
      if (chordCopy != null) {
         if (!modifierValueObject.isDuplicateSet()) {
            chordCopy = null;
         }
      }
      prehearSteps = modifierValueObject.isOption();
   }

   public void init(final NotePlayingSetup notePlayingSetup, final NoteInput noteInput) {
      final MixerSectionHardware hwControls = control.getHwControls();
      this.notePlayingSetup = notePlayingSetup;
      chordHandler.init(noteInput);
      cursorClip = getCursorTrack().createLauncherCursorClip(STEPS, 127);
//      getCursorTrack().clipLauncherSlotBank().cursorIndex().addValueObserver(v -> {
//         RemoteConsole.out.println(" SLOT {}", v);
//      });

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
      clipPlayStatus = createPlayingStatusValue(cursorClip, control.getModifier());
      initNoteValues();
      initSelection(hwControls);  // no good as long as the step sequencer uses all rows
      for (int row = 1; row < 3; row++) {
         for (int col = 0; col < 8; col++) {
            final int step = (row - 1) * 8 + col;
            final HardwareButton button = hwControls.getButton(row, col);
            bindStepButton(button, step);
         }
      }
      page1Menu = initPage1Menu();
      page2Menu = initPage2Menu();
      page3Menu = initPage3Menu();
      currentMenu = page1Menu;
      menuPageIndex.addValueObserver(this::setPageIndex);
   }

   private void setPageIndex(final int v) {
      switch (v) {
         case 0:
            currentMenu = page1Menu;
            break;
         case 1:
            currentMenu = page2Menu;
            break;
         case 2:
            currentMenu = page3Menu;
            break;
      }
      control.applyUpdate();
   }

   private void initSelection(final MixerSectionHardware hwControls) {
      for (int i = 0; i < 8; i++) {
         final int index = i;
         final int mask = 0x1 << index;
         hwControls.bindButton(this, index, MixerSectionHardware.REC_INDEX, () -> chordSlot(index),
            press -> selectChord(press, index));
         hwControls.bindButton(recurrenceLayer, index, MixerSectionHardware.REC_INDEX, () -> maskLighting(mask, index),
            () -> editMask(mask));
         hwControls.bindButton(this, index, MixerSectionHardware.SELECT_INDEX, () -> menuPageIndex.get() == index,
            press -> selectMenuPage(press, index));
      }
   }

   private boolean chordSlot(final int index) {
      if (chordCopy != null && chordCopy.getSlotIndex() == index) {
         return blinkTicks % 2 == 1;
      }
      return chordBank.getSelectedIndex().get() == index;
   }

   private void selectMenuPage(final boolean press, final int index) {
      if (menuPageIndex.inRange(index)) {
         menuPageIndex.set(index);
      }
   }

   private void selectChord(final boolean press, final int index) {
      if (control.getModifier().isDuplicateSet()) {
         if (press) {
            if (chordCopy != null) {
               final Chord destChord = chordBank.get(index);
               if (chordCopy.getSlotIndex() != destChord.getSlotIndex()) {
                  destChord.apply(chordCopy);
               }
            } else {
               chordCopy = chordBank.get(index).copy();
            }
         }
      } else {
         if (press) {
            chordBank.getSelectedIndex().set(index);
            chordHandler.apply(chordBank.get());
            velocityValue.set(chordBank.get().getVelocity());

            chordHandler.play(chordBank.get(index));
         } else {
            chordHandler.release(chordBank.get(index));
         }
      }
   }

   void initNoteValues() {
   }

   public boolean stepState(final int step) {
      final boolean hasStep = assignments[step].hasNotes();
      final int steps = positionHandler.getAvailableSteps();
      if (hasStep) {
         if (copyNote != null && assignments[step] != null && assignments[step].getSlotNr() == copyNote.getSlotNr()) {
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
      if (prehearSteps) {
         if (assignments[step].hasNotes()) {
            chordHandler.playNotes(assignments[step]);
         }
      } else if (copyNote != null) {
         handleNoteCopyAction(step, copyNote);
      } else if (control.getModifier().isClearSet()) {
         if (assignments[step].hasNotes() && !addedSteps.contains(step)) {
            cursorClip.clearStepsAtX(0, step);
         }
      } else {
         heldSteps.add(step);
         if (!assignments[step].hasNotes()) {
            placeNotes(step);
            noteValue.setEditValue(noteValue.getSetValue());
            velocityValue.setEditValue(noteValue.getSetValue());
            addedSteps.add(step);
         } else if (control.getModifier().isDuplicateSet()) {
            copyNote = assignments[step].copy();
            heldSteps.remove(step);
         }
      }
   }

   public void handleStepReleased(final int step) {
      final long diff = System.currentTimeMillis() - firstDown;
      chordHandler.release();
      if (control.getModifier().isClearSet() || copyNote != null) {
         heldSteps.remove(step);
         return;
      }

      final boolean doToggle = deselectEnabled && diff < 1000; // && copyNote != null
      if (assignments[step].hasNotes() && !addedSteps.contains(step)) {
         if (doToggle) {
            cursorClip.clearStepsAtX(0, step);
         }
      }
      addedSteps.remove(step);
      heldSteps.remove(step);
   }

   @Override
   void handleSelect() {
      getHeldNotes().stream().findFirst().ifPresent(note -> noteValue.setEditValue(note.y()));
   }

   private void placeNotes(final int step) {
      if (page3Menu.isActive()) {
         applyChordToStep(step, chordHandler.getNotes());
      } else {

         cursorClip.setStep(step, noteValue.getSetValue(), velocityValue.getSetValue(),
            positionHandler.getGridResolution() * gatePercent);
      }
   }

   private void applyChordToStep(final int step, final List<Integer> notes) {
      // TO Preserver values of chord as best possible
      cursorClip.clearStepsAtX(0, step);
      for (final Integer noteNr : notes) {
         cursorClip.setStep(step, noteNr, velocityValue.getSetValue(),
            positionHandler.getGridResolution() * gatePercent);
      }
   }


   private void handleNoteCopyAction(final int destinationIndex, final NoteStepSlot copyNote) {
      if (copyNote != null) {
         if (destinationIndex == copyNote.getSlotNr()) {
            return;
         }
         cursorClip.clearStepsAtX(0, destinationIndex);
         copyNote.steps().forEach(step -> copyNote(step, destinationIndex));
      }
   }

   private void copyNote(final NoteStep noteStep, final int destinationIndex) {
      final int vel = (int) Math.round(noteStep.velocity() * 127);
      final double duration = noteStep.duration();
      final int vx = destinationIndex << 8 | noteStep.y();
      expectedNoteChange.put(vx, noteStep);
      cursorClip.setStep(destinationIndex, noteStep.y(), vel, duration);
   }


   @Override
   void handleReleased() {
      noteValue.exitEdit();
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
         final BasicStringValue title = new BasicStringValue(mode.getSymb() + " Note");
         control.addNameBinding(index, title);
         control.addEncoderIncBinding(index, this::incrementNoteValue, false);
         control.addDisplayValueBinding(index, noteValue);
         control.addPressEncoderBinding(index, which -> {
            mode = mode.next();
            title.set(mode.getSymb() + " Note");
         });
      });
      builder.bind(this::bindVelocityValue);
      builder.bind(this::bindNoteLength);
      builder.bind((index, control) -> bindStepValue(index, control, "Vl.Spr", velSpread));
      builder.bind(this::bindClipControl);
      builder.fillRest();
      return menu;
   }

   private MenuModeLayerConfiguration initPage2Menu() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("STEP_CLIP_MENU", control);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bind((index, control) -> bindStepValue(index, control, "Timbre", timbre));
      builder.bind((index, control) -> bindStepValue(index, control, "Press", pressure));
      builder.bind((index, control) -> bindStepValue(index, control, "Chance", chance));
      builder.bind(this::bindRepeatValue);
      builder.bind(this::bindRecurrence);
      builder.bind(this::bindOccurrence);
      builder.bind(this::bindClipControl);
      builder.fillRest();
      return menu;
   }

   private MenuModeLayerConfiguration initPage3Menu() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("STEP_CLIP_MENU", control);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
//      builder.bind((index, control) -> bindMenuNavigate(index, control, false, true,
//         () -> control.getDriver().getActionSet().zoomToFitEditor()));
      builder.bindValue("Note", chordHandler.getChordBaseNote(), false);
      builder.bind((index, control) -> {
         control.addNameBinding(index, new BasicStringValue("Chord"));
         control.addEncoderIncBinding(index, chordHandler.getChordType(), false);
         control.addDisplayValueBinding(index, chordHandler.getChordType());
//         control.addRingBinding(index, chordHandler.getChordType());
         control.addPressEncoderBinding(index, which -> chordHandler.play(velocityValue.getSetValue()));
         control.addReleaseEncoderBinding(index, which -> chordHandler.release());
         control.addRingFixedBinding(index);
      });
      builder.bindValue("Octave", chordHandler.getOctaveOffset(), false);
      builder.bindValue("Inv", chordHandler.getInversion(), false);
      builder.bindValue("Exp", chordHandler.getExpansion(), false);
      builder.bind(this::bindVelocityValue);
      builder.bind(this::bindNoteLength);
      builder.bind(this::bindClipControl);

      chordHandler.getChordType().addValueObserver((old, chordType) -> {
         chordBank.get().setChordType(chordType);
         notifyChordChanged();
      });
      chordHandler.getChordBaseNote().addValueObserver(v -> {
         chordBank.get().setChordBaseNote(v);
         notifyChordChanged();
      });
      chordHandler.getInversion().addValueObserver(v -> {
         chordBank.get().setInversion(v);
         notifyChordChanged();
      });
      chordHandler.getExpansion().addIntValueObserver(v -> {
         chordBank.get().setExpansion(v);
         notifyChordChanged();
      });
      chordHandler.getOctaveOffset().addIntValueObserver(v -> {
         chordBank.get().setOctaveOffset(v);
         notifyChordChanged();
      });

      builder.fillRest();
      return menu;
   }

   private void notifyChordChanged() {
      if (!heldSteps.isEmpty()) {
         heldSteps.forEach(step -> applyChordToStep(step, chordHandler.getNotes()));
      }
   }

   private void incrementNoteValue(final int increment) {
      if (!heldSteps.isEmpty()) {
         final List<NoteStep> notes = getHeldNotes();

         handleTranspose(notes, increment);
         deselectEnabled = false;
      } else {
         if (mode == TransposeMode.SCALE) {
            noteValue.set(notePlayingSetup.transpose(noteValue.getSetValue(), increment));
         } else if (mode == TransposeMode.OCTAVE) {
            noteValue.increment(increment * 12);
         } else {
            noteValue.increment(increment);
         }
      }
   }

   static class NoteTranspose {
      NoteStep step;
      int destNote;

      public NoteTranspose(final NoteStep step, final int destNote) {
         this.step = step;
         this.destNote = destNote;
      }
   }

   private void handleTranspose(final List<NoteStep> notes, final int increment) {
      if (notes.isEmpty()) {
         return;
      }
      if (notes.size() == 1) {
         final int destNote = calcTranspose(notes.get(0).y(), mode == TransposeMode.OCTAVE ? increment * 12 : increment,
            mode == TransposeMode.SCALE);
         if (destNote != -1) {
            transpose(notes.get(0), destNote);
         }
      } else {
         final List<NoteTranspose> list = new ArrayList<>();

         for (final NoteStep step : notes) {
            final int destNote = calcTranspose(step.y(), mode == TransposeMode.OCTAVE ? increment * 12 : increment,
               mode == TransposeMode.SCALE);
            if (destNote != -1) {
               list.add(new NoteTranspose(step, destNote));
            }
         }

         if (increment > 0) {
            list.sort((t1, t2) -> t2.destNote - t1.destNote);
         } else {
            list.sort(Comparator.comparingInt(t -> t.destNote));
         }

         for (final NoteTranspose t : list) {
            transpose(t.step, t.destNote);
         }
      }
   }

   private int calcTranspose(final int origY, final int amount, final boolean scale) {
      final int newY = !scale ? origY + amount : notePlayingSetup.transpose(origY, amount);
      if (newY >= 0 && newY < 128) {
         return newY;
      }
      return -1;
   }

   private void transpose(final NoteStep note, final int destNote) {
      final int origX = note.x();
      final int origY = note.y();
      final int vx = origX << 8 | destNote;
      expectedNoteChange.put(vx, note);
      noteValue.setEditValue(destNote);
      cursorClip.moveStep(origX, origY, 0, destNote - origY);
   }

   @Override
   public void nextMenu() {
      if (!isActive()) {
         return;
      }
      menuPageIndex.increment(1);
      control.applyUpdate();
   }

   @Override
   public void previousMenu() {
      if (!isActive()) {
         return;
      }
      menuPageIndex.increment(-1);
      control.applyUpdate();
   }

   @Override
   List<NoteStep> getHeldNotes() {
      return heldSteps.stream()//
         .map(idx -> assignments[idx].steps())//
         .flatMap(Collection::stream) //
         .filter(ns -> ns.state() == NoteStep.State.NoteOn) //
         .collect(Collectors.toList());
   }

   private void handleNoteStep(final NoteStep noteStep) {
      final int newStep = noteStep.x();
      final int xyIndex = noteStep.x() << 8 | noteStep.y();
      assignments[newStep].updateNote(noteStep);

      if (isActive()) {
         updateNotesSelected();
      }
      final NoteStep previousStep = expectedNoteChange.get(xyIndex);
      if (previousStep != null) {
         expectedNoteChange.remove(xyIndex);
         applyValues(noteStep, previousStep);
      }
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      chordHandler.release();
      heldSteps.clear();
   }
}
