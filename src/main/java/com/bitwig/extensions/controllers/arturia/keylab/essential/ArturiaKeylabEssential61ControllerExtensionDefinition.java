package com.bitwig.extensions.controllers.arturia.keylab.essential;

import java.util.UUID;

public class ArturiaKeylabEssential61ControllerExtensionDefinition extends ArturiaKeylabEssentialControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("6ff7f378-6844-4d2a-8f99-fb823b8ba9c0");

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

   public static ArturiaKeylabEssential61ControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylabEssential61ControllerExtensionDefinition
      mInstance = new ArturiaKeylabEssential61ControllerExtensionDefinition();
}
