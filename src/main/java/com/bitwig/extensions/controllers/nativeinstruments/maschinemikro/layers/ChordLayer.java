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
import com.bitwig.extensions.framework.values.PadScaleHandler;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChordLayer extends Layer {

   private final MidiProcessor midiProcessor;
   private final SettableStringValue chordData;
   private boolean filterByScale = false;
   private FilterMethod filterMethod = FilterMethod.UP;
   private boolean recordingModeActive = false;
   private int recordingIndex = 0;
   private PadScaleHandler scaleHandler;

   private enum FilterMethod {
      UP,
      DOWN,
      ELIMINATE;

      FilterMethod next() {
         return switch (this) {
            case UP -> DOWN;
            case DOWN -> ELIMINATE;
            case ELIMINATE -> UP;
         };
      }
   }

   static class Chord {
      private final List<Integer> notes;
      private int basicOffset;
      private boolean allInScale = false;

      public Chord(int... notes) {
         this.notes = Arrays.stream(notes).mapToObj(Integer::valueOf).toList();
      }

      public Chord(Collection<Integer> notes) {
         this.notes = notes.stream().toList();
      }

      public void setBasicOffset(int basicOffset) {
         this.basicOffset = basicOffset;
      }

      public int getBasicOffset() {
         return basicOffset;
      }

      public Set<Integer> getNotes(boolean filterByScale, FilterMethod method, int baseNote,
                                   PadScaleHandler scaleHandler) {
         int offset = baseNote + basicOffset;
         Set<Integer> result = new HashSet<>();
         for (Integer note : notes) {
            int playedNote = note + offset;
            if (filterByScale) {
               switch (method) {
                  case UP:
                     playedNote = scaleHandler.matchScale(playedNote, 1);
                  case DOWN:
                     playedNote = scaleHandler.matchScale(playedNote, -1);
                  case ELIMINATE:
                     playedNote = scaleHandler.inScale(playedNote) ? playedNote : -1;
               }
            }
            if (playedNote >= 0 && playedNote < 128) {
               result.add(playedNote);
            }
         }
         return result;
      }

      public boolean isAllInScale() {
         return allInScale;
      }

      public void calcAllInScale(int baseNote, PadScaleHandler scaleHandler) {
         if (scaleHandler.getCurrentScale().getIntervals().length == 12) {
            allInScale = true;
         } else {
            allInScale = notes.stream().allMatch(note -> scaleHandler.inScale(note + baseNote + basicOffset));
         }
      }

      public Chord getPlayingNotes(boolean filterByScale, FilterMethod method, int baseNote,
                                   PadScaleHandler scaleHandler) {
         return new Chord(getNotes(filterByScale, method, baseNote, scaleHandler));
      }

      public String serialized() {
         return notes.stream().map(v -> v.toString()).collect(Collectors.joining(","));
      }
   }

   private final static String INIT_CHORD_DATA = "0,12,16,19;7,19,23,26;5,17,21,24;9,21,24,28;4,16,19,23;0,12,14,19;" + "0,12,16,19,23;0,7,14,19,26;5,17,19,24;7,19,24,26;0,12,19,24,26;-7,5,17,24,31";

   private List<Chord> chords = List.of(new Chord(0, 12, 16, 19), new Chord(7, 19, 23, 26), new Chord(5, 17, 21, 24),
      new Chord(4, 16, 19, 23), new Chord(2, 14, 17, 21), new Chord(-3, 4, 9, 14, 17, 23), new Chord(9, 14, 17, 23),
      new Chord(-3, 9, 21, 24, 28), new Chord(9, 21, 24, 28), new Chord(0, 12, 14, 19), new Chord(0, 16, 19, 23),
      new Chord(0, 12, 16, 19, 23)).stream().collect(Collectors.toList());


   @Inject
   private ModifierLayer modifierLayer;
   private PadLayer padLayer;

   private Chord[] playing = new Chord[12];

   private int baseNote = 48;

   public ChordLayer(ControllerHost host, Layers layers, HwElements hwElements, MidiProcessor midiProcessor,
                     ViewControl viewControl) {
      super(layers, "CHORD_LAYER");

      this.midiProcessor = midiProcessor;
      DocumentState documentState = host.getDocumentState();
      chordData = documentState.getStringSetting("Chord Data", "Chords", 500, INIT_CHORD_DATA);
      chordData.addValueObserver(this::deserializeChordData);

      CursorTrack cursorTrack = viewControl.getCursorTrack();

      cursorTrack.playingNotes().addValueObserver(this::handleNotesIn);

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      RgbButton scaleTransposeMethod = gridButtons.get(2);
      scaleTransposeMethod.bindLight(this, this::getFilterStateButtonColor);
      scaleTransposeMethod.bindPressed(this, () -> this.filterMethod = this.filterMethod.next());

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
      gridButtons.get(1).bindDisabled(this);

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
      this.scaleHandler = this.padLayer.getScaleHandler();
      scaleHandler.addStateChangedListener(() -> {
         for (Chord chord : chords) {
            chord.calcAllInScale(baseNote, scaleHandler);
         }
//         MaschineMikroExtension.println(" SCALE %s %d", scaleHandler.getCurrentScale().getName(),
//            scaleHandler.getBaseNote());
      });
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
      if (playing[index] != null) {
         return chords.get(index).isAllInScale() ? RgbColor.WHITE.brightness(
            ColorBrightness.BRIGHT) : RgbColor.PINK.brightness(ColorBrightness.SUPERBRIGHT);
      } else {
         return chords.get(index).isAllInScale() ? RgbColor.GREEN.brightness(ColorBrightness.DIMMED) : RgbColor.of(
            Colors.MINT).brightness(ColorBrightness.DIMMED);
      }
   }

   private Set<Integer> noteSet = new HashSet<>();

   private RgbColor getFilterStateButtonColor() {
      if (filterByScale) {
         return switch (filterMethod) {
            case UP -> RgbColor.BLUE;
            case DOWN -> RgbColor.PURPLE;
            case ELIMINATE -> RgbColor.GRAY;
         };
      }
      return RgbColor.OFF;
   }

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
      chord.calcAllInScale(baseNote, scaleHandler);
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
      for (Chord chord : chords) {
         chord.calcAllInScale(baseNote, scaleHandler);
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
            chord.calcAllInScale(baseNote, scaleHandler);
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
      if (modifierLayer.getShiftHeld().get()) {
         deserializeChordData(chordData.get());
      } else {
         String dataString = chords.stream().map(Chord::serialized).collect(Collectors.joining(";"));
         chordData.set(dataString);
      }
   }

   void handlePlayed(int index, double velocity) {
      if (modifierLayer.getSelectHeld().get()) {
         recordingIndex = index;
         if (!recordingModeActive) {
            recordingModeActive = true;
         }
      } else {
         Chord notesChord = chords.get(index).getPlayingNotes(filterByScale, filterMethod, baseNote, scaleHandler);
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
         int playedNote = filterByScale ? scaleHandler.matchScale(note + offset, -1) : note + offset;
         if (playedNote >= 0 && playedNote < 128) {
            notes.add(playedNote);
         }
      }
      return new Chord(notes);
   }

   private void playChord(Chord chord, double velocity) {
      int intVelocity = (int) (velocity * 127);
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
