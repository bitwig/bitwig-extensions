package com.bitwig.extensions.controllers.novation.launch_control_xl;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchControlXlControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launch Control XL";
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
      list.add(
         new String[]{"Launch Control XL"},
         new String[]{"Launch Control XL"});

      list.add(
         new String[]{"Launch Control XL MIDI 1"},
         new String[]{"Launch Control XL MIDI 1"});

      // Weird, but it happened on Linux...
      list.add(
         new String[]{"Launch Control XL Launch Contro"},
         new String[]{"Launch Control XL Launch Contro"});
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LaunchControlXlControllerExtension(this, host);
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
      return 18;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Novation/Launch Control XL.html";
   }

   public static LaunchControlXlControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final LaunchControlXlControllerExtensionDefinition INSTANCE = new LaunchControlXlControllerExtensionDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("3ebc6cd6-d5d6-43c6-8cd1-670b111b5304");
}
