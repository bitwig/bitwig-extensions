package com.bitwig.extensions.controllers.icon;

import java.util.Arrays;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VCastProDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getName()
   {
      return "VCast Pro";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 14;
   }

   @Override
   public String getHardwareVendor()
   {
      return "iCON";
   }

   @Override
   public String getHardwareModel()
   {
      return "VCast Pro";
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
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      for (final String version : VCastDefinition.VERSIONS)
      {
         switch (platformType)
         {
            case MAC:
               list.add(new String[] {"iCON VCast Pro " + version}, new String[] {"iCON VCast Pro " + version});
               break;

            case WINDOWS:
               list.add(new String[] {"iCON VCast Pro " + version}, new String[] {"iCON VCast Pro " + version});
               break;

            case LINUX:
               list.add(new String[] {"iCON VCast Pro " + version + " MIDI 1"}, new String[] {"iCON VCast Pro " + version + " MIDI 1"});
               break;
         }
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new VCastPro(this, host);
   }

   private static final UUID ID = UUID.fromString("3a96f591-61ea-4ff8-8861-d3b0954854a4");
}
