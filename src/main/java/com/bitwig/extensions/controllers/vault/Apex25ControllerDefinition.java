package com.bitwig.extensions.controllers.vault;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class Apex25ControllerDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Vault";
   }

   @Override
   public String getHardwareModel()
   {
      return "Apex 25";
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
            list.add(new String[] {"Apex 25 MIDI 1"}, new String[] {"Apex 25 MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"Apex 25"}, new String[] {"Apex 25"});
            break;

         case MAC:
            list.add(new String[] {"Apex 25"}, new String[] {"Apex 25"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ApexControllerExtension(this, host, false);
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
      return "Controllers/Vault/Apex.html";
   }

   public static Apex25ControllerDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final Apex25ControllerDefinition INSTANCE = new Apex25ControllerDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("f739a976-64bb-4ce7-8ded-a2783fea89a7");
}
