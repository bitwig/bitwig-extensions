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

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MidiProcessor {

   protected final MidiIn midiIn;
   protected final MidiOut midiOut;
   protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
   protected final ControllerHost host;
   private final NoteInput noteInput;
   private int blinkState = 0;


   public MidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
      this.host = host;
      this.midiIn = midiIn;
      this.midiOut = midiOut;
      noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????");
      setupNoteInput();
      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::handleSysEx);
   }

   private void setupNoteInput() {
      noteInput.setShouldConsumeEvents(false);
      Integer[] noAssignTable = new Integer[128];
      Arrays.fill(noAssignTable, Integer.valueOf(-1));
      noteInput.setKeyTranslationTable(noAssignTable);
   }

   private void onMidi0(final ShortMidiMessage msg) {
      final int sb = msg.getStatusByte();
      //DebugOutMk.println("MIDI %02X %02X %02x ", sb, msg.getData1(), msg.getData2());
   }

   protected void handleSysEx(final String sysExString) {
      if (sysExString.equals("f000210917004d5000014601f7")) {
         DebugOutMk.println(" HANDLE Return from Maschine");
         // TODO Refresh all => MK3
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

   public void sendMidi(final int status, final int val1, final int val2) {
      midiOut.sendMidi(status, val1, val2);
   }


   public void updateColorPad(int midiId, RgbColor rgbState) {
      midiOut.sendMidi(Midi.NOTE_ON, midiId, rgbState.getColorIndex());
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

   public RgbColor blinkMid(Colors color) {
      if (blinkState % 4 < 2) {
         return RgbColor.of(color, ColorBrightness.DIMMED);
      }
      return RgbColor.of(color, ColorBrightness.BRIGHT);
   }

   RgbColor blinkFast(int onColor, int offColor) {
      return RgbColor.of(blinkState % 8 == 0 ? onColor : offColor);
   }

   public NoteInput getNoteInput() {
      return noteInput;
   }
}
