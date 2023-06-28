package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PadLayer extends Layer {
   private boolean hasDrumPads = true;
   private final RgbColor[] slotColors = new RgbColor[16];
   private final NoteInput noteInput;
   private final DrumPadBank drumPadBank;
   private RgbColor cursorTrackColor;
   private int padOffset = 36;
   protected final int[] noteToPad = new int[128];
   private final boolean[] isSelected = new boolean[16];
   protected final Integer[] deactivationTable = new Integer[128];
   protected final boolean[] playing = new boolean[16];
   protected final Integer[] noteTable = new Integer[128];

   public PadLayer(Layers layers, HwElements hwElements, ViewControl viewControl, MidiProcessor midiProcessor) {
      super(layers, "PAD_LAYER");
      Arrays.fill(slotColors, RgbColor.OFF);
      Arrays.fill(deactivationTable, Integer.valueOf(-1));
      Arrays.fill(noteTable, Integer.valueOf(-1));
      Arrays.fill(noteToPad, Integer.valueOf(-1));
      Arrays.fill(playing, false);
      this.noteInput = midiProcessor.getMidiIn().createNoteInput("PAD CONTROL");
      this.noteInput.setKeyTranslationTable(noteTable);
      drumPadBank = viewControl.getPrimaryDevice().createDrumPadBank(16);
      viewControl.getCursorTrack().color().addValueObserver((r, g, b) -> cursorTrackColor = RgbColor.toColor(r, g, b));
      DebugOutOxy.println(" Activate PADS FOR OXY");
      drumPadBank.setIndication(true);
      viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(hasPads -> this.hasDrumPads = hasPads);

      drumPadBank.scrollPosition().addValueObserver(scrollPos -> {
         padOffset = scrollPos;
         selectPad(getSelectedIndex());
         applyScale();
      });
      List<PadButton> padButtons = hwElements.getPadButtons();
      for (int i = 0; i < 16; i++) {
         PadButton button = padButtons.get(i);
         final int drumPadIndex = toLayout(i); //  (1 - (i / 8)) * 8 + i % 8;
         final DrumPad pad = drumPadBank.getItemAt(drumPadIndex);
         setUpPad(drumPadIndex, pad);
         button.bindLight(this, () -> computeGridLedState(drumPadIndex, pad));
      }
      viewControl.getCursorTrack().playingNotes().addValueObserver(this::handleNotePlaying);
   }

   private int toLayout(int padIndex) {
      final int col = padIndex % 8 / 4;
      final int row = (1 - (padIndex / 8));
      return row * 4 + padIndex % 4 + col * 8;
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
//         if (isSelected[index]) {
//            return color;
//         }
         return color; // TO this need to be done with lookup table
      } else {
         return RgbColor.OFF;
      }
   }

   private void handleNotePlaying(PlayingNote[] notes) {
      if (isActive()) {
         for (int i = 0; i < 16; i++) {
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
      // TODO noteFocusHandler.notifyDrumPadSelected(pad, padOffset, index);
   }

   private int getSelectedIndex() {
      for (int i = 0; i < 16; i++) {
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
      for (int i = 0; i < 16; i++) {
         final int drumPadIndex = toLayout(i);
         noteTable[HwElements.PAD_NOTE_NR[i]] = padOffset + drumPadIndex;
         noteToPad[padOffset + drumPadIndex] = drumPadIndex;
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
