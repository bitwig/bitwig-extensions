package com.bitwig.extensions.controllers.mackie.definition;

import java.util.UUID;

public class IconQconProXXt3ExtensionDefinition extends IconQconProXExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b60");

   public IconQconProXXt3ExtensionDefinition() {
      super(3);
   }

   @Override
   public UUID getId() {
      return IconQconProXXt3ExtensionDefinition.DRIVER_ID;
   }
}
