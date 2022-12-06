package com.bitwig.extensions.controllers.arturia.beatsteppro;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class BeatStepProExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("6ac59dac-77c4-4f93-b6a5-fb1dfcacffac");

   public BeatStepProExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "Beatstep Pro";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "0.9";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Arturia/Arturia BeatStep Pro.pdf";
   }

   @Override
   public String getHardwareVendor() {
      return "Arturia";
   }

   @Override
   public String getHardwareModel() {
      return "Beatstep Pro";
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
         list.add(new String[]{"Arturia BeatStep Pro", "MIDIIN2 (Arturia BeatStep Pro)"},
            new String[]{"Arturia BeatStep Pro", "MIDIOUT2 (Arturia BeatStep Pro)"});
      } else if (platformType == PlatformType.MAC) {
         list.add(
            new String[]{"Arturia BeatStep Pro Arturia BeatStepPro", "Arturia BeatStep Pro BeatStepPro OutEditor"},
            new String[]{"Arturia BeatStep Pro Arturia BeatStepPro", "Arturia BeatStep Pro BeatStepProInEditor"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"BeatStepPro", "MIDIIN2 (Arturia BeatStep Pro)"},
            new String[]{"BeatStepPro", "MIDIOUT2 (Arturia BeatStep Pro)"});
      }
   }

   @Override
   public BeatStepProExtension createInstance(final ControllerHost host) {
      return new BeatStepProExtension(this, host);
   }
}
