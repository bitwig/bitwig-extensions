package com.bitwig.extensions.controllers.nativeinstruments.komplete.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolAExtension;

public class KompleteKontrolMSeriesExtensionDefinition extends AbstractKompleteKontrolExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("963457d6-fa2f-4974-b61d-8c6881dc097e");
   private static final String MODEL = "M32";

   public KompleteKontrolMSeriesExtensionDefinition() {
      super(MODEL);
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Native Instruments/Komplete Kontrol A_M-Series/Komplete Kontrol A_M-Series.pdf";
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{"Komplete Kontrol M DAW", "KOMPLETE KONTROL M32 MIDI"},
            new String[]{"Komplete Kontrol M DAW", "KOMPLETE KONTROL M32 MIDI"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Komplete Kontrol M DAW", "KOMPLETE KONTROL M32"},
            new String[]{"Komplete Kontrol M DAW", "KOMPLETE KONTROL M32"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"Komplete Kontrol M DAW", "KOMPLETE KONTROL M32"},
            new String[]{"Komplete Kontrol M DAW", "KOMPLETE KONTROL M32"});
      }
   }

   @Override
   public KompleteKontrolAExtension createInstance(final ControllerHost host) {
      return new KompleteKontrolAExtension(this, host);
   }
}
