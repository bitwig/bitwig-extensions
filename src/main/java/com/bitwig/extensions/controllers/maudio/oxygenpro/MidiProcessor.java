package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MidiProcessor {
   private static final String OXYGEN_SYSEX = "F0 00 01 05 7F 00 00 %02X 00 01 %02X F7";

   protected final MidiIn midiIn;
   protected final MidiOut midiOut;
   protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
   protected final ControllerHost host;

   public MidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
      this.host = host;
      this.midiIn = midiIn;
      this.midiOut = midiOut;
      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::handleSysEx);
   }

   private void onMidi0(final ShortMidiMessage msg) {
      final int sb = msg.getStatusByte();
      DebugOutOxy.println("MIDI %02X %02X %02x ", sb, msg.getData1(), msg.getData2());
   }

   public void start() {
      host.scheduleTask(this::handlePing, 50);
   }

   public void queueEvent(final TimedEvent event) {
      timedEvents.add(event);
   }

   public MidiIn getMidiIn() {
      return midiIn;
   }

   private void handlePing() {
      if (!timedEvents.isEmpty()) {
         for (final TimedEvent event : timedEvents) {
            event.process();
            if (event.isCompleted()) {
               timedEvents.remove(event);
            }
         }
      }
      host.scheduleTask(this::handlePing, 50);
   }

   protected void handleSysEx(final String sysExString) {
      DebugOutOxy.println("SYSEX = %s", sysExString);
   }

   public void sendMidi(final int status, final int val1, final int val2) {
      midiOut.sendMidi(status, val1, val2);
   }


   public void initSysexMessages() {
      midiOut.sendSysex("F0 7E 7F 06 01 F7");
      sendOxyCommand(0x6D, 2); // Bitwig
      sendOxyCommand(0x6E, 2);
      sendOxyCommand(0x6E, 7);
      sendOxyCommand(0x6B, 1);
      sendOxyCommand(0x6C, 3);
   }

   private void sendOxyCommand(int commandId, int arg) {
      String message = String.format(OXYGEN_SYSEX, commandId, arg);
      midiOut.sendSysex(message);
   }


}
