package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChordLayer extends Layer {

   private final MidiProcessor midiProcessor;
   private final SettableEnumValue scaleFiltering;
   private final SettableStringValue chordData;

   static class Chord {
      private final int[] notes;

      private int basicOffset;

      public Chord(int... notes) {
         this.notes = notes;
      }

      public Chord(List<Integer> notes) {
         this.notes = new int[notes.size()];
         for (int i = 0; i < notes.size(); i++) {
            this.notes[i] = notes.get(i);
         }
      }

      public void setBasicOffset(int basicOffset) {
         this.basicOffset = basicOffset;
      }

      public int getBasicOffset() {
         return basicOffset;
      }

      public Chord copy() {
         Chord newChord = new Chord(notes);
         return newChord;
      }
   }

   private List<Chord> chords = List.of(new Chord(-1, 11, 14, 17, 21), new Chord(-8, 11, 14, 17, 20),
      new Chord(-3, 11, 12, 16, 19), new Chord(-7, 9, 12, 16, 19), new Chord(-10, 9, 12, 16, 17),
      new Chord(-2, 8, 12, 14, 17), new Chord(-3, 11, 12, 16, 19), new Chord(-6, 13, 18, 20), new Chord(-4, 14, 20, 23),
      new Chord(1, 8, 9, 13, 25), new Chord(2, 9, 18, 25), new Chord(-6, 13, 18, 20));


   private List<Chord> chords1 = List.of(new Chord(0, 7, 12, 16), new Chord(2, 9, 14, 17), new Chord(4, 7, 11, 16),
      new Chord(5, 9, 12, 17), new Chord(-5, 7, 11, 14), new Chord(-3, 9, 12, 16), new Chord(-1, 7, 11, 14),
      new Chord(-3, 7, 12, 16), new Chord(-3, 4, 11, 12, 19), new Chord(-10, 5, 9, 12, 16), new Chord(-5, 5, 11, 14),
      new Chord(2, 9, 17, 24));


   private List<Chord> chords2 = List.of(new Chord(0, 7, 9, 12, 24), new Chord(5, 12, 17, 21), new Chord(4, 12, 16, 21),
      new Chord(2, 14, 17, 21), new Chord(-7, 12, 17, 21), new Chord(-2, 14, 17, 22), new Chord(-5, 14, 19, 22),
      new Chord(-7, 12, 17, 19), new Chord(0, 12, 16, 19), new Chord(2, 14, 17, 21), new Chord(-3, 12, 16, 21),
      new Chord(2, 9, 17, 24));


   @Inject
   PadLayer padLayer;
   @Inject
   ModifierLayer modifierLayer;

   private Chord[] playing = new Chord[12];

   private int baseNote = 48;

   public ChordLayer(ControllerHost host, Layers layers, HwElements hwElements, MidiProcessor midiProcessor,
                     ViewControl viewControl) {
      super(layers, "CHORD_LAYER");

      this.midiProcessor = midiProcessor;
      DocumentState documentState = host.getDocumentState();
      scaleFiltering = documentState.getEnumSetting("Chord Filtering", //
         "Chord", new String[]{"No Filter", "Filter by Scale"}, "No Filter");
      scaleFiltering.markInterested();
      chordData = documentState.getStringSetting("Chord Data", "Chords", 2000, "");
//      baseNotesAssignment = documentState.getEnumSetting("Base Note", //
//         "Pads", baseNotes.stream().toArray(String[]::new), baseNotes.get(0));
//      baseNotesAssignment.addValueObserver(this::handleBaseNoteChanged);
//

      CursorTrack cursorTrack = viewControl.getCursorTrack();

      cursorTrack.playingNotes().addValueObserver(this::handleNotesIn);

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < 4; i++) {
         final int index = i;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this, () -> RgbColor.OFF);
         button.bindPressed(this, () -> {
         });
         button.bindRelease(this, () -> {
         });
      }
      for (int i = 4; i < 16; i++) {
         final int index = (3 - i / 4) * 4 + i % 4;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this,
            () -> playing[index] != null ? RgbColor.WHITE : RgbColor.GREEN.brightness(ColorBrightness.DIMMED));
         button.bindPressed(this, velocity -> handlePlayed(index, velocity));
         button.bindRelease(this, () -> handleReleased(index));
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindPressed(this, () -> handleEncoderPress(true));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindRelease(this, () -> handleEncoderPress(false));
   }

   private void handleNotesIn(PlayingNote[] notes) {
      if (!isActive()) {
         return;
      }
      if (notes.length > 2) {
         List<Integer> noteValues = new ArrayList<>();
         for (int i = 0; i < notes.length; i++) {
            noteValues.add(notes[i].pitch() - baseNote);
         }
         String v = noteValues.stream().sorted().map(Object::toString).collect(Collectors.joining(","));
         MaschineMikroExtension.println("new Chord (%s)", v);
      }
   }

   private void handleEncoder(int dir) {
      if (modifiedPlaying(dir)) {
         return;
      }

      int newBaseNote = -1;
      if (modifierLayer.getShiftHeld().get()) {
         newBaseNote = baseNote + dir * 12;
      } else {
         newBaseNote = baseNote + dir;
      }
      if (newBaseNote >= 12 && newBaseNote < 112) {
         baseNote = newBaseNote;
      }
   }

   private boolean modifiedPlaying(int dir) {
      boolean modifed = false;
      for (int i = 0; i < 12; i++) {
         if (playing[i] != null) {
            handleReleased(i);
            Chord chord = chords.get(i);
            int offset = chord.getBasicOffset() + dir;
            if (offset >= -36 && offset <= 36) {
               chord.setBasicOffset(offset);
            }
            Chord notesChord = toNotesChord(chord);
            playChord(notesChord, 0.5);
            playing[i] = notesChord;
            modifed = true;
         }
      }
      return modifed;
   }

   private void handleEncoderPress(boolean pressed) {
      if (!pressed) {
         return;
      }
      if (scaleFiltering.get().equals("No Filter")) {
         scaleFiltering.set("Filter by Scale");
      } else {
         scaleFiltering.set("No Filter");
      }
   }

   void handlePlayed(int index, double velocity) {
      Chord notesChord = toNotesChord(chords.get(index));

      playChord(notesChord, velocity);
      playing[index] = notesChord;
   }

   void handleReleased(int index) {
      if (playing[index] == null) {
         return;
      }
      releaseChord(playing[index]);

      playing[index] = null;
   }

   private Chord toNotesChord(Chord chord) {
      List<Integer> notes = new ArrayList<>();
      int offset = baseNote + chord.getBasicOffset();
      for (int note : chord.notes) {
         int playedNote = scaleFiltering.get().equals("No Filter") ? note + offset : padLayer.matchScale(note + offset);
         if (playedNote >= 0 && playedNote < 128) {
            notes.add(playedNote);
         }
      }
      return new Chord(notes);
   }

   private void playChord(Chord chord, double velocity) {
      int intVelocity = (int) (velocity * 127);
      //MaschineMikroExtension.println(" Play %s vel=%d", Arrays.toString(chord.notes), intVelocity);
      for (int note : chord.notes) {
         midiProcessor.sendRawNoteOn(note, intVelocity);
      }
   }

   private void releaseChord(Chord chord) {
      for (int note : chord.notes) {
         midiProcessor.sendNoteOff(note);
      }
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      midiProcessor.getNoteInput().setShouldConsumeEvents(false);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      midiProcessor.getNoteInput().setShouldConsumeEvents(true);
   }
}
