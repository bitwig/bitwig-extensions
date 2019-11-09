package com.bitwig.extensions.controllers.novation.launchkey_mini;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchkeyMiniControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launchkey Mini";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 2;
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      switch (platformType)
      {
         case MAC:
            list.add(
               new String[]{"Launchkey Mini LK Mini MIDI", "Launchkey Mini LK Mini InControl"},
               new String[]{"Launchkey Mini LK Mini MIDI", "Launchkey Mini LK Mini InControl"});
            break;

         case WINDOWS:
            list.add(
               new String[]{"Launchkey Mini", "MIDIIN2 (Launchkey Mini)"},
               new String[]{"Launchkey Mini", "MIDIOUT2 (Launchkey Mini)"});
            break;

         case LINUX:
            list.add(
               new String[]{"Launchkey Mini MIDI 1", "Launchkey Mini MIDI 2"},
               new String[]{"Launchkey Mini MIDI 1", "Launchkey Mini MIDI 2"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LaunchkeyMiniControllerExtension(this, host);
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
      return "Controllers/Novation/Launchkey Mini.html";
   }

   public static LaunchkeyMiniControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final LaunchkeyMiniControllerExtensionDefinition
      INSTANCE = new LaunchkeyMiniControllerExtensionDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("dd7922b8-ba1e-4f41-929b-d3f74c469a17");
}
