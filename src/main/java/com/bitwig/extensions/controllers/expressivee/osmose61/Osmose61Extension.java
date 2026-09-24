package com.bitwig.extensions.controllers.expressivee.osmose61;

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
import com.bitwig.extensions.controllers.expressivee.common.SynthPreferences;


public class Osmose61Extension extends ControllerExtension
{
   private TrackManager mTrackManager;
   private ApplicationManager mApplicationManager;
   private TransportManager mTransportManager;
   private ExternalMidiPreferences mExternalMidiPreference;
   private SynthPreferences mSynthPreferences;

   protected Osmose61Extension(final Osmose61ExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init() {
      final ControllerHost host = getHost();

      final HardwareSurface surface = host.createHardwareSurface();

      mSynthPreferences = new SynthPreferences(host);
      mExternalMidiPreference = new ExternalMidiPreferences(host);
      mTransportManager = new TransportManager(host, surface, host.getMidiInPort(2), host.getMidiOutPort(2));
      mTrackManager = new TrackManager(host, surface, host.getMidiInPort(2), host.getMidiOutPort(2), false);
      mApplicationManager = new ApplicationManager(host, surface, host.getMidiInPort(2), host.getMidiOutPort(2));

      final NoteInput soundEngineNoteInput = host.getMidiInPort(1).createNoteInput("Osmose 61 sound engine", "??????");
      soundEngineNoteInput.setShouldConsumeEvents(true);
      soundEngineNoteInput.setUseExpressiveMidi(true, 0, 48);

      final NoteInput playNoteInput = host.getMidiInPort(0).createNoteInput("Osmose 61 play", "??????");
      playNoteInput.setShouldConsumeEvents(true);
      playNoteInput.setUseExpressiveMidi(true, 0, 48);

      if (mSynthPreferences.soundEngineInAllInputs()) {
         soundEngineNoteInput.includeInAllInputs();
      }

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
}
