package com.bitwig.extensions.controllers.arturia.microlab;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class ArturiaMicroLabControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Arturia";
   }

   @Override
   public String getHardwareModel()
   {
      return "MicroLab";
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
         case MAC:
         case WINDOWS:
            list.add(new String[]{"Arturia MicroLab"}, new String[]{"Arturia MicroLab"});
            break;
         case LINUX:
            list.add(new String[]{"Arturia MicroLab MIDI 1"}, new String[]{"Arturia MicroLab MIDI 1"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ArturiaMicroLabControllerExtension(this, host);
   }

   @Override
   public String getName()
   {
      return "MicroLab";
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

   public String getHelpFilePath()
   {
      return "Controllers/Arturia/MicroLab.html";
   }

   public static ArturiaMicroLabControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   private final static ArturiaMicroLabControllerExtensionDefinition INSTANCE = new ArturiaMicroLabControllerExtensionDefinition();
   private final static UUID EXTENSION_UUID = UUID.fromString("DD87946C-D5B9-4D8C-ADB1-742212103E7E");
}
