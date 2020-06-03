package com.bitwig.extensions.controllers.devine;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VersaKey25Definition extends VersaKeyCommonDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("3ff25b55-3169-4e71-812f-9d6d701cee98");

   public VersaKey25Definition()
   {
      super(DRIVER_ID, "VersaKey 25");
   }

   @Override
   public VersaKey25 createInstance(final ControllerHost host)
   {
      return new VersaKey25(this, host);
   }
}
