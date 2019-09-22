package com.bitwig.extensions.controllers.presonus;

import java.util.UUID;

public class PresonusFaderPort8Definition extends PresonusFaderPortDefinition
{
   private final static UUID ID = UUID.fromString("122775f6-6726-4a28-8dcc-f358c175f749");

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   int channelCount()
   {
      return 8;
   }

   @Override
   int sysexDeviceID()
   {
      return 2;
   }
}
