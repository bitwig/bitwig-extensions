package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.CcButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.definition.OxygenProExtensionDefinition;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class OxygenProExtension extends ControllerExtension {

   private static final String OXYGEN_SYSEX = "F0 00 01 05 7F 00 00 %02X 00 01 %02X F7";

   private Layer mainLayer;
   private ControllerHost host;
   private HardwareSurface surface;
   private MidiOut midiOut;
   private MidiIn midiInKey;
   private final OxyConfig config;

   public OxygenProExtension(final OxygenProExtensionDefinition definition, final ControllerHost host,
                             OxyConfig config) {
      super(definition, host);
      this.config = config;
   }

   @Override
   public void init() {
      host = getHost();
      DebugOutOxy.registerHost(host);
      final Context diContext = new Context(this);
      diContext.registerService(OxyConfig.class, config);
      surface = diContext.getService(HardwareSurface.class);
      MidiIn midiIn = host.getMidiInPort(0);
      midiInKey = host.getMidiInPort(1);
      midiOut = host.getMidiOutPort(0);
      diContext.registerService(MidiIn.class, midiIn);
      diContext.registerService(MidiOut.class, midiOut);
      MidiProcessor midiProcessor = new MidiProcessor(host, midiIn, midiOut);
      diContext.registerService(MidiProcessor.class, midiProcessor);

      midiInKey.createNoteInput("MIDI", "8?????", "9?????", "A?????", "D?????");

      mainLayer = diContext.createLayer("MAIN");
      initSysexMessages();
      host.showPopupNotification("Oxygen Pro ");
      DebugOutOxy.println(" >> OXY");
      initTransport(diContext);
      mainLayer.setIsActive(true);
      diContext.activate();
   }

   private void initSysexMessages() {
      midiOut.sendSysex("F0 7E 7F 06 01 F7");
//      midiOut.sendSysex("F0 00 01 05 7F 00 00 6D 00 01 02 F7"); // Changing to the Bitwig DAW-Program
//      midiOut.sendSysex("F0 00 01 05 7F 00 00 6E 00 01 02 F7");
//      midiOut.sendSysex("F0 00 01 05 7F 00 00 6E 00 01 07 F7");
//      midiOut.sendSysex("F0 00 01 05 7F 00 00 6B 00 01 01 F7");
//      midiOut.sendSysex("F0 00 01 05 7F 00 00 6C 00 01 03 F7"); // activate Light Control
      sendOxyCommand(0x6D, 2);
      sendOxyCommand(0x6E, 2);
      sendOxyCommand(0x6E, 7);
      sendOxyCommand(0x6B, 1);
      sendOxyCommand(0x6C, 3);
   }

   private void sendOxyCommand(int commandId, int arg) {
      String message = String.format(OXYGEN_SYSEX, commandId, arg);
      midiOut.sendSysex(message);
   }

   void initTransport(Context diContext) {
      Transport transport = diContext.getService(Transport.class);
      HwElements hwElements = diContext.getService(HwElements.class);

      CcButton playButton = hwElements.getButton(OxygenCcAssignments.PLAY);
      playButton.bindPressed(mainLayer, () -> {
         transport.play();
      });

   }


   @Override
   public void exit() {
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }

}
