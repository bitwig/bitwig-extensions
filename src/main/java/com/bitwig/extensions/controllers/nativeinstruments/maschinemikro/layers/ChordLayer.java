package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChordLayer extends Layer {

   private final MidiProcessor midiProcessor;
   private final SettableStringValue chordData;
   private boolean filterByScale = false;
   private boolean recordingModeActive = false;
   private int recordingIndex = 0;

   static class Chord {
      private final int[] notes;

      private int basicOffset;

      public Chord(int... notes) {
         this.notes = notes;
      }

      public Chord(Collection<Integer> notes) {
         this.notes = notes.stream().mapToInt(Integer::intValue).toArray();
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

      public String serialized() {
         return Arrays.stream(notes).mapToObj(Integer::toString).collect(Collectors.joining(","));
      }
   }

   private List<Chord> chords = List.of(new Chord(0, 12, 16, 19), new Chord(7, 19, 23, 26), new Chord(5, 17, 21, 24),
      new Chord(4, 16, 19, 23), new Chord(2, 14, 17, 21), new Chord(-3, 4, 9, 14, 17, 23), new Chord(9, 14, 17, 23),
      new Chord(-3, 9, 21, 24, 28), new Chord(9, 21, 24, 28), new Chord(0, 12, 14, 19), new Chord(0, 16, 19, 23),
      new Chord(0, 12, 16, 19, 23), new Chord(0, 12, 16, 23)).stream().collect(Collectors.toList());


   private PadLayer padLayer;
   @Inject
   private ModifierLayer modifierLayer;

   private Chord[] playing = new Chord[12];

   private int baseNote = 48;

   public ChordLayer(ControllerHost host, Layers layers, HwElements hwElements, MidiProcessor midiProcessor,
                     ViewControl viewControl) {
      super(layers, "CHORD_LAYER");

      this.midiProcessor = midiProcessor;
      DocumentState documentState = host.getDocumentState();
      chordData = documentState.getStringSetting("Chord Data", "Chords", 2000, "");
      chordData.addValueObserver(this::deserializeChordData);
//      baseNotesAssignment = documentState.getEnumSetting("Base Note", //
//         "Pads", baseNotes.stream().toArray(String[]::new), baseNotes.get(0));
//      baseNotesAssignment.addValueObserver(this::handleBaseNoteChanged);
//
      CursorTrack cursorTrack = viewControl.getCursorTrack();

      cursorTrack.playingNotes().addValueObserver(this::handleNotesIn);

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      RgbButton scaleLockButton = gridButtons.get(3);
      scaleLockButton.bindLight(this,
         () -> filterByScale ? RgbColor.of(Colors.ORANGE, ColorBrightness.BRIGHT) : RgbColor.of(Colors.ORANGE,
            ColorBrightness.DIMMED));
      scaleLockButton.bindPressed(this, () -> filterByScale = !filterByScale);
      RgbButton scaleRecordButton = gridButtons.get(0);
      scaleRecordButton.bindLight(this,
         () -> recordingModeActive ? RgbColor.RED.brightness(ColorBrightness.BRIGHT) : RgbColor.RED.brightness(
            ColorBrightness.DIMMED));
      scaleRecordButton.bindPressed(this, () -> {
         recordingModeActive = !recordingModeActive;
      });

      for (int i = 1; i < 3; i++) {
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
         button.bindLight(this, () -> getPadLight(index));
         button.bindPressed(this, velocity -> handlePlayed(index, velocity));
         button.bindRelease(this, () -> handleReleased(index));
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindIsPressed(this, this::handleEncoderPress);
      hwElements.getButton(CcAssignment.LOCK).bindIsPressed(this, this::handleLockButton);
      hwElements.getButton(CcAssignment.LOCK).bindLightHeld(this);
   }

   @Inject
   public void setPadLayer(PadLayer padLayer) {
      this.padLayer = padLayer;
      this.padLayer.registerSustainReleaseListener(() -> collectedHeldNotesToChord());
   }

   private void deserializeChordData(String data) {
      String[] chordValues = data.split(";");
      List<Chord> readChords = new ArrayList<>();
      for (String individualChord : chordValues) {
         String[] offsets = individualChord.split(",");
         List<Integer> chordOffsets = new ArrayList<>();
         for (String offset : offsets) {
            if (!offset.isEmpty() && offset.chars().allMatch(Character::isDigit)) {
               chordOffsets.add(Integer.parseInt(offset));
            }
         }
         if (!chordOffsets.isEmpty()) {
            readChords.add(new Chord(chordOffsets));
         }
      }
      MaschineMikroExtension.println(" Read chords %d", readChords.size());
      int limit = Math.min(chords.size(), readChords.size());
      for (int i = 0; i < limit; i++) {
         chords.set(i, readChords.get(i));
      }
   }

   private InternalHardwareLightState getPadLight(int index) {
      if (recordingModeActive && index == recordingIndex) {
         return playing[index] != null ? RgbColor.ORANGE : RgbColor.RED.brightness(ColorBrightness.DIMMED);
      }
      return playing[index] != null ? RgbColor.WHITE : RgbColor.GREEN.brightness(ColorBrightness.DIMMED);
   }

   private Set<Integer> noteSet = new HashSet<>();

   private void handleNotesIn(PlayingNote[] notes) {
      if (!recordingModeActive || isPadHeld()) {
         return;
      }
      if (notes.length == 0 && !padLayer.isSustainOn()) {
         collectedHeldNotesToChord();
      } else {
         for (int i = 0; i < notes.length; i++) {
            noteSet.add(notes[i].pitch() - baseNote);
         }
      }
   }

   private void collectedHeldNotesToChord() {
      if (noteSet.isEmpty()) {
         return;
      }
      List<Integer> noteList = noteSet.stream().sorted().toList();
      Chord chord = new Chord(noteList);
      chords.set(recordingIndex, chord);
      //String v = noteList.stream().sorted().map(Object::toString).collect(Collectors.joining(","));
      //MaschineMikroExtension.println("new Chord (%s), ", v);
      noteSet.clear();
      recordingIndex = recordingIndex + 1;
      if (recordingIndex > 11) {
         recordingModeActive = false;
         recordingIndex = 0;
      }
   }

   private boolean isPadHeld() {
      for (Chord play : playing) {
         if (play != null) {
            return true;
         }
      }
      return false;
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
      String dataString = chords.stream().map(Chord::serialized).collect(Collectors.joining(";"));
      chordData.set(dataString);
   }

   void handlePlayed(int index, double velocity) {
      if (modifierLayer.getSelectHeld().get()) {
         recordingIndex = index;
         if (!recordingModeActive) {
            recordingModeActive = true;
         }
      } else {
         Chord notesChord = toNotesChord(chords.get(index));
         double playVel = padLayer.isFixedActive() ? (padLayer.getFixedVelocity() / 127.0) : velocity;
         playChord(notesChord, playVel);
         playing[index] = notesChord;
      }
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
         int playedNote = filterByScale ? padLayer.matchScale(note + offset) : note + offset;
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

   private void handleLockButton(boolean pressed) {
      midiProcessor.sendRawCC(0x40, pressed ? 127 : 0);
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
