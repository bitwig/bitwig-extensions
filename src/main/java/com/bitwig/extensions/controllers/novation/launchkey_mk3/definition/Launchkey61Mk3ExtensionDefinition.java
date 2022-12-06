package com.bitwig.extensions.controllers.novation.launchkey_mk3.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;

import java.util.UUID;

public class Launchkey61Mk3ExtensionDefinition extends LaunchkeyMk3ExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("832368d3-c9fa-479b-91a5-2d8c194a1a76");

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public int numberOfKeys() {
      return 61;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"MIDIIN2 (LKMK3 MIDI)", "LKMK3 MIDI"},
            new String[]{"MIDIOUT2 (LKMK3 MIDI)", "LKMK3 MIDI"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Launchkey MK3 61 LKMK3 DAW Out", "Launchkey MK3 61 LKMK3 MIDI Out"},
            new String[]{"Launchkey MK3 61 LKMK3 DAW In", "Launchkey MK3 61 LKMK3 MIDI In"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"MIDIIN2 (LKMK3 MIDI)", "LKMK3 MIDI"},
            new String[]{"MIDIOUT2 (LKMK3 MIDI)", "LKMK3 MIDI"});
      }
   }

   @Override
   public LaunchkeyMk3Extension createInstance(final ControllerHost host) {
      return new LaunchkeyMk3Extension(this, host, true);
   }

}
