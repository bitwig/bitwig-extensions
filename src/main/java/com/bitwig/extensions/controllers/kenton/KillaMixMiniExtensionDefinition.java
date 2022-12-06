package com.bitwig.extensions.controllers.kenton;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class KillaMixMiniExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("e745cfa6-b392-4922-973a-9d4d8520ec9c");

   public KillaMixMiniExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Killamix Mini";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getVersion()
   {
      return "1.0";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Kenton";
   }

   @Override
   public String getHardwareModel()
   {
      return "Killamix Mini";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 11;
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
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]{"Kenton Killamix Mini"}, new String[]{"Kenton Killamix Mini"});
      }
      else if (platformType == PlatformType.MAC)
      {
         list.add(new String[]{"Kenton Killamix Mini"}, new String[]{"Kenton Killamix Mini"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[]{"Kenton Killamix Mini MIDI 1"}, new String[]{"Kenton Killamix Mini MIDI 1"});
      }
   }

   @Override
   public KillaMixMiniExtension createInstance(final ControllerHost host)
   {
      return new KillaMixMiniExtension(this, host);
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Kenton/KillaMixMini.html";
   }

}
