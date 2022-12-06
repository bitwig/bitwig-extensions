package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DrumPadLayer extends Layer {
   private static final String[] noteValues = {"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "};

   private final RgbLightState[] colorSlots = new RgbLightState[8];
   //protected final Integer[] velTable = new Integer[128];
   private final Integer[] noteTable = new Integer[128];
   private final boolean[] isPlaying = new boolean[128];
   private final Set<Integer> padNotes = new HashSet<>();
   private final NoteInput noteInput;
   private final DrumPadBank padBank;
   private final MiniLab3Extension driver;
   private RgbLightState trackColor = RgbLightState.OFF;
   private boolean focusOnDrums = false;
   private int keysNoteOffset = 60;
   private int padsNoteOffset = 32;
   private final int[] hangingNotes = new int[8];

   public DrumPadLayer(final MiniLab3Extension driver) {
      super(driver.getLayers(), "DRUM_PAD");
      this.driver = driver;
      noteInput = driver.getMidiIn().createNoteInput("MIDI", "89????", "99????", "A9????");

      noteInput.setShouldConsumeEvents(false);
      final PinnableCursorDevice primaryDevice = driver.getPrimaryDevice();
      primaryDevice.hasDrumPads().addValueObserver(this::handleHasDrumsChanged);
      padBank = primaryDevice.createDrumPadBank(8);
      padBank.scrollPosition().addValueObserver(index -> {
         padsNoteOffset = index;
         if (isActive()) {
            driver.getOled()
               .sendTextInfo(DisplayMode.SCENE, "Pad Notes",
                  String.format("%s - %s", toNote(padsNoteOffset), toNote(padsNoteOffset + 7)), true);
            applyNotes(padsNoteOffset);
         }
      });
      padBank.scrollPosition().set(30);
      padBank.setIndication(true);
      primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
         if (isActive()) {
            applyNotes(hasDrumPads ? padsNoteOffset : keysNoteOffset);
         }
      });

      padsNoteOffset = padBank.scrollPosition().get();
      Arrays.fill(noteTable, -1);
      Arrays.fill(colorSlots, RgbLightState.BLUE);
      Arrays.fill(hangingNotes, -1);

      final CursorTrack cursorTrack = driver.getCursorTrack();

      final RgbButton[] buttons = driver.getPadBankBButtons();
      for (int i = 0; i < buttons.length; i++) {
         final int index = i;
         padNotes.add(buttons[i].getNoteValue());
         final DrumPad pad = padBank.getItemAt(i);
         pad.exists().markInterested();

         final RgbButton button = buttons[i];
         pad.color().addValueObserver((r, g, b) -> colorSlots[index] = RgbLightState.getColor(r, g, b));
         button.bindPressed(this, down -> handleSelected(index, down), () -> getLightState(index, pad));
      }

      final RgbBankLightState.Handler bankLightHandler = new RgbBankLightState.Handler(PadBank.BANK_B, buttons);
      final MultiStateHardwareLight bankLight = driver.getSurface().createMultiStateHardwareLight("PAD_LAUNCH_LIGHTS");
      bankLight.state().onUpdateHardware(driver::updateBankState);
      bindLightState(bankLightHandler::getBankLightState, bankLight);

      cursorTrack.playingNotes().addValueObserver(this::handleNotes);
      cursorTrack.color().addValueObserver((r, g, b) -> trackColor = RgbLightState.getColor(r, g, b));
   }

   public void notifyNote(final int sb, final int noteValue) {
      if (!isActive() || !padNotes.contains(noteValue) || (sb != Midi.NOTE_ON && sb != Midi.NOTE_OFF)) {
         return;
      }
      final int index = noteValue - 44;
      final int notePlayed = focusOnDrums ? padsNoteOffset + index : keysNoteOffset + index;
      if (sb == Midi.NOTE_ON) {
         hangingNotes[index] = notePlayed;
      } else {
         if (hangingNotes[index] != -1 && hangingNotes[index] != notePlayed) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF | 9, hangingNotes[index], 0);
         }
         hangingNotes[index] = -1;
      }
   }

   private void handleHasDrumsChanged(final boolean hasDrums) {
      focusOnDrums = hasDrums;
   }

   private void handleNotes(final PlayingNote[] playingNotes) {
      if (!isActive()) {
         return;
      }
      Arrays.fill(isPlaying, false);
      for (final PlayingNote playingNote : playingNotes) {
         isPlaying[playingNote.pitch()] = true;
      }
   }

   public void navigate(final int dir) {
      if (focusOnDrums) {
         driver.getOled().enableValues(DisplayMode.SCENE);
         padBank.scrollBy(dir * 4);
      } else {
         final int newOffset = keysNoteOffset + dir * 4;
         if (newOffset >= 0 && newOffset <= 116) {
            //muteNoteFromOffset(keysNoteOffset);
            keysNoteOffset = newOffset;
            driver.getOled().enableValues(DisplayMode.SCENE);
            driver.getOled()
               .sendTextInfo(DisplayMode.SCENE, "Pad Notes",
                  String.format("%s - %s", toNote(newOffset), toNote(newOffset + 7)), true);
            applyNotes(keysNoteOffset);
         }
      }
   }

   private String toNote(final int midiValue) {
      final int noteValue = midiValue % 12;
      final int octave = (midiValue / 12) - 2;
      return noteValues[noteValue] + octave;
   }

   private void handleSelected(final int index, final boolean pressed) {
      driver.getHost().println(String.format("%d %s", index, pressed));
   }

   RgbLightState getLightState(final int index, final DrumPad pad) {
      final int noteValue = (focusOnDrums ? padsNoteOffset : keysNoteOffset) + index;
      if (noteValue < 128) {
         final boolean notePlaying = isPlaying[noteValue];
         if (focusOnDrums) {
            if (pad.exists().get()) {
               if (colorSlots[index] != null) {
                  return notePlaying ? colorSlots[index].getBrighter() : colorSlots[index].getDarker();
               }
            } else {
               return RgbLightState.OFF;
            }
            return notePlaying ? trackColor.getBrighter() : trackColor.getDarker();
         }
         return notePlaying ? trackColor.getBrighter() : trackColor.getDarker();
      } else {
         return RgbLightState.OFF;
      }
   }

   public void applyNotes(final int noteOffset) {
      Arrays.fill(noteTable, -1);
      for (int note = 0; note < 8; note++) {
         final int value = noteOffset + note;
         noteTable[0x2c + note] = value < 128 ? value : -1;
      }
      noteInput.setKeyTranslationTable(noteTable);
   }

   private void resetNotes() {
      Arrays.fill(noteTable, -1);
      noteInput.setKeyTranslationTable(noteTable);
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      applyNotes(focusOnDrums ? padsNoteOffset : keysNoteOffset);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      resetNotes();
   }

}
