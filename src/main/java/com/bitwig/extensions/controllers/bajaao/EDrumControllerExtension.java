package com.bitwig.extensions.controllers.bajaao;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Transport;

public class EDrumControllerExtension extends ControllerExtension
{
   public EDrumControllerExtension(
      final EDrumControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.setSysexCallback(this::onSysex);
      final NoteInput noteInput = midiIn.createNoteInput("E-Drums");
      noteInput.setShouldConsumeEvents(true);
   }

   private void onSysex(final String data)
   {
      switch (data)
      {
      case "f07f7f0605f7":
         mTransport.rewind();
         break;
      case "f07f7f0604f7":
         mTransport.fastForward();
         break;
      case "f07f7f0601f7":
         mTransport.stop();
         break;
      case "f07f7f0602f7":
         mTransport.play();
         break;
      case "f07f7f0606f7":
         mTransport.record();
         break;
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

   private Transport mTransport;
}
