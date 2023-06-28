package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PadLayer extends Layer {
   private final DrumPadBank drumPadBank;
   private boolean hasDrumPads = true;
   private final NoteInput noteInput;
   private RgbColor cursorTrackColor;
   private final Layer muteLayer;
   private final Layer soloLayer;
   private final Layer eraseLayer;
   private int padOffset = 36;
   protected final int[] noteToPad = new int[128];
   private final boolean[] isSelected = new boolean[16];
   protected final Integer[] deactivationTable = new Integer[128];
   protected final boolean[] playing = new boolean[16];
   protected final Integer[] noteTable = new Integer[128];
   private final RgbColor[] padColors = new RgbColor[16];

   public PadLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                   MidiProcessor midiProcessor) {
      super(layers, "PAD_LAYER");
      this.noteInput = midiProcessor.getNoteInput();
      muteLayer = new Layer(layers, "Drum-mute");
      soloLayer = new Layer(layers, "Drum-solo");
      eraseLayer = new Layer(layers, "Drum-erase");

      Arrays.fill(padColors, RgbColor.OFF);
      Arrays.fill(deactivationTable, Integer.valueOf(-1));
      Arrays.fill(noteTable, Integer.valueOf(-1));
      Arrays.fill(noteToPad, Integer.valueOf(-1));
      Arrays.fill(playing, false);

      drumPadBank = viewControl.getPrimaryDevice().createDrumPadBank(16);
      viewControl.getCursorTrack().color().addValueObserver((r, g, b) -> cursorTrackColor = RgbColor.toColor(r, g, b));

      drumPadBank.setIndication(true);

      drumPadBank.scrollPosition().addValueObserver(scrollPos -> {
         padOffset = scrollPos;
         selectPad(getSelectedIndex());
         applyScale();
      });

      List<RgbButton> padButtons = hwElements.getPadButtons();
      for (int i = 0; i < 16; i++) {
         RgbButton button = padButtons.get(i);
         final int drumPadIndex = (3 - i / 4) * 4 + i % 4;
         final DrumPad pad = drumPadBank.getItemAt(drumPadIndex);
         setUpPad(drumPadIndex, pad);
         button.bindLight(this, () -> computeGridLedState(drumPadIndex, pad));
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));

      viewControl.getCursorTrack().playingNotes().addValueObserver(this::handleNotePlaying);
   }

   private void handleEncoder(int dir) {
      drumPadBank.scrollBy(-dir * 4);
   }

   private void setUpPad(int index, DrumPad pad) {
      pad.color().addValueObserver((r, g, b) -> padColors[index] = RgbColor.toColor(r, g, b));
      pad.name().markInterested();
      pad.exists().markInterested();
      pad.solo().markInterested();
      pad.mute().markInterested();
      pad.addIsSelectedInEditorObserver(selected -> {
         if (selected) {
            // TODO noteFocusHandler.notifyDrumPadSelected(pad, padOffset, index);
            isSelected[index] = true;
         } else {
            isSelected[index] = false;
         }
      });
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

   private RgbColor computeGridLedState(final int index, final DrumPad pad) {
      if (hasDrumPads) {
         RgbColor color = padColors[index];
         if (playing[index]) {
            if (isSelected[index]) {
               return RgbColor.WHITE.brightness(ColorBrightness.BRIGHT);
            }
            return color.brightness(ColorBrightness.BRIGHT);
         }
         if (isSelected[index]) {
            return color.brightness(ColorBrightness.DIMMED);
         }
         return color; // TO this need to be done with lookup table
      } else {
         return RgbColor.OFF;
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
         noteTable[i + PadButton.PAD_NOTE_OFFSET] = padOffset + i;
         noteToPad[padOffset + i] = i;
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
      DebugOutMk.println(" Deaktivate Drum");
      Arrays.fill(noteTable, -1);
      noteInput.setKeyTranslationTable(noteTable);
   }
}
