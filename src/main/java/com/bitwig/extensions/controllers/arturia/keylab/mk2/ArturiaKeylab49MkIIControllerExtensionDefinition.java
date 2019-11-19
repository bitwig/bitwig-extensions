package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.UUID;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

public class ArturiaKeylab49MkIIControllerExtensionDefinition extends ArturiaKeylabMkIIControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("006b42e9-d4af-499e-89ba-e6df0fc36a46");

   @Override
   public int getNumberOfKeys()
   {
      return 49;
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ArturiaKeylab49MkII(this, host);
   }

   public static ArturiaKeylab49MkIIControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab49MkIIControllerExtensionDefinition
      mInstance = new ArturiaKeylab49MkIIControllerExtensionDefinition();
}
