package com.bitwig.extensions.controllers.mackie.definition;

import java.util.UUID;

public class IconQconProG2X3ExtensionDefinition extends IconQconProG2ExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("807d0bf1-9560-494f-a3a7-a24cf25db51c");

   public IconQconProG2X3ExtensionDefinition() {
      super(3);
   }

   @Override
   public UUID getId() {
      return IconQconProG2X3ExtensionDefinition.DRIVER_ID;
   }
}
