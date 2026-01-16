package com.bitwig.extensions.controllers.expressivee.osmoseCE61;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.HardwareSurface;

import com.bitwig.extensions.controllers.expressivee.common.ApplicationManager;
import com.bitwig.extensions.controllers.expressivee.common.Manager;
import com.bitwig.extensions.controllers.expressivee.common.TrackManager;
import com.bitwig.extensions.controllers.expressivee.common.TransportManager;
import com.bitwig.extensions.controllers.expressivee.common.ExternalMidiPreferences;

public class OsmoseCE61Extension extends ControllerExtension {
   private TrackManager mTrackManager;
   private ApplicationManager mApplicationManager;
   private ExternalMidiPreferences mExternalMidiPreference;

   protected OsmoseCE61Extension(final OsmoseCE61ExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      final ControllerHost host = getHost();

      final HardwareSurface surface = host.createHardwareSurface();

      mExternalMidiPreference = new ExternalMidiPreferences(host);
      mTransportManager = new TransportManager(host, surface, host.getMidiInPort(1), host.getMidiOutPort(1));
      mTrackManager = new TrackManager(host, surface, host.getMidiInPort(1), host.getMidiOutPort(1), false);
      mApplicationManager = new ApplicationManager(host, surface, host.getMidiInPort(1), host.getMidiOutPort(1));

      final NoteInput playNoteInput = host.getMidiInPort(0).createNoteInput("Osmose CE 61 play", "??????");
      playNoteInput.setShouldConsumeEvents(true);
      playNoteInput.setUseExpressiveMidi(true, 0, 48);

      if (mExternalMidiPreference.playInAllInputs()) {
         playNoteInput.includeInAllInputs();
      }

      mTransportManager.sendSysexConnectionInfos(true);

      mTransportManager.init();
      mTrackManager.init();
      mApplicationManager.init();
   }

   @Override
   public void exit() {
      mTransportManager.sendSysexConnectionInfos(false);
   }

   @Override
   public void flush() {
   }

   private TransportManager mTransportManager;
}
