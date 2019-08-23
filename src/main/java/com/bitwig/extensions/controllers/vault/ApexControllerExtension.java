package com.bitwig.extensions.controllers.vault;

import com.bitwig.extensions.controllers.devine.EzCreatorCommon;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class ApexControllerExtension extends ControllerExtension
{
   public ApexControllerExtension(
      final ControllerExtensionDefinition definition,
      final ControllerHost host,
      final boolean hasFaders)
   {
      super(definition, host);

      mHasFaders = hasFaders;
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback(this::onMidi);
      final NoteInput keysInput = midiIn.createNoteInput("Apex Keys", "80????", "90????");
      final NoteInput padsInput = midiIn.createNoteInput("Apex Pads", "89????", "99????");

      keysInput.setShouldConsumeEvents(true);
      padsInput.setShouldConsumeEvents(true);

      final Integer[] keyTranslationTable = new Integer[128];
      for (int i = 0; i < keyTranslationTable.length; ++i)
         keyTranslationTable[i] = 0;

      final int keyC1 = 12 * 3;
      keyTranslationTable[0x30] = keyC1 + 4;
      keyTranslationTable[0x2D] = keyC1 + 5;
      keyTranslationTable[0x31] = keyC1 + 6;
      keyTranslationTable[0x33] = keyC1 + 7;
      keyTranslationTable[0x2D] = keyC1 + 0;
      keyTranslationTable[0x24] = keyC1 + 1;
      keyTranslationTable[0x26] = keyC1 + 2;
      keyTranslationTable[0x2E] = keyC1 + 3;

      padsInput.setKeyTranslationTable(keyTranslationTable);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(EzCreatorCommon.INIT_SYSEX);

      mTrackCursor = host.createCursorTrack("panda-cursor", "ApexControllerExtension", 0, 0, true);
      mCursorDevice = mTrackCursor.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage("ApexControllerExtension", 8, "");

      if (mHasFaders)
      {
         mTrackBank = host.createTrackBank(8, 0, 0);
         for (int i = 0; i < 8; ++i)
            mTrackBank.getItemAt(i).volume().setIndication(true);

         mMasterTrack = host.createMasterTrack(0);
         mMasterTrack.volume().setIndication(true);
      }

      mTransport = host.createTransport();
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      getHost().println("msg: " + msg + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      switch (status)
      {
         case 0xB0:
            if (0x0A <= data1 && data1 <= 0x11)
               mRemoteControls.getParameter(data1 - 0x0A).set(data2, 128);
            else if (mHasFaders && 0x01 <= data1 && data1 <= 0x08)
               mTrackBank.getItemAt(data1 - 0x01).volume().set(data2, 128);
            else if (mHasFaders && data1 == 0x09)
               mMasterTrack.volume().set(data2, 128);
            else if (data1 == 0x72 && data2 == 0x7F)
               mTransport.isArrangerLoopEnabled().toggle();
            else if (data1 == 0x73 && data2 == 0x7F)
               mTransport.rewind();
            else if (data1 == 0x74 && data2 == 0x7F)
               mTransport.fastForward();
            else if (data1 == 0x75 && data2 == 0x7F)
               mTransport.record();
            else if (data1 == 0x76 && data2 == 0x7F)
               mTransport.stop();
            else if (data1 == 0x77 && data2 == 0x7F)
               mTransport.togglePlay();
            break;
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

   private MidiOut mMidiOut;
   private CursorTrack mTrackCursor;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private TrackBank mTrackBank;
   private Transport mTransport;
   private final boolean mHasFaders;
   private MasterTrack mMasterTrack;
}
