package com.bitwig.extensions.controllers.roli;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class SeaboardRISEDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "ROLI";
   }

   @Override
   public String getHardwareModel()
   {
      return "Seaboard RISE";
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
      list.add(new String[] {"Seaboard RISE"}, new String[] {"Seaboard RISE"});
      list.add(new String[] {"Seaboard RISE MIDI 1"}, new String[] {"Seaboard RISE MIDI 1"});
      list.add(new String[] {"Seaboard RISE 49"}, new String[] {"Seaboard RISE 49"});
      list.add(new String[] {"ROLI Seaboard RISE"}, new String[] {"ROLI Seaboard RISE"});
      list.add(new String[] {"ROLI Seaboard RISE 49"}, new String[] {"ROLI Seaboard RISE 49"});
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new SeaboardRISE(this, host);
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/ROLI/Seaboard RISE.pdf";
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
      return "2.4";
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

   public static SeaboardRISEDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final SeaboardRISEDefinition
      INSTANCE = new SeaboardRISEDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("61A1536C-D7D3-4D76-9733-BA9E7012F091");
}
