package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

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
}
