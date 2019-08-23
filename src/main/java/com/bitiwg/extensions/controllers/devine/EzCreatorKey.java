package com.bitiwg.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;

public class EzCreatorKey extends ControllerExtension
{
   public EzCreatorKey(final EzCreatorKeyDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.createNoteInput("EZ-Creator Key", "80????", "90????").setShouldConsumeEvents(true);
      midiIn.setMidiCallback(this::onMidiIn);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(EzCreatorCommon.INIT_SYSEX);

      CursorTrack cursorTrack = host.createCursorTrack(0, 0);
      PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      CursorRemoteControlsPage remoteControls = cursorDevice.createCursorRemoteControlsPage(1);
      remoteControls.setHardwareLayout(HardwareControlType.KNOB, 1);
      mParameter = remoteControls.getParameter(0);
      mParameter.markInterested();
      mParameter.setIndication(true);
   }

   private void onMidiIn(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      // getHost().println("msg: " + msg + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      if (status == 0xB0 && data1 == 0x03)
         mParameter.set(data2, 128);
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
   private RemoteControl mParameter;
}
