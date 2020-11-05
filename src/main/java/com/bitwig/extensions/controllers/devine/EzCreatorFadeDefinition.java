package com.bitwig.extensions.controllers.devine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class EzCreatorFadeDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Devine";
   }

   @Override
   public String getHardwareModel()
   {
      return "EZ-Creator Fade";
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
      switch (platformType)
      {
         case LINUX:
            list.add(new String[] {"EZ-Creator Fade MIDI 1"}, new String[] {"EZ-Creator Fade MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"EZ-Creator Fade"}, new String[] {"EZ-Creator Fade"});
            break;

         case MAC:
            list.add(new String[] {"EZ-Creator Fade"}, new String[] {"EZ-Creator Fade"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new EzCreatorFade(this, host);
   }

   @Override
   public String getName()
   {
      return getHardwareModel();
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
      return EXTENSION_UUID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return EzCreatorCommon.REQUIRED_API_VERSION;
   }

   public static EzCreatorFadeDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Devine/EZ-Creator Fade.html";
   }

   private static final EzCreatorFadeDefinition INSTANCE = new EzCreatorFadeDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("bd5afaf4-b4a1-4637-9100-930dc67e9af1");
}
