package com.bitwig.extensions.controllers.mackie.definition;

import java.util.UUID;

public class IconQconProXXt2ExtensionDefinition extends IconQconProXExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847b5f");

   public IconQconProXXt2ExtensionDefinition() {
      super(2);
   }

   @Override
   public UUID getId() {
      return IconQconProXXt2ExtensionDefinition.DRIVER_ID;
   }
}
