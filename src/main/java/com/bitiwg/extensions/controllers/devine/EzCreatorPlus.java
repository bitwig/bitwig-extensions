package com.bitiwg.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;

public class EzCreatorPlus extends ControllerExtension
{
   public EzCreatorPlus(final EzCreatorPlusDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback(this::onMidi);
      final NoteInput keysInput = midiIn.createNoteInput("EZ-Creator Plus Keys", "80????", "90????");
      final NoteInput padsInput = midiIn.createNoteInput("EZ-Creator Plus Pads", "89????", "99????");

      keysInput.setShouldConsumeEvents(true);
      padsInput.setShouldConsumeEvents(true);

      final Integer[] keyTranslationTable = new Integer[128];
      for (int i = 0; i < keyTranslationTable.length; ++i)
         keyTranslationTable[i] = 0;

      final int keyC1 = 12 * 3;
      keyTranslationTable[36] = keyC1 + 0;
      keyTranslationTable[38] = keyC1 + 1;
      keyTranslationTable[42] = keyC1 + 2;
      keyTranslationTable[46] = keyC1 + 3;
      keyTranslationTable[48] = keyC1 + 4;
      keyTranslationTable[45] = keyC1 + 5;
      keyTranslationTable[49] = keyC1 + 6;
      keyTranslationTable[51] = keyC1 + 7;

      padsInput.setKeyTranslationTable(keyTranslationTable);

      host.getMidiOutPort(0).sendSysex(EzCreatorCommon.INIT_SYSEX);

      mTrackCursor = host.createCursorTrack("ez-creator-plus-cursor", "EZ-Creator Plus", 0, 0, true);
      mCursorDevice = mTrackCursor.createCursorDevice();
      mKnobs = mCursorDevice.createCursorRemoteControlsPage("EZ-Creator Plus", 8, "");

      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl parameter = mKnobs.getParameter(i);
         parameter.setIndication(true);
      }
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      getHost().println("msg: " + msg + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      switch (status)
      {
         case 0xB0:
            if (0x0E <= data1 && data1 <= 0x11)
               mKnobs.getParameter(data1 - 0x0E).set(data2, 128);
            else if (0x03 <= data1 && data1 <= 0x06)
               mKnobs.getParameter(data1 - 0x03 + 4).set(data2, 128);
            break;
      }
   }

   @Override
   public void exit()
   {
      getHost().getMidiOutPort(0).sendSysex(EzCreatorCommon.DEINIT_SYSEX);
   }

   @Override
   public void flush()
   {

   }

   private CursorTrack mTrackCursor;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mKnobs;
}
