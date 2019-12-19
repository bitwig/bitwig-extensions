package com.bitwig.extensions.controllers.roger_linn_design;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LinnStrumentDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Roger Linn Design";
   }

   @Override
   public String getHardwareModel()
   {
      return "LinnStrument";
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
      list.add(new String[] {"LinnStrument MIDI"}, new String[] {"LinnStrument MIDI"});
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Roger Linn Design/LinnStrument.pdf";
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LinnStrument(this, host);
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

   public static LinnStrumentDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final LinnStrumentDefinition
      INSTANCE = new LinnStrumentDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("B7DD06CB-63BA-4902-879E-050B09D3058F");
}
