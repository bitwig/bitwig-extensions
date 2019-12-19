package com.bitwig.extensions.controllers.devine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class EzCreatorPlusDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Devine";
   }

   @Override
   public String getHardwareModel()
   {
      return "EZ-Creator Plus";
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
            list.add(new String[] {"EZ-Creator Plus MIDI 1"}, new String[] {"EZ-Creator Plus MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"EZ-Creator Plus"}, new String[] {"EZ-Creator Plus"});
            break;

         case MAC:
            list.add(new String[] {"EZ-Creator Plus"}, new String[] {"EZ-Creator Plus"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new EzCreatorPlus(this, host);
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

   public static EzCreatorPlusDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Devine/Ez-Creator Plus.html";
   }

   private static final EzCreatorPlusDefinition INSTANCE = new EzCreatorPlusDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("3be22ab5-b159-4bd4-b88b-1c0fe6ce56fb");
}
