package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import java.util.UUID;

public class OxygenPro49ExtensionDefinition extends OxygenProExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("1bb00489-4322-45c7-9b07-48d21393a579");

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   protected String getKeys() {
      return "49";
   }
}
