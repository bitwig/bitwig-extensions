package com.bitwig.extensions.controllers.maudio.oxygenpro.modes;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.definition.BasicMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.PadScaleHandler;
import com.bitwig.extensions.framework.values.Scale;

import java.util.Arrays;
import java.util.List;

@Component
public class PadLayer extends Layer {
   private final int nrOfPads;
   private final PadScaleHandler scaleHandler;
   private final PinnableCursorDevice cursorDevice;
   private boolean hasDrumPads = true;
   private final RgbColor[] slotColors = new RgbColor[16];
   private final NoteInput noteInput;
   private final DrumPadBank drumPadBank;
   private RgbColor cursorTrackColor;
   private int padOffset = 36;
   protected final int[] noteToPad = new int[128];
   private final Integer[] noteTable = new Integer[128];

   private final boolean[] isSelected = new boolean[16];
   private final boolean[] isBaseNote = new boolean[16];
   private final boolean[] playing = new boolean[16];
   private final boolean[] assigned = new boolean[16];

   private boolean backButtonHeld = false;
   private ModeHandler modeHandler;


   public PadLayer(Layers layers, HwElements hwElements, ViewControl viewControl, MidiProcessor midiProcessor,
                   OxyConfig config, ControllerHost host) {
      super(layers, "PAD_LAYER");
      nrOfPads = config.getNumberOfControls() * 2;
      scaleHandler = new PadScaleHandler(host,
         List.of(Scale.CHROMATIC, Scale.MAJOR, Scale.MINOR, Scale.PENTATONIC, Scale.PENTATONIC_MINOR), nrOfPads, true);
      scaleHandler.addStateChangedListener(this::handleScaleChange);
      Arrays.fill(slotColors, RgbColor.OFF);
      Arrays.fill(noteTable, Integer.valueOf(-1));
      Arrays.fill(noteToPad, Integer.valueOf(-1));
      this.noteInput = midiProcessor.getMidiIn().createNoteInput("PAD CONTROL");
      this.noteInput.setKeyTranslationTable(noteTable);
      drumPadBank = viewControl.getPrimaryDevice().createDrumPadBank(nrOfPads);
      viewControl.getCursorTrack().color().addValueObserver((r, g, b) -> cursorTrackColor = RgbColor.toColor(r, g, b));
      drumPadBank.setIndication(true);
      viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(this::handleHasDrumPadsChanged);
      this.cursorDevice = viewControl.getCursorDevice();

      drumPadBank.scrollPosition().addValueObserver(scrollPos -> {
         padOffset = scrollPos;
         selectPad(getSelectedIndex());
         applyScale();
      });
      List<PadButton> padButtons = hwElements.getPadButtons();
      for (int i = 0; i < nrOfPads; i++) {
         PadButton button = padButtons.get(i);
         final int drumPadIndex = toLayout(i); //  (1 - (i / 8)) * 8 + i % 8;
         final DrumPad pad = drumPadBank.getItemAt(drumPadIndex);
         setUpPad(drumPadIndex, pad);
         button.bindLight(this, () -> computeGridLedState(drumPadIndex, pad));
      }
      CursorRemoteControlsPage parameterBank = viewControl.getParameterBank();
      viewControl.getCursorTrack().playingNotes().addValueObserver(this::handleNotePlaying);
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), this::handleEncoder);
      hwElements.getButton(OxygenCcAssignments.ENCODER_PUSH).bindIsPressed(this, this::handleEncoderPressed);
      hwElements.getButton(OxygenCcAssignments.BANK_LEFT).bindPressed(this, () -> handleBankLeft(parameterBank));
      hwElements.getButton(OxygenCcAssignments.BANK_RIGHT).bindPressed(this, () -> handleBankRight(parameterBank));
   }

   private void handleEncoderPressed(boolean pressed) {
      if (modeHandler != null && pressed && isActive()) {
         modeHandler.changeMode(BasicMode.CLIP_LAUNCH);
      }
   }

   public void setBackButtonHeld(boolean isHeld) {
      this.backButtonHeld = isHeld;
   }

   private void handleBankLeft(CursorRemoteControlsPage parameterBank) {
      if (backButtonHeld) {
         cursorDevice.selectPrevious();
      } else {
         parameterBank.selectPrevious();
      }
   }

   private void handleBankRight(CursorRemoteControlsPage parameterBank) {
      if (backButtonHeld) {
         cursorDevice.selectNext();
      } else {
         parameterBank.selectNext();
      }
   }

   private void handleHasDrumPadsChanged(boolean hasDrumPads) {
      this.hasDrumPads = hasDrumPads;
      if (isActive()) {
         applyScale();
      }
   }

   private void handleScaleChange() {
      if (isActive() && !hasDrumPads) {
         applyScale();
      }
   }

   public void registerModeHandler(ModeHandler modeHandler) {
      this.modeHandler = modeHandler;
   }


   private void handleEncoder(int dir) {
      if (hasDrumPads) {
         drumPadBank.scrollBy(-4 * dir);
      } else {
         if (backButtonHeld) {
            scaleHandler.incScaleSelection(dir);
         } else {
            scaleHandler.incrementNoteOffset(dir);
         }
      }
   }

   private int toLayout(int padIndex) {
      if (nrOfPads > 8) {
         final int col = padIndex % 8 / 4;
         final int row = (1 - (padIndex / 8));
         return row * 4 + padIndex % 4 + col * 8;
      } else {
         return (1 - (padIndex / 4)) * 4 + padIndex % 4;
      }
   }

   private int toNoteLayout(int padIndex) {
      if (nrOfPads > 8) {
         final int col = padIndex % 8;
         final int row = (1 - (padIndex / 8));
         return row * 8 + col;
      } else {
         return (1 - (padIndex / 4)) * 4 + padIndex % 4;
      }
   }

   private void setUpPad(int index, DrumPad pad) {
      pad.color().addValueObserver((r, g, b) -> slotColors[index] = RgbColor.toColor(r, g, b));
      pad.name().markInterested();
      pad.exists().markInterested();
      pad.solo().markInterested();
      pad.mute().markInterested();
      pad.addIsSelectedInEditorObserver(selected -> {
         if (selected) {
            isSelected[index] = true;
         } else {
            isSelected[index] = false;
         }
      });
   }

   private RgbColor computeGridLedState(final int index, final DrumPad pad) {
      if (hasDrumPads) {
         RgbColor color = slotColors[index] == RgbColor.OFF ? cursorTrackColor : slotColors[index];
         if (!pad.exists().get()) {
            color = RgbColor.OFF;
         }
         if (playing[index]) {
            return RgbColor.WHITE;
         }
         return color;
      } else {
         if (playing[index]) {
            return RgbColor.WHITE;
         }
         if (isBaseNote[index]) {
            return RgbColor.GREEN;
         }
         if (!assigned[index]) {
            return RgbColor.OFF;
         }
         return cursorTrackColor;
      }
   }

   private void handleNotePlaying(PlayingNote[] notes) {
      if (isActive()) {
         for (int i = 0; i < nrOfPads; i++) {
            playing[i] = false;
         }
         for (final PlayingNote playingNote : notes) {
            final int padIndex = noteToPad[playingNote.pitch()];
            if (padIndex != -1) {
               playing[padIndex] = true;
            }
         }
      }
   }

   void selectPad(final int index) {
      final DrumPad pad = drumPadBank.getItemAt(index);
      pad.selectInEditor();
   }

   private int getSelectedIndex() {
      for (int i = 0; i < nrOfPads; i++) {
         if (isSelected[i]) {
            return i;
         }
      }
      return 0;
   }

   void applyScale() {
      if (!isActive()) {
         return;
      }
      Arrays.fill(noteToPad, -1);
      if (hasDrumPads) {
         for (int i = 0; i < nrOfPads; i++) {
            final int drumPadIndex = toLayout(i);
            noteTable[HwElements.PAD_NOTE_NR[i]] = padOffset + drumPadIndex;
            noteToPad[padOffset + drumPadIndex] = drumPadIndex;
            assigned[i] = true;
         }
      } else {
         final int startNote = scaleHandler.getStartNote(); //baseNote;
         final int[] intervals = scaleHandler.getCurrentScale().getIntervals();

         for (int i = 0; i < nrOfPads; i++) {
            //final int drumPadIndex = toNoteLayout(i);
            final int noteIndex = toNoteLayout(i);
            final int drumPadIndex = toLayout(i);
            final int index = noteIndex % intervals.length;
            final int oct = noteIndex / intervals.length;
            int note = startNote + intervals[index] + 12 * oct;
            note = note < 0 || note > 127 ? -1 : note;
            if (note < 0 || note > 127) {
               noteTable[HwElements.PAD_NOTE_NR[i]] = -1;
               isBaseNote[drumPadIndex] = false;
               assigned[drumPadIndex] = false;
            } else {
               noteTable[HwElements.PAD_NOTE_NR[i]] = note;
               noteToPad[note] = drumPadIndex;
               isBaseNote[drumPadIndex] = scaleHandler.isBaseNote(note);
               assigned[drumPadIndex] = true;
            }
         }
      }
      noteInput.setKeyTranslationTable(noteTable);
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      applyScale();
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      Arrays.fill(noteTable, -1);
      noteInput.setKeyTranslationTable(noteTable);
   }
}
