package com.bitwig.extensions.controllers.novation.launchkey_mk3.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;

import java.util.UUID;

public class LaunchkeyMiniMk3ExtensionDefinition extends LaunchkeyMk3ExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("1a5ae0c1-68d3-4912-950f-cc19af519e08");


   @Override
   public String getName() {
      return "Launchkey Mini Mk3";
   }

   @Override
   public String getHardwareModel() {
      return "Launchkey Mini Mk3";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public int numberOfKeys() {
      return 25;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"MIDIIN2 (Launchkey Mini MK3 MID", "Launchkey Mini MK3 MIDI"},
            new String[]{"MIDIOUT2 (Launchkey Mini MK3 MI", "Launchkey Mini MK3 MIDI"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Launchkey Mini LKMK3 DAW Out", "Launchkey Mini MIDI Out"},
            new String[]{"Launchkey Mini LKMK3 DAW In", "Launchkey Mini LKMK3 MIDI In"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"MIDIIN2 (LKMK3 MIDI)", "LKMK3 MIDI"},
            new String[]{"MIDIOUT2 (LKMK3 MIDI)", "LKMK3 MIDI"});
      }
   }

   @Override
   public LaunchkeyMk3Extension createInstance(final ControllerHost host) {
      return new LaunchkeyMk3Extension(this, host, false, true);
   }

}
