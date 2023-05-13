package com.bitwig.extensions.controllers.mackie.definition;

import java.util.UUID;

public class IconQconProXXt1ExtensionDefinition extends IconQconProXExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b5e");

   public IconQconProXXt1ExtensionDefinition() {
      super(1);
   }

   @Override
   public UUID getId() {
      return IconQconProXXt1ExtensionDefinition.DRIVER_ID;
   }
}
