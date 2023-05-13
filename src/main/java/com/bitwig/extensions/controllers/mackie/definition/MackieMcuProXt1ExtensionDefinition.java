package com.bitwig.extensions.controllers.mackie.definition;

import java.util.UUID;

public class MackieMcuProXt1ExtensionDefinition extends MackieMcuProExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-81ad-1de77ffa2daa");

   public MackieMcuProXt1ExtensionDefinition() {
      super(1);
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

}
