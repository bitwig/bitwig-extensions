package com.bitwig.extensions.controllers.devine;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VersaKey49Definition extends VersaKeyCommonDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("5c39f30a-7f9d-4d4b-aeaa-2e9e32230828");

   public VersaKey49Definition()
   {
      super(DRIVER_ID, "VersaKey 49");
   }

   @Override
   public VersaKey49 createInstance(final ControllerHost host)
   {
      return new VersaKey49(this, host);
   }
}
