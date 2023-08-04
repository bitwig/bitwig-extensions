package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import java.util.UUID;

public class OxygenPro61ExtensionDefinition extends OxygenProExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("b75a10e5-3f23-4b17-9f32-23c0ff99e0bc");

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   protected String getKeys() {
      return "61";
   }
}
