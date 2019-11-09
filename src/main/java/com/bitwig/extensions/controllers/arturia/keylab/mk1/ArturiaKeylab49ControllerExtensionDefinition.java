package com.bitwig.extensions.controllers.arturia.keylab.mk1;

import java.util.UUID;

public class ArturiaKeylab49ControllerExtensionDefinition extends ArturiaKeylabControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("360d96b0-0210-11e4-9191-0800200c9a66");

   @Override
   public int getNumberOfKeys()
   {
      return 49;
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

   public static ArturiaKeylab49ControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab49ControllerExtensionDefinition mInstance = new ArturiaKeylab49ControllerExtensionDefinition();
}
