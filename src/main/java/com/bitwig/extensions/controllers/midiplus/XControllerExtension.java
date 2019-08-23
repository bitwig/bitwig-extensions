package com.bitwig.extensions.controllers.midiplus;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class XControllerExtension extends ControllerExtension
{
   public XControllerExtension(
      final ControllerExtensionDefinition definition,
      final ControllerHost host,
      final int numPads,
      final int numKnobs,
      final byte[] initSysex,
      final byte[] deinitSysex)
   {
      super(definition, host);

      mKeyboardInputName = definition.getHardwareModel() + (numPads > 0 ? " Keys" : "");
      mPadsInputName = definition.getHardwareModel() + " Pads";
      mNumPads = numPads;
      mNumKnobs = numKnobs;
      mInitSysex = initSysex;
      mDeinitSysex = deinitSysex;
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback(this::onMidi);
      midiIn.setSysexCallback(this::onSysex);
      midiIn.createNoteInput(mKeyboardInputName, "80????", "90????", "b001??", "e0????", "b040??").setShouldConsumeEvents(true);

      if (mNumPads > 0)
         midiIn.createNoteInput(mPadsInputName, "89????", "99????").setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(mInitSysex);

      mCursorTrack = host.createCursorTrack("X2mini-track-cursor", "X2mini", 0, 0, true);

      if (mNumKnobs == 9)
         mCursorTrack.volume().setIndication(true);

      mCursorDevice =
         mCursorTrack.createCursorDevice("X2mini-device-cursor", "X2mini", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      final int numRemoteControls = Math.min(mNumKnobs, 8);
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(numRemoteControls);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, numRemoteControls);
      for (int i = 0; i < numRemoteControls; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();

      mTrackBank = host.createTrackBank(8, 0, 0, true);
   }

   private void onSysex(final String sysex)
   {
      getHost().println("received sysex: " + sysex);
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;
      getHost().println("msg: " + msg + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      if (status == 0xB0)
      {
         if (0x10 <= data1 && data1 <= 0x17)
            mRemoteControls.getParameter(data1 - 0x10).value().set(data2, 128);
         else if (data1 == 0x07)
            mCursorTrack.volume().set(data2, 128);
         if (0x18 <= data1 && data1 <= 0x1F)
            mCursorTrack.selectChannel(mTrackBank.getItemAt(data1 - 0x18));
      }
      else if (status == 0xA0)
      {
         switch (data1)
         {
            case 0x5A:
               if (data2 == 127)
                  mTransport.rewind();
               break;

            case 0x5B:
               if (data2 == 127)
                  mTransport.fastForward();
               break;

            case 0x5C:
               if (data2 == 127)
                  mTransport.stop();
               break;

            case 0x5D:
               if (data2 == 127)
                  mTransport.play();
               break;

            case 0x5E:
               if (data2 == 127)
                  mTransport.isArrangerLoopEnabled().toggle();
               break;

            case 0x5F:
               if (data2 == 127)
                  mTransport.record();
               break;
         }
      }
   }

   @Override
   public void exit()
   {
      // Restore the controller in the factory setting
      mMidiOut.sendSysex(mDeinitSysex);
   }

   @Override
   public void flush()
   {

   }

   /* Configuration */
   private final String mKeyboardInputName;
   private final String mPadsInputName;
   private final int mNumPads;
   private final int mNumKnobs;
   private final byte[] mInitSysex;
   private final byte[] mDeinitSysex;

   /* API Objects */
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private Transport mTransport;
   private MidiOut mMidiOut;
   private TrackBank mTrackBank;
}
