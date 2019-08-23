package com.bitwig.extensions.controllers.bajaao;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class EDrumControllerExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("d60cb2f2-9808-4145-8d4b-cc2579c1b6a3");

   @Override
   public String getName()
   {
      return "EDrum";
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
   public int getRequiredAPIVersion()
   {
      return 8;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Vault";
   }

   @Override
   public String getHardwareModel()
   {
      return "ED10M";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 0;
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list,
      final PlatformType platformType)
   {
      switch (platformType)
      {
         case WINDOWS:
            list.add(new String[] { "E-DRUM" }, new String[0]);
            list.add(new String[] { "E-DRUMS" }, new String[0]);
            break;
         case MAC:
            list.add(new String[] { "E-DRUM" }, new String[0]);
            list.add(new String[] { "E-DRUMS" }, new String[0]);
            break;
         case LINUX:
            list.add(new String[] { "E-DRUM MIDI 1" }, new String[0]);
            list.add(new String[] { "E-DRUMS MIDI 1" }, new String[0]);
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new EDrumControllerExtension(this, host);
   }

   public static EDrumControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/Generic/E-Drum.html";
   }

   private static EDrumControllerExtensionDefinition INSTANCE = new EDrumControllerExtensionDefinition();
}
