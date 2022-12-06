package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public final class LaunchpadProControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getName()
   {
      return "Launchpad Pro";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launchpad Pro";
   }

   @Override
   public String getVersion()
   {
      return "0.2";
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
      final AutoDetectionMidiPortNamesList list,
      final PlatformType platformType)
   {
      list.add(new String[]{"Launchpad Pro MIDI 2"}, new String[]{"Launchpad Pro MIDI 2"});
      list.add(new String[]{"MIDIIN2 (Launchpad Pro)"}, new String[]{"MIDIOUT2 (Launchpad Pro)"});
      list.add(new String[]{"Launchpad Pro Standalone Port"}, new String[]{"Launchpad Pro Standalone Port"});
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LaunchpadProControllerExtension(this, host);
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Novation/LaunchPad Pro.html";
   }

   public static LaunchpadProControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

//   @Override
//   public boolean shouldBeSubscribedByDefault()
//   {
//      return false;
//   }

   private static final LaunchpadProControllerExtensionDefinition INSTANCE = new LaunchpadProControllerExtensionDefinition();

   private static final UUID EXTENSION_UUID = java.util.UUID
      .fromString("f6c0dc4c-8fbd-44f8-b577-15181e8b0649");
}
