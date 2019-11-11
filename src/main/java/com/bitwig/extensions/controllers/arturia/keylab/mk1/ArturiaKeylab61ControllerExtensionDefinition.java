package com.bitwig.extensions.controllers.arturia.keylab.mk1;

import java.util.UUID;

public class ArturiaKeylab61ControllerExtensionDefinition extends ArturiaKeylabControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("2d1a0610-0210-11e4-9191-0800200c9a66");

   @Override
   public int getNumberOfKeys()
   {
      return 61;
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

   public static ArturiaKeylab61ControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab61ControllerExtensionDefinition mInstance = new ArturiaKeylab61ControllerExtensionDefinition();
}
