package com.bitwig.extensions.controllers.vault;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class Apex61ControllerDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Vault";
   }

   @Override
   public String getHardwareModel()
   {
      return "Apex 61";
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
            list.add(new String[] {"Apex 61 MIDI 1"}, new String[] {"Apex 61 MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"Apex 61"}, new String[] {"Apex 61"});
            break;

         case MAC:
            list.add(new String[] {"Apex 61"}, new String[] {"Apex 61"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ApexControllerExtension(this, host, true);
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

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/Vault/Apex.html";
   }

   public static Apex61ControllerDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final Apex61ControllerDefinition INSTANCE = new Apex61ControllerDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("16cfb3ae-7f6a-4fed-bfc0-e64e8dcdbf4c");
}
