package com.bitwig.extensions.controllers.akai.apc40_mkii;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class APC40MKIIControllerExtensionDefinition extends ControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("0b134b19-a791-4aa8-8a2f-1fdd2b73c4fc");

   @Override
   public String getName()
   {
      return "APC40 mkII";
   }

   @Override
   public String getVersion()
   {
      return "1.2";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 17;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Akai";
   }

   @Override
   public String getHardwareModel()
   {
      return getName();
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Akai/APC40 MKII.pdf";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new APC40MKIIControllerExtension(this, host);
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list,
      final PlatformType platformType)
   {
      final String inputNames[] = new String[1];
      final String outputNames[] = new String[1];

      switch (platformType)
      {
      case LINUX:
         inputNames[0] = "APC40 mkII MIDI 1";
         outputNames[0] = "APC40 mkII MIDI 1";
         break;

      case WINDOWS:
      case MAC:
         inputNames[0] = "APC40 mkII";
         outputNames[0] = "APC40 mkII";
         break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }

   public static APC40MKIIControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   private static APC40MKIIControllerExtensionDefinition mInstance = new APC40MKIIControllerExtensionDefinition();
}
