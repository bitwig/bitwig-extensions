package com.bitwig.extensions.controllers.nativeinstruments.komplete.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolAExtension;

public class KompleteKontrolASeriesExtensionDefinition extends AbstractKompleteKontrolExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("523a5a44-409c-496b-bb74-58bcea37867d");
   private static final String MODEL = "A Series";

   public KompleteKontrolASeriesExtensionDefinition() {
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
         list.add(new String[]{"Komplete Kontrol A DAW", "KOMPLETE KONTROL A25 MIDI"},
            new String[]{"Komplete Kontrol A DAW", "KOMPLETE KONTROL A25 MIDI"});
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{"Komplete Kontrol A DAW", "KOMPLETE KONTROL A25"},
            new String[]{"Komplete Kontrol A DAW", "KOMPLETE KONTROL A25"});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{"Komplete Kontrol A DAW", "KOMPLETE KONTROL A25"},
            new String[]{"Komplete Kontrol A DAW", "KOMPLETE KONTROL A25"});
      }
   }

   @Override
   public KompleteKontrolAExtension createInstance(final ControllerHost host) {
      return new KompleteKontrolAExtension(this, host);
   }
}
