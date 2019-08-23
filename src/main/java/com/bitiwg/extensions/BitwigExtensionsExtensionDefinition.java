package com.bitiwg.extensions;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class BitwigExtensionsExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("433bf731-1f49-496c-a667-7cb87cbcbabe");
   
   public BitwigExtensionsExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "BitwigExtensions";
   }
   
   @Override
   public String getAuthor()
   {
      return "abique";
   }

   @Override
   public String getVersion()
   {
      return "1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "Bitiwg Studio";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "BitwigExtensions";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 9;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 0;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 0;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
   }

   @Override
   public BitwigExtensionsExtension createInstance(final ControllerHost host)
   {
      return new BitwigExtensionsExtension(this, host);
   }
}
