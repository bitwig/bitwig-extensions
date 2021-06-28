package com.bitwig.extensions.controllers.icon;

import java.util.Arrays;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VCastDefinition extends ControllerExtensionDefinition
{
   public static String[] VERSIONS = {"V1.00", "V1.01", "V1.02", "V1.03", "V1.04", "V1.05", "V1.06", "V1.07"};

   @Override
   public String getName()
   {
      return "VCast";
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
      return 12;
   }

   @Override
   public String getHardwareVendor()
   {
      return "iCON";
   }

   @Override
   public String getHardwareModel()
   {
      return "VCast";
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
      for (final String version : VERSIONS)
      {
         switch (platformType)
         {
            case MAC:
               list.add(new String[] {"iCON VCast " + version}, new String[] {"iCON VCast " + version});
               break;

            case WINDOWS:
               list.add(new String[] {"iCON VCast " + version}, new String[] {"iCON VCast " + version});
               break;

            case LINUX:
               list.add(new String[] {"iCON VCast " + version + " MIDI 1"}, new String[] {"iCON VCast " + version + " MIDI 1"});
               break;
         }
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new VCast(this, host);
   }

   private static final UUID ID = UUID.fromString("53bae222-b3ce-43f0-85ad-ef31c224f6c2");
}
