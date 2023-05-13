package com.bitwig.extensions.controllers.mackie.definition;

import java.util.UUID;

public class IconQconProG2X1ExtensionDefinition extends IconQconProG2ExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("d32bead3-83e4-4dc2-90f8-eb3a1a847bf0");

   public IconQconProG2X1ExtensionDefinition() {
      super(1);
   }

   @Override
   public UUID getId() {
      return IconQconProG2X1ExtensionDefinition.DRIVER_ID;
   }
}
