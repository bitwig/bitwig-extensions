package com.bitwig.extensions.controllers.generic;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Transport;

public class MIDIKeyboardExtension extends ControllerExtension
{
   protected MIDIKeyboardExtension(
      final MIDIKeyboardExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);
      mMidiIn.setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi(msg));
      mMidiIn.setSysexCallback((final String data) -> onSysex(data));

      mMidiIn.createNoteInput("All Channels", "??????");

      for (int i = 0; i < 16; i++)
      {
         mMidiIn.createNoteInput("Channel " + (i + 1), "?" + Integer.toHexString(i) + "????");
      }

      mTransport = host.createTransport();
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi(final ShortMidiMessage msg)
   {
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex(final String data)
   {
      // MMC Transport Controls:
      if (data.equals("f07f7f0605f7"))
         mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
         mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
         mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
         mTransport.play();
      else if (data.equals("f07f7f0606f7"))
         mTransport.record();
   }

   private Transport mTransport;

   private MidiIn mMidiIn;
}
