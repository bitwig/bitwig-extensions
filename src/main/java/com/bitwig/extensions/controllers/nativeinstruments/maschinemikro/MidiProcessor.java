package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MidiProcessor {

   protected final MidiIn midiIn;
   protected final MidiOut midiOut;
   protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
   protected final ControllerHost host;
   private final NoteInput noteInput;
   private int blinkState = 0;
   private List<NoteListener> noteListeners = new ArrayList<>();

   public void sendRawNoteOn(int note, int velocity) {
      noteInput.sendRawMidiEvent(Midi.NOTE_ON, note, velocity);
   }

   public void sendNoteOff(int note) {
      noteInput.sendRawMidiEvent(Midi.NOTE_OFF, note, 0);
   }

   @FunctionalInterface
   public interface NoteListener {
      void handleNote(int noteNr, int vel);
   }

   private int[] noteStatus = new int[128];
   private int[] ccStatus = new int[128];

   public MidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
      this.host = host;
      this.midiIn = midiIn;
      this.midiOut = midiOut;
      noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A?????");
      setupNoteInput();
      Arrays.fill(noteStatus, -1);
      Arrays.fill(ccStatus, -1);
      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::handleSysEx);
   }

   public void addNoteListener(NoteListener noteListener) {
      this.noteListeners.add(noteListener);
   }

   private void setupNoteInput() {
      noteInput.setShouldConsumeEvents(true);
      Integer[] noAssignTable = new Integer[128];
      Arrays.fill(noAssignTable, Integer.valueOf(-1));
      noteInput.setKeyTranslationTable(noAssignTable);
   }

   private void onMidi0(final ShortMidiMessage msg) {
      final int sb = msg.getStatusByte();
      if (msg.getStatusByte() == 0x90) {
         noteListeners.forEach(listener -> listener.handleNote(msg.getData1(), msg.getData2()));
      }
   }

   protected void handleSysEx(final String sysExString) {
      if (sysExString.equals("f000210917004d5000014601f7")) {
         DebugOutMk.println(" HANDLE Return from Maschine");
         for (int i = 0; i < noteStatus.length; i++) {
            if (noteStatus[i] != -1) {
               midiOut.sendMidi(Midi.NOTE_ON, i, noteStatus[i]);
            }
         }
         for (int i = 0; i < ccStatus.length; i++) {
            if (ccStatus[i] != -1) {
               midiOut.sendMidi(Midi.CC, i, ccStatus[i]);
            }
         }
      } else {
         DebugOutMk.println("SYSEX = %s", sysExString);
      }
   }

   public void start() {
      host.scheduleTask(this::handlePing, 100);
   }

   public void queueEvent(final TimedEvent event) {
      timedEvents.add(event);
   }

   public MidiIn getMidiIn() {
      return midiIn;
   }

   private void handlePing() {
      blinkState = (blinkState + 1) % 8;
      if (!timedEvents.isEmpty()) {
         for (final TimedEvent event : timedEvents) {
            event.process();
            if (event.isCompleted()) {
               timedEvents.remove(event);
            }
         }
      }
      host.scheduleTask(this::handlePing, 100);
   }

   public void sendMidiCC(final int val1, final int val2) {
      midiOut.sendMidi(Midi.CC, val1, val2);
      ccStatus[val1] = val2;
   }

   public void updateColorPad(int midiId, RgbColor rgbState) {
      midiOut.sendMidi(Midi.NOTE_ON, midiId, rgbState.getColorIndex());
      noteStatus[midiId] = rgbState.getColorIndex();
   }

   public RgbColor blinkSlow(int onColor, int offColor) {
      return RgbColor.of(blinkState / 4 == 0 ? onColor : offColor);
   }

   public RgbColor blinkSlow(Colors color) {
      if (blinkState / 4 == 0) {
         return RgbColor.of(color, ColorBrightness.DIMMED);
      }
      return RgbColor.of(color, ColorBrightness.BRIGHT);
   }


   public RgbColor blinkMid(int onColor, int offColor) {
      return RgbColor.of(blinkState % 4 < 2 ? onColor : offColor);
   }

   public RgbColor blinkMid(RgbColor color) {
      if (blinkState % 4 < 2) {
         return color.brightness(ColorBrightness.BRIGHT);
      }
      return color;
   }

   public boolean blinkMid() {
      return blinkState % 4 < 2;
   }

   public RgbColor blinkMid(Colors color) {
      if (blinkState % 4 < 2) {
         return RgbColor.of(color, ColorBrightness.DIMMED);
      }
      return RgbColor.of(color, ColorBrightness.BRIGHT);
   }

   public RgbColor blinkFast(int onColor, int offColor) {
      return RgbColor.of(blinkState % 2 == 0 ? onColor : offColor);
   }

   public NoteInput getNoteInput() {
      return noteInput;
   }

   public void sendCcToNoteInput(int ccNr, int value) {
      noteInput.sendRawMidiEvent(Midi.CC, ccNr, value);
   }

   public void sendPitchBendToNoteInput(int channel, double value) {
      int msb = (int) Math.min(127, Math.round((value + 1.0) * 64));
      noteInput.sendRawMidiEvent(Midi.PITCH_BEND | channel, 0, msb);
   }

   public void playNote(int channel, int noteNr, int velocity) {
      noteInput.sendRawMidiEvent(Midi.NOTE_ON | channel, noteNr, velocity);
   }

   public void releaseNote(int channel, int noteNr) {
      noteInput.sendRawMidiEvent(Midi.NOTE_OFF | channel, noteNr, 0);
   }
}
