package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;

import java.util.UUID;

public class OxygenPro88ExtensionDefinition extends OxygenProExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("fd0e72e1-2910-4abb-823c-05b31523c3d9");

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   protected String getKeys() {
      return "88";
   }

   public String getHardwareModel() {
      return "Hammer 88 Pro";
   }

   @Override
   public String getName() {
      return "M-Audio Hammer 88 Pro";
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"Hammer 88 Pro", "MIDIIN3 (Hammer 88 Pro)"},
            new String[]{"Hammer 88 Pro", "MIDIOUT3 (Hammer 88 Pro)"});
      } else if (platformType == PlatformType.MAC || platformType == PlatformType.LINUX) {
         list.add(new String[]{"Hammer 88 Pro Mackie/HUI", "Hammer 88 Pro USB MIDI"},
            new String[]{"Hammer 88 Pro Mackie/HUI", "Hammer 88 Pro USB MIDI"});
      }
   }
}
