package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.UUID;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

public class ArturiaKeylab88MkIIControllerExtensionDefinition
   extends ArturiaKeylabMkIIControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("370bba64-d8bd-4edb-a272-1e4c883eea05");

   @Override
   public int getNumberOfKeys()
   {
      return 88;
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ArturiaKeylab88MkII(this, host);
   }

   public static ArturiaKeylab88MkIIControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab88MkIIControllerExtensionDefinition mInstance = new ArturiaKeylab88MkIIControllerExtensionDefinition();
}
