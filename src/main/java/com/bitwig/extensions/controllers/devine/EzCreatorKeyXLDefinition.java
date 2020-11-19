package com.bitwig.extensions.controllers.devine;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;


public class EzCreatorKeyXLDefinition extends EzCreatorKeyCommonDefinition
{
   private static final UUID EXTENSION_UUID = UUID.fromString("9043a14c-4530-49c2-8e32-e7c006aa0d5f");

   public EzCreatorKeyXLDefinition()
   {
      super(EXTENSION_UUID, "EZ-Creator Key XL");
   }

   @Override
   public EzCreatorKeyXL createInstance(final ControllerHost host)
   {
      return new EzCreatorKeyXL(this, host);
   }
}
