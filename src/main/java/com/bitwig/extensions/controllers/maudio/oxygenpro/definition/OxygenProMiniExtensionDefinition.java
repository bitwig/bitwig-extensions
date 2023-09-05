package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxyConfig;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxygenProExtension;

import java.util.UUID;

public class OxygenProMiniExtensionDefinition extends OxygenProExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("1bb00489-4322-45c7-9b07-48d21393a57b");

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   protected String getKeys() {
      return "Mini";
   }

   @Override
   public OxygenProExtension createInstance(final ControllerHost host) {
      return new OxygenProExtension(this, host, new OxyConfig(4, true, false, false));
   }
   
   @Override
   public String getHelpFilePath() {
      return "Controllers/M-Audio/M-Audio Oxygen Pro Mini.pdf";
   }
}
