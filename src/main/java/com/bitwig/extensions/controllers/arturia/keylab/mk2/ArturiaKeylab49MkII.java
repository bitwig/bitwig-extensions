package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;

public class ArturiaKeylab49MkII extends ArturiaKeylabMkII
{
   public ArturiaKeylab49MkII(
      final ArturiaKeylab49MkIIControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   protected void initHardwareLayout(final HardwareSurface surface)
   {
      surface.setPhysicalSize(793, 297);
   }

}
