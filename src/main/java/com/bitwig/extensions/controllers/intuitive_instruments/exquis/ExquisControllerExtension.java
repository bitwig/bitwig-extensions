package com.bitwig.extensions.controllers.intuitive_instruments.exquis;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class ExquisControllerExtension extends ControllerExtension
{
   protected ExquisControllerExtension(ExquisControllerExtensionDefinition definition, ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final var host = getHost();

      mMidiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      mNoteInput = mMidiIn.createNoteInput("", "8?????", "9?????", "B?40??", "B?4A??", "D?????", "E?????");

      final String[] bendRanges = { "12", "24", "36", "48", "60", "72", "84", "96" };
      final var bendRange = host.getPreferences().getEnumSetting("Bend Range", "MIDI", bendRanges, "48");
      bendRange.addValueObserver(this::setPitchBendRange);

      setPitchBendRange(bendRange.get());

      final var cursorTrack = host.createCursorTrack(0, 0);
      final var cursorDevice = cursorTrack.createCursorDevice();
      final var cursorRemoteControlsPage = cursorDevice.createCursorRemoteControlsPage(4);

      final var layers = new Layers(this);
      final var mainLayer = new Layer(layers, "Main");

      final var hardwareSurface = host.createHardwareSurface();
      final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[4];

      for (int i = 0; i < 4; ++i)
      {
         final var parameter = cursorRemoteControlsPage.getParameter(i);
         parameter.markInterested();
         parameter.setIndication(true);

         knobs[i] = hardwareSurface.createAbsoluteHardwareKnob(Integer.toString(i + 1));
         knobs[i].setIndexInGroup(i);
         knobs[i].setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(41 + i));

         mainLayer.bind(knobs[i], parameter);
      }

      mainLayer.activate();
   }

   void setPitchBendRange(final String range)
   {
      final int pb = Integer.parseInt(range);
      mNoteInput.setUseExpressiveMidi(true, 0, pb);
      sendPitchBendRangeRPN(1, pb);
   }

   void sendPitchBendRangeRPN(final int channel, final int range)
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

   private MidiIn mMidiIn;

   private MidiOut mMidiOut;

   private NoteInput mNoteInput;
}
