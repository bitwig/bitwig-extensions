package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.UUID;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

public class ArturiaKeylab61MkIIControllerExtensionDefinition
   extends ArturiaKeylabMkIIControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("07e52a91-2ede-4092-a27f-52a42801937a");

   @Override
   public int getNumberOfKeys()
   {
      return 61;
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ArturiaKeylab61MkII(this, host);
   }

   public static ArturiaKeylab61MkIIControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab61MkIIControllerExtensionDefinition mInstance = new ArturiaKeylab61MkIIControllerExtensionDefinition();
}
