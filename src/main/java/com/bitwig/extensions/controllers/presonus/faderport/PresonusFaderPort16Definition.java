package com.bitwig.extensions.controllers.presonus.faderport;

import java.util.UUID;

import com.bitwig.extension.controller.api.ControllerHost;

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
   String sysexDeviceID()
   {
      return "16";
   }

   @Override
   public PresonusFaderPort16 createInstance(final ControllerHost host)
   {
      return new PresonusFaderPort16(this, host);
   }

}
