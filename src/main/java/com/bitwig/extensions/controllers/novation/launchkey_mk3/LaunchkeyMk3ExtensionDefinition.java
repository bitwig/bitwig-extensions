package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchkeyMk3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("4840b76d-318e-40c8-bab0-0eb780061276");

   @Override
   public String getName() {
      return "Launchkey 49 Mk3";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "0.0b";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Novation";
   }

   @Override
   public String getHardwareModel() {
      return "Launchkey 49 Mk3";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 15;
   }

   @Override
   public int getNumMidiInPorts() {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 2;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"MIDIIN2 (LKMK3 MIDI)", "LKMK3 MIDI"},
            new String[]{"MIDIOUT2 (LKMK3 MIDI)", "LKMK3 MIDI"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Launchkey MK3 49 LKMK3 DAW Out", "Launchkey MK3 49 LKMK3 MIDI Out"},
            new String[]{"Launchkey MK3 49 LKMK3 DAW In", "Launchkey MK3 49 LKMK3 MIDI In"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"MIDIIN2 (LKMK3 MIDI)", "LKMK3 MIDI"},
            new String[]{"MIDIOUT2 (LKMK3 MIDI)", "LKMK3 MIDI"});
      }
   }

   @Override
   public LaunchkeyMk3Extension createInstance(final ControllerHost host) {
      return new LaunchkeyMk3Extension(this, host);
   }
}
