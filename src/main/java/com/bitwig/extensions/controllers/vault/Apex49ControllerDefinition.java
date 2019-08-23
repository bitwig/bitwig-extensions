package com.bitwig.extensions.controllers.vault;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class Apex49ControllerDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Vault";
   }

   @Override
   public String getHardwareModel()
   {
      return "Apex 49";
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
            list.add(new String[] {"Apex 49 MIDI 1"}, new String[] {"Apex 49 MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"Apex 49"}, new String[] {"Apex 49"});
            break;

         case MAC:
            list.add(new String[] {"Apex 49"}, new String[] {"Apex 49"});
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

   public static Apex49ControllerDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final Apex49ControllerDefinition INSTANCE = new Apex49ControllerDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("f311ed46-8f80-406c-abf2-9d172d762dac");
}
