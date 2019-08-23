package com.bitiwg.extensions.controllers.devine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class EzCreatorKeyDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Devine";
   }

   @Override
   public String getHardwareModel()
   {
      return "EZ-Creator Key";
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
            list.add(new String[] {"EZ-Creator Key MIDI 1"}, new String[] {"EZ-Creator Key MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"EZ-Creator Key"}, new String[] {"EZ-Creator Key"});
            break;

         case MAC:
            list.add(new String[] {"EZ-Creator Key"}, new String[] {"EZ-Creator Key"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new EzCreatorKey(this, host);
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
      return 7;
   }

   public static EzCreatorKeyDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Devine/Ez-Creator Keys.html";
   }

   private static final EzCreatorKeyDefinition INSTANCE = new EzCreatorKeyDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("3be22ab5-b159-4bd4-b88b-1c0fe6ce56fb");
}
