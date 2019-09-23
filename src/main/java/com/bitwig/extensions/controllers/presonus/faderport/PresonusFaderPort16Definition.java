package com.bitwig.extensions.controllers.presonus.faderport;

import java.util.UUID;

public class PresonusFaderPort16Definition extends PresonusFaderPortDefinition
{
   private final static UUID ID = UUID.fromString("5610a656-af63-42ab-b2ea-bcdf87a61b53");

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   int channelCount()
   {
      return 16;
   }

   @Override
   int sysexDeviceID()
   {
      return 16;
   }
}
