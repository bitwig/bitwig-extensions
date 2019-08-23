package com.bitwig.extensions.controllers.roli;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.callback.EnumValueChangedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableEnumValue;

public class SeaboardRISE extends ControllerExtension
{
   public SeaboardRISE(final SeaboardRISEDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback(this::onMidi);
      final NoteInput noteInput = midiIn.createNoteInput("", "8?????", "9?????", "B?40??", "B?4A??", "D?????",
         "E?????");

      noteInput.setUseExpressiveMidi(true, 0, 48);

      mMidiOut = host.getMidiOutPort(0);

      final String[] bendRanges = { "12", "24", "36", "48", "60", "72", "84", "96" };

      final SettableEnumValue bendRange = host.getPreferences().getEnumSetting("Bend Range", "MIDI",
         bendRanges, "48");

      bendRange.addValueObserver(new EnumValueChangedCallback()
      {
         @Override
         public void valueChanged(final String newValue)
         {
            final int pb = Integer.parseInt(newValue);
            noteInput.setUseExpressiveMidi(true, 0, pb);
            sendPitchBendRangeRPN(1, pb);
         }
      });

      // Set up pitch bend sensitivity to 48 semitones
      sendPitchBendRangeRPN(1, 48);

      mCursorTrack = host.createCursorTrack(0, 0);

      final PinnableCursorDevice device = mCursorTrack.createCursorDevice("DeviceSelection", "Instrument", 0,
         CursorDeviceFollowMode.FIRST_INSTRUMENT_OR_DEVICE);

      mRemoteControlsPage = device.createCursorRemoteControlsPage(3);
      mXyPage = device.createCursorRemoteControlsPage("xy", 2, "xy");

      device.name().addValueObserver(name -> mIsEquatorSelected = name.toLowerCase().startsWith("equator"));

      for (int p = 0; p < 3; p++)
      {
         final int pf = p;
         mRemoteControlsPage.getParameter(p).markInterested();
         mRemoteControlsPage.getParameter(p).setIndication(true);
         mRemoteControlsPage.getParameter(p).value().addValueObserver(128,
            value -> setSliderValueLED(pf, value));
      }

      for (int p = 0; p < 2; p++)
      {
         mXyPage.getParameter(p).setIndication(true);
      }
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      if (status == 192)
      {
         mCursorTrack.sendMidi(status, data1, data2);
      }
      else if (status == 176)
      {
         if (mIsEquatorSelected)
         {
            switch (data1)
            {
            case 107:
            case 109:
            case 111:
            case 113:
            case 114:
               mCursorTrack.sendMidi(status, data1, data2);
               break;
            }
         }
         else
         {
            switch (data1)
            {
            case 107:
               mRemoteControlsPage.getParameter(0).set(data2, 128);
               break;
            case 109:
               mRemoteControlsPage.getParameter(1).set(data2, 128);
               break;
            case 111:
               mRemoteControlsPage.getParameter(2).set(data2, 128);
               break;

            case 113:
               mXyPage.getParameter(0).set(data2, 128);
               break;
            case 114:
               mXyPage.getParameter(1).set(data2, 128);
               break;
            }
         }
      }
   }

   void setSliderValueLED(int slider, int value)
   {
      final byte[] message = SysexBuilder.fromHex("F0 00 21 10 78 3D").addByte(20 + slider)
         .addByte(Math.max(11, value)).terminate();

      mMidiOut.sendSysex(message);
   }

   void sendPitchBendRangeRPN(int channel, int range)
   {
      // Set up MPE mode: Zone 1 15 channels
      mMidiOut.sendMidi(0xB0, 101, 0); // Registered Parameter Number (RPN) - MSB*
      mMidiOut.sendMidi(0xB0, 100, 6); // Registered Parameter Number (RPN) - LSB*
      mMidiOut.sendMidi(0xB0, 6, 15);
      mMidiOut.sendMidi(0xB0, 38, 0);

      // Set up pitch bend sensitivity to "range" semitones (00/00)
      mMidiOut.sendMidi(0xB1, 100, 0); // Registered Parameter Number (RPN) - LSB*
      mMidiOut.sendMidi(0xB1, 101, 0); // Registered Parameter Number (RPN) - MSB*
      mMidiOut.sendMidi(0xB1, 38, 0);
      mMidiOut.sendMidi(0xB1, 6, range);
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
   }

   private MidiOut mMidiOut;

   private CursorRemoteControlsPage mRemoteControlsPage;

   private boolean mIsEquatorSelected;

   private CursorTrack mCursorTrack;

   private CursorRemoteControlsPage mXyPage;
}
