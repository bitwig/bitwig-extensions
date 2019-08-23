package com.bitiwg.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Transport;

public class EzCreatorPad extends ControllerExtension
{
   public EzCreatorPad(final EzCreatorPadDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback(this::onMidi);
      final NoteInput noteInput = midiIn.createNoteInput("EZ-Creator Pad", "89????", "99????");
      noteInput.setShouldConsumeEvents(true);

      final Integer[] keyTranslationTable = new Integer[128];
      for (int i = 0; i < keyTranslationTable.length; ++i)
      {
         if (0x30 <= i && i <= 0x3B)
            keyTranslationTable[i] = i - 12;
         else
            keyTranslationTable[i] = 0;
      }

      noteInput.setKeyTranslationTable(keyTranslationTable);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(EzCreatorCommon.INIT_SYSEX);

      mCursorTrack = host.createCursorTrack(0, 0);
      final PinnableCursorDevice cursorDevice = mCursorTrack.createCursorDevice();
      final CursorRemoteControlsPage remoteControls = cursorDevice.createCursorRemoteControlsPage(1);
      remoteControls.setHardwareLayout(HardwareControlType.SLIDER, 1);
      mParameter = remoteControls.getParameter(0);
      mParameter.markInterested();
      mParameter.setIndication(true);
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      // getHost().println("msg: " + msg + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      if (status == 0xB0)
      {
         if (data1 == 0x20 && data2 == 127)
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
         else if (data1 == 0x02)
            mParameter.set(data2, 128);
         else if (data1 == 64)
            mCursorTrack.selectPrevious();
         else if (data1 == 65)
            mCursorTrack.selectNext();
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
   private MidiOut mMidiOut;
   private RemoteControl mParameter;
   private CursorTrack mCursorTrack;
}
