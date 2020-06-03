package com.bitwig.extensions.controllers.devine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class EzCreatorKeyDefinition extends EzCreatorKeyCommonDefinition
{
   private static final UUID EXTENSION_UUID = UUID.fromString("0f5fb728-6c41-489f-a98a-5b7ed019550f");

   public EzCreatorKeyDefinition()
   {
      super(EXTENSION_UUID, "EZ-Creator Key");
   }

   @Override
   public EzCreatorKey createInstance(final ControllerHost host)
   {
      return new EzCreatorKey(this, host);
   }
}
