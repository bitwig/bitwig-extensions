package com.bitwig.extensions.controllers.arturia.microlab;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;

public class ArturiaMicroLabControllerExtension extends ControllerExtension
{
   public ArturiaMicroLabControllerExtension(
      final ArturiaMicroLabControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiInput = host.getMidiInPort(0);
      mMidiOutput = host.getMidiOutPort(0);

      mNoteInput = mMidiInput.createNoteInput("Arturia MicroLab", "??????");
      mNoteInput.setShouldConsumeEvents(true);

      mMidiInput.setSysexCallback(this::onSysex);
   }

   private void onSysex(final String sysex)
   {
      getHost().println(sysex);
   }

   @Override
   public void exit()
   {

   }

   @Override
   public void flush()
   {

   }

   private MidiIn mMidiInput;
   private MidiOut mMidiOutput;
   private NoteInput mNoteInput;
}
