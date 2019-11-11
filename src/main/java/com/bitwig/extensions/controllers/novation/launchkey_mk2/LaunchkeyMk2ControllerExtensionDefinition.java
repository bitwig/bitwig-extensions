package com.bitwig.extensions.controllers.novation.launchkey_mk2;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchkeyMk2ControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launchkey MK2";
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
               new String[]{"Launchkey MIDI LK Mini MIDI", "Launchkey MIDI LK Mini InControl"},
               new String[]{"Launchkey MIDI LK Mini MIDI", "Launchkey MIDI LK Mini InControl"});
            break;

         case WINDOWS:
            list.add(
               new String[]{"Launchkey MIDI", "MIDIIN2 (Launchkey MIDI)"},
               new String[]{"Launchkey MIDI", "MIDIOUT2 (Launchkey MIDI)"});
            break;

         case LINUX:
            list.add(
               new String[]{"Launchkey MIDI MIDI 1", "Launchkey MIDI MIDI 2"},
               new String[]{"Launchkey MIDI MIDI 1", "Launchkey MIDI MIDI 2"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LaunchkeyMk2ControllerExtension(this, host);
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
      return "Controllers/Novation/Launchkey MK2.html";
   }

   public static LaunchkeyMk2ControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final LaunchkeyMk2ControllerExtensionDefinition
      INSTANCE = new LaunchkeyMk2ControllerExtensionDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("cca78f4a-a37e-4e83-a8af-31bcc751881d");
}
