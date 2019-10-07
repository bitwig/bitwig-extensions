package com.bitwig.extensions.controllers.keith_mcmillen;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.devine.EzCreatorCommon;

public class QuNexus extends ControllerExtension
{
   public QuNexus(final QuNexusDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.createNoteInput("", "8?????", "9?????", "A?????", "D?????").setShouldConsumeEvents(true);

      // For now this is just a bare-bones script to enable auto-detection
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {

   }
}
