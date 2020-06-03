package com.bitwig.extensions.controllers.devine;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VersaKey61Definition extends VersaKeyCommonDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("af8fa1e2-477b-4246-bdd1-d5869e36e4ad");

   public VersaKey61Definition()
   {
      super(DRIVER_ID, "VersaKey 61");
   }

   @Override
   public VersaKey61 createInstance(final ControllerHost host)
   {
      return new VersaKey61(this, host);
   }
}
