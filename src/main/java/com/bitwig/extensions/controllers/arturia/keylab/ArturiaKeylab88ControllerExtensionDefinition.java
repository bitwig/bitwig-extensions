package com.bitwig.extensions.controllers.arturia.keylab;

import java.util.UUID;

public class ArturiaKeylab88ControllerExtensionDefinition extends ArturiaKeylabControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("633d85ce-d7f8-4f1e-b9aa-54cce2b01e0d");

   @Override
   public int getNumberOfKeys()
   {
      return 88;
   }

   @Override
   public boolean hasDrumPads()
   {
      return true;
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   public static ArturiaKeylab88ControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab88ControllerExtensionDefinition mInstance = new ArturiaKeylab88ControllerExtensionDefinition();
}
