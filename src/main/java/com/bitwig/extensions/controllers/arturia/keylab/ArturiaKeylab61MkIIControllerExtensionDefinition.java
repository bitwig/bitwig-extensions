package com.bitwig.extensions.controllers.arturia.keylab;

import java.util.UUID;

public class ArturiaKeylab61MkIIControllerExtensionDefinition extends ArturiaKeylabMkIIControllerExtensionDefinition
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

   public static ArturiaKeylab61MkIIControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static ArturiaKeylab61MkIIControllerExtensionDefinition
      mInstance = new ArturiaKeylab61MkIIControllerExtensionDefinition();
}
