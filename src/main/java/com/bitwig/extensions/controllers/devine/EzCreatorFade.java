package com.bitwig.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class EzCreatorFade extends ControllerExtension
{
   public EzCreatorFade(final EzCreatorFadeDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();
      mTransport.crossfade().setIndication(true);

      mTrackBank = host.createTrackBank(8, 0, 0);
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         track.volume().setIndication(true);
         track.pan().setIndication(true);
      }

      mMasterTrack = host.createMasterTrack(0);
      mMasterTrack.volume().setIndication(true);
      mMasterTrack.pan().setIndication(true);

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback(this::onMidi);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(EzCreatorCommon.INIT_SYSEX);
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      // getHost().println("msg: " + msg + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      if (status == 0xB0)
      {
         if (0x01 <= data1 && data1 <= 0x08)
            mTrackBank.getItemAt(data1 - 0x01).volume().set(data2, 128);
         else if (data1 == 0x09)
            mMasterTrack.volume().set(data2, 128);
         else if (0x0A <= data1 && data1 < 0x12)
            mTrackBank.getItemAt(data1 - 0x0A).pan().set(data2, 128);
         else if (64 <= data1 && data1 <= 71 && data2 == 127)
            mTrackBank.getItemAt(data1 - 64).mute().toggle();
         else if (data1 == 72 && data2 == 127)
            mMasterTrack.mute().toggle();
         else if (data1 == 0x12)
            mMasterTrack.pan().set(data2, 128);
         else if (data1 == 0x13)
            mTransport.crossfade().set(data2, 128);
         else if (data1 == 0x20 && data2 == 127)
            mTransport.isArrangerRecordEnabled().toggle();
         else if (data1 == 0x1C && data2 == 127)
            mTransport.togglePlay();
         else if (data1 == 0x1F && data2 == 127)
            mTransport.stop();
         else if (data1 == 0x1E && data2 == 127)
            mTransport.isArrangerLoopEnabled().toggle();
         else if (data1 == 0x1D && data2 == 127)
            mTransport.fastForward();
         else if (data1 == 0x1B && data2 == 127)
            mTransport.rewind();
         else if (data1 == 81 && data2 == 127)
            mTrackBank.scrollPageBackwards();
         else if (data1 == 82 && data2 == 127)
            mTrackBank.scrollPageForwards();
         else if (data1 == 0x30)
            mTransport.tempo().incRaw(data2 < 64 ? data2 : (data2 - 128));
      }
   }

   @Override
   public void exit()
   {
      mMidiOut.sendSysex(EzCreatorCommon.DEINIT_SYSEX);
   }

   @Override
   public void flush()
   {

   }

   private Transport mTransport;
   private MasterTrack mMasterTrack;
   private TrackBank mTrackBank;
   private MidiOut mMidiOut;
}
