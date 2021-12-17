package com.bitwig.extensions.controllers.icon;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

/**
 * VCast Rx is a Bluetooth receiver for VCast and VCast Pro
 */
public class VCastRxDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getName()
   {
      return "VCast Rx";
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
      return "VCast Rx";
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
               list.add(new String[] {"iCON VCast Rx " + version}, new String[] {"iCON VCast Rx " + version});
               break;

            case WINDOWS:
               list.add(new String[] {"iCON VCast Rx " + version}, new String[] {"iCON VCast Rx " + version});
               break;

            case LINUX:
               list.add(new String[] {"iCON VCast Rx " + version + " MIDI 1"}, new String[] {"iCON VCast Rx " + version + " MIDI 1"});
               break;
         }
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new VCastPro(this, host);
   }

   private static final UUID ID = UUID.fromString("ae9aae72-1224-4338-8efb-171479ea2a50");
}
