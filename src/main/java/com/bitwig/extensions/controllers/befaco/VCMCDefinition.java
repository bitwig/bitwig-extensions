package com.bitwig.extensions.controllers.befaco;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VCMCDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Befaco";
   }

   @Override
   public String getHardwareModel()
   {
      return "VCMC";
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
      if (platformType == PlatformType.MAC)
      {
         list.add(new String[] {"VCMC"}, new String[] {"VCMC"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[] {"VCMC MIDI 1"}, new String[] {"VCMC MIDI 1"});
      }
      else if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[] {"VCMC"}, new String[] {"VCMC"});
      }
   }

   @Override
   public VCMC createInstance(final ControllerHost host)
   {
      return new VCMC(this, host);
   }

   @Override
   public String getName()
   {
      return "VCMC";
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
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 11;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Befaco/VCMC.pdf";
   }

   private static final UUID ID = UUID.fromString("0f60fc00-e0fe-11e9-aaef-0800200c9a66");
}
