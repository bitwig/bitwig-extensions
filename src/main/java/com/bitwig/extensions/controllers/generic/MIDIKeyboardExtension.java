package com.bitwig.extensions.controllers.generic;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.UserControlBank;

public class MIDIKeyboardExtension extends ControllerExtension
{
   private static final int LOWEST_CC = 1;

   private static final int HIGHEST_CC = 119;

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

      final NoteInput allChannels = mMidiIn.createNoteInput("All Channels", "??????");
      allChannels.setShouldConsumeEvents(false);

      for (int i = 0; i < 16; i++)
      {
         mMidiIn.createNoteInput("Channel " + (i + 1), "?" + Integer.toHexString(i) + "????");
      }

      mTransport = host.createTransport();

      // Make CCs 1-119 freely mappable
      mUserControls = host.createUserControls(HIGHEST_CC - LOWEST_CC + 1);

      for (int i = LOWEST_CC; i <= HIGHEST_CC; i++)
      {
         mUserControls.getControl(i - LOWEST_CC).setLabel("CC" + i);
      }
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
      if (msg.isControlChange())
      {
         final int data1 = msg.getData1();
         final int data2 = msg.getData2();

         if (data1 >= LOWEST_CC && data1 <= HIGHEST_CC)
         {
            final int index = data1 - LOWEST_CC;
            mUserControls.getControl(index).set(data2, 128);
         }
      }
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

   private UserControlBank mUserControls;
}
