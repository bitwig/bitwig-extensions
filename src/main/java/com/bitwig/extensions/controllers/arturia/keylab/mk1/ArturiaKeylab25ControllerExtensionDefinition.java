package com.bitwig.extensions.controllers.arturia.keylab.mk1;

import java.util.UUID;

public class ArturiaKeylab25ControllerExtensionDefinition extends ArturiaKeylabControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("faa89c00-020a-11e4-9191-0800200c9a66");

   @Override
   public int getNumberOfKeys()
   {
      return 25;
   }

   @Override
   public boolean hasDrumPads()
   {
      return false;
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   public static ArturiaKeylab25ControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab25ControllerExtensionDefinition mInstance = new ArturiaKeylab25ControllerExtensionDefinition();
}
