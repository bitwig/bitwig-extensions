package com.bitwig.extensions.controllers.devine;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VersaKey88Definition extends VersaKeyCommonDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("e6e502da-7572-4ff8-aec8-98ebc34d88ea");

   public VersaKey88Definition()
   {
      super(DRIVER_ID, "VersaKey 88");
   }

   @Override
   public VersaKey88 createInstance(final ControllerHost host)
   {
      return new VersaKey88(this, host);
   }
}
