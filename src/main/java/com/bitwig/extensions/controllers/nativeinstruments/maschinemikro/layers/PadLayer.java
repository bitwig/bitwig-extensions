package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.PadScaleHandler;
import com.bitwig.extensions.framework.values.Scale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PadLayer extends Layer {
   private final DrumPadBank drumPadBank;
   private final PadScaleHandler scaleHandler;
   private final Arpeggiator arp;
   private boolean hasDrumPads = true;
   private BooleanValueObject inDrumMode = new BooleanValueObject();
   private final NoteInput noteInput;
   private RgbColor cursorTrackColor;
   private final Layer muteLayer;
   private final Layer soloLayer;
   private final Layer eraseLayer;
   private int padOffset = 36;
   private int currentArpRate = 2;

   private final double[] arpRateTable = {0.0625, 0.125, 0.25, 0.5, 1.0, 2.0, 4.0};
   private final String[] rateDisplayValues = { "1/64", "1/32", "1/16", "1/8", "1/4", "1/2", "1/1" };
   private SettableEnumValue arpRate;
   private boolean noteRepeatActive;
   private SettableEnumValue arpMode;
   protected final int[] noteToPad = new int[128];
   protected final Integer[] deactivationTable = new Integer[128];
   private final Integer[] noteTable = new Integer[128];

   private final boolean[] isSelected = new boolean[16];
   private final boolean[] isBaseNote = new boolean[16];
   private final boolean[] playing = new boolean[16];
   private final boolean[] assigned = new boolean[16];
   private final RgbColor[] padColors = new RgbColor[16];

   public PadLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                   MidiProcessor midiProcessor, ControllerHost host) {
      super(layers, "PAD_LAYER");
      this.noteInput = midiProcessor.getNoteInput();
      scaleHandler = new PadScaleHandler(host,
         List.of(Scale.CHROMATIC, Scale.MAJOR, Scale.MINOR, Scale.PENTATONIC, Scale.PENTATONIC_MINOR, Scale.DORIAN,
            Scale.MIXOLYDIAN, Scale.LOCRIAN, Scale.LYDIAN, Scale.PHRYGIAN), 16, true);
      scaleHandler.addStateChangedListener(this::handleScaleChange);
      arp = noteInput.arpeggiator();
      initArp(host);

      muteLayer = new Layer(layers, "Drum-mute");
      soloLayer = new Layer(layers, "Drum-solo");
      eraseLayer = new Layer(layers, "Drum-erase");

      Arrays.fill(padColors, RgbColor.OFF);
      Arrays.fill(deactivationTable, Integer.valueOf(-1));
      Arrays.fill(noteTable, Integer.valueOf(-1));
      Arrays.fill(noteToPad, Integer.valueOf(-1));

      drumPadBank = viewControl.getPrimaryDevice().createDrumPadBank(16);
      viewControl.getCursorTrack().color().addValueObserver((r, g, b) -> cursorTrackColor = RgbColor.toColor(r, g, b));
      viewControl.getPrimaryDevice().hasDrumPads().addValueObserver(this::handleHasDrumPadsChanged);

      drumPadBank.setIndication(true);

      drumPadBank.scrollPosition().addValueObserver(this::handlePadBankScrolling);

      List<RgbButton> padButtons = hwElements.getPadButtons();
      for (int i = 0; i < 16; i++) {
         RgbButton button = padButtons.get(i);
         final int drumPadIndex = (3 - i / 4) * 4 + i % 4;
         final DrumPad pad = drumPadBank.getItemAt(drumPadIndex);
         setUpPad(drumPadIndex, pad);
         button.bindLight(this, () -> computeGridLedState(drumPadIndex, pad));
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindPressed(this, () -> handleEncoderPress(true));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindRelease(this, () -> handleEncoderPress(false));
   
      viewControl.getCursorTrack().playingNotes().addValueObserver(this::handleNotePlaying);
   }
   
   private void handleEncoderPress(final boolean press) {
      // TODO Arp Type
   }
   
   private void initArp(ControllerHost host) {
      arp.usePressureToVelocity().markInterested();
      arp.usePressureToVelocity().set(true);
      arp.octaves().markInterested();
      arp.rate().markInterested();
      arp.mode().markInterested();
      arp.rate().set(arpRateTable[currentArpRate]);

      List<String> arpModes = new ArrayList<>();
      final EnumDefinition modes = arp.mode().enumDefinition();
      for (int i = 0; i < modes.getValueCount(); i++) {
         final EnumValueDefinition valDef = modes.valueDefinitionAt(i);
         arpModes.add(valDef.getId().replace("_","-"));
      }
      DocumentState documentState = host.getDocumentState();
   
      arpRate = documentState.getEnumSetting("Arp/Note Repeat Rate", //
          "Arp/Note Repeat", rateDisplayValues, rateDisplayValues[currentArpRate]);
      arpRate.addValueObserver(this::handleArpRateChanged);
      arpMode = documentState.getEnumSetting("Arp/Note Repeat Mode", //
          "Arp/Note Repeat", arpModes.stream().toArray(String[]::new), arpModes.get(1));
      arpMode.addValueObserver(mode ->this.handleArpModeChanged(mode, arpModes));
   }
   
   private void handleArpModeChanged(final String mode, List<String> arpModes) {
      int index = arpModes.indexOf(mode);
      if(index != -1) {
         arp.mode().set(arpModes.get(index));
      }
   }
   
   private void handleArpRateChanged(final String rate) {
      int index = -1;
      for(int i=0;i<rateDisplayValues.length;i++) {
         if(rateDisplayValues[i].equals(rate)) {
            index = i;
            break;
         }
      }
      if(index != -1) {
         currentArpRate = index;
         arp.rate().set(arpRateTable[currentArpRate]);
      }
   }
   
   public BooleanValueObject getInDrumMode() {
      return inDrumMode;
   }

   private void handlePadBankScrolling(int scrollPos) {
      padOffset = scrollPos;
      selectPad(getSelectedIndex());
      if (isActive()) {
         applyScale();
      }
   }

   public void enableNoteRepeat(boolean noteRepeatActive) {
      if (noteRepeatActive) { // arp.mode()
         if (inDrumMode.get()) {
            arp.mode().set("all"); // that's the note repeat way
            arp.octaves().set(0);
            arp.humanize().set(0);
            arp.isFreeRunning().set(false);
         } else {
            arp.octaves().set(0);
            arp.humanize().set(0);
            arp.isFreeRunning().set(false);
            arp.mode().set("up");
         }
         arp.rate().set(arpRateTable[currentArpRate]);
         arp.isEnabled().set(true);
      } else {
         arp.isEnabled().set(false);
      }
      this.noteRepeatActive = noteRepeatActive;
   }

   private void handleEncoder(int dir) {
      if(noteRepeatActive) {
         int newRate = currentArpRate - dir;
         if(newRate >= 0 && newRate < arpRateTable.length) {
            currentArpRate = newRate;
            arpRate.set(rateDisplayValues[currentArpRate]);
         }
      } else if (inDrumMode.get()) {
         drumPadBank.scrollBy(4 * dir);
      } else {
         scaleHandler.incrementNoteOffset(dir);
//         if (encoderPressed.get()) {
//            scaleHandler.incScaleSelection(dir);
//         } else {
//         }
      }
   }

   private void handleHasDrumPadsChanged(boolean hasDrumPads) {
      this.hasDrumPads = hasDrumPads;
      inDrumMode.set(hasDrumPads);
      if (isActive()) {
         applyScale();
      }
   }

   private void handleScaleChange() {
      if (isActive() && !inDrumMode.get()) {
         applyScale();
      }
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
      if (inDrumMode.get()) {
         if (hasDrumPads) {
            return getPadColors(index);
         } else {
            RgbColor color = cursorTrackColor;
            if (playing[index]) {
               return color.brightness(ColorBrightness.SUPERBRIGHT);
            }
            return color.brightness(ColorBrightness.DIMMED);
         }
      } else {
         if (!assigned[index]) {
            return RgbColor.OFF;
         }
         RgbColor color = isBaseNote[index] ? RgbColor.GREEN : cursorTrackColor;
         if (playing[index]) {
            return color.brightness(ColorBrightness.SUPERBRIGHT);
         }
         return color.brightness(ColorBrightness.DIMMED);
      }
   }

   private RgbColor getPadColors(int index) {
      RgbColor color = padColors[index];
      if (isSelected[index]) {
         if (playing[index]) {
            return RgbColor.WHITE.brightness(ColorBrightness.BRIGHT);
         }
         return color.brightness(ColorBrightness.BRIGHT);
      } else {
         if (playing[index]) {
            return color.brightness(ColorBrightness.SUPERBRIGHT);
         }
         return color.brightness(ColorBrightness.DIMMED);
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
      Arrays.fill(noteToPad, -1);
      if (inDrumMode.get()) {
         for (int i = 0; i < 16; i++) {
            noteTable[i + PadButton.PAD_NOTE_OFFSET] = padOffset + i;
            noteToPad[padOffset + i] = i;
         }
      } else {
         final int startNote = scaleHandler.getStartNote(); //baseNote;
         final int[] intervals = scaleHandler.getCurrentScale().getIntervals();
         for (int i = 0; i < 16; i++) {
            final int drumPadIndex = i;
            final int index = drumPadIndex % intervals.length;
            final int oct = drumPadIndex / intervals.length;
            int note = startNote + intervals[index] + 12 * oct;
            note = note < 0 || note > 127 ? -1 : note;
            if (note < 0 || note > 127) {
               noteTable[i + PadButton.PAD_NOTE_OFFSET] = -1;
               isBaseNote[drumPadIndex] = false;
               assigned[drumPadIndex] = false;
            } else {
               noteTable[i + PadButton.PAD_NOTE_OFFSET] = note;
               noteToPad[note] = drumPadIndex;
               isBaseNote[drumPadIndex] = note % 12 == scaleHandler.getBaseNote();
               assigned[drumPadIndex] = true;
            }
         }

      }
      if (isActive()) {
         noteInput.setKeyTranslationTable(noteTable);
      }
   }

   public void forceModeSwitch() {
      inDrumMode.toggle();
      if (isActive()) {
         applyScale();
      }
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

   public void setNotesActive(boolean notesActive) {
      if (notesActive) {
         applyScale();
         noteInput.setKeyTranslationTable(noteTable);
      } else {
         Arrays.fill(noteTable, -1);
         noteInput.setKeyTranslationTable(noteTable);
      }
   }


}
