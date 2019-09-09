package com.bitwig.extensions.controllers.arturia.keylab;

import java.util.UUID;

public class ArturiaKeylabEssential49ControllerExtensionDefinition extends ArturiaKeylabEssentialControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("e143e7ff-60f3-42c2-baed-4315ecf05f66");

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

   public static ArturiaKeylabEssential49ControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylabEssential49ControllerExtensionDefinition
      mInstance = new ArturiaKeylabEssential49ControllerExtensionDefinition();
}
