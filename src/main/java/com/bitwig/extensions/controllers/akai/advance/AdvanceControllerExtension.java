package com.bitwig.extensions.controllers.akai.advance;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;

public class AdvanceControllerExtension extends ControllerExtension
{
   public AdvanceControllerExtension(
      final AdvanceControllerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);
      mMidiIn.setMidiCallback(this::onMiniIn);
      mMidiIn.setSysexCallback(this::onSysexIn);

      mKeyboardInput = mMidiIn.createNoteInput("Keyboard", "80????", "90????", "B001??", "B00B??", "B040??", "D0????", "E0????");
      mKeyboardInput.setShouldConsumeEvents(true);

      mPadInput = mMidiIn.createNoteInput("Pads", "89????", "99????", "B901??", "B90B??", "B940??", "D9????", "E9????");
      mPadInput.setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack("0", "Akai Advance", 0, 0, true);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.setIndication(true);
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

   private void onMiniIn(final int status, final int data1, final int data2)
   {
      int channel = status & 0xF;
      int msg = status >> 4;

      //getHost().println("MIDI IN, msg: " + msg + " channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      switch (msg)
      {
         case 11:
         {
            if (50 <= data1 && data1 < 58)
            {
               int index = data1 - 50;
               int inc = data2 < 64 ? data2 : (data2 - 128);
               double scaledInc = inc / 128.0f;
               mRemoteControls.getParameter(index).inc(scaledInc);
            }
         }
      }
   }

   private void onSysexIn(final String sysex)
   {
      getHost().println("got sysex: " + sysex);
   }

   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private NoteInput mKeyboardInput;
   private NoteInput mPadInput;
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
}
