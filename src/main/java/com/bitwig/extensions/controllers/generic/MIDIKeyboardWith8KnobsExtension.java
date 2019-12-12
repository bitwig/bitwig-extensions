package com.bitwig.extensions.controllers.generic;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.UserControlBank;

public class MIDIKeyboardWith8KnobsExtension extends ControllerExtension
{
   private static final int LOWEST_CC = 1;

   private static final int HIGHEST_CC = 119;

   private static final int DEVICE_START_CC = 20;

   private static final int DEVICE_END_CC = 27;

   protected MIDIKeyboardWith8KnobsExtension(
      final MIDIKeyboardWith8KnobsExtensionDefinition definition,
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

      // Map CC 20 - 27 to device parameters

      mCursorTrack = host.createCursorTrack(3, 0);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      for (int i = 0; i < 8; i++)
      {
         final Parameter p = mRemoteControls.getParameter(i);
         p.setIndication(true);
         p.setLabel("P" + (i + 1));
      }

      // Make the rest freely mappable
      mUserControls = host.createUserControls(HIGHEST_CC - LOWEST_CC + 1 - 8);

      for (int i = LOWEST_CC; i < HIGHEST_CC; i++)
      {
         if (!isInDeviceParametersRange(i))
         {
            final int index = userIndexFromCC(i);
            mUserControls.getControl(index).setLabel("CC" + i);
         }
      }
   }

   private static boolean isInDeviceParametersRange(final int cc)
   {
      return cc >= DEVICE_START_CC && cc <= DEVICE_END_CC;
   }

   private static int userIndexFromCC(final int cc)
   {
      if (cc > DEVICE_END_CC)
      {
         return cc - LOWEST_CC - 8;
      }

      return cc - LOWEST_CC;
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

         if (isInDeviceParametersRange(data1))
         {
            final int index = data1 - DEVICE_START_CC;
            mRemoteControls.getParameter(index).value().set(msg.getData2(), 128);
         }
         else if (data1 >= LOWEST_CC && data1 <= HIGHEST_CC)
         {
            final int index = userIndexFromCC(data1);
            mUserControls.getControl(index).value().set(msg.getData2(), 128);
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

   private CursorTrack mCursorTrack;

   private PinnableCursorDevice mCursorDevice;

   private CursorRemoteControlsPage mRemoteControls;

   private MidiIn mMidiIn;

   private UserControlBank mUserControls;
}
