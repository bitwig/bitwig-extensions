package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxyConfig;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxygenProExtension;

import java.util.UUID;

public class OxygenPro25ExtensionDefinition extends OxygenProExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("2bb01489-4332-45c7-9b07-48d21393b579");

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   protected String getKeys() {
      return "25";
   }

   @Override
   public OxygenProExtension createInstance(final ControllerHost host) {
      return new OxygenProExtension(this, host, new OxyConfig(8, false, true, true));
   }
}
