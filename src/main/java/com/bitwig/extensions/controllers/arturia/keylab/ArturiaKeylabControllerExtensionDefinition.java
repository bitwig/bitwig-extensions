package com.bitwig.extensions.controllers.arturia.keylab;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class ArturiaKeylabControllerExtensionDefinition extends ControllerExtensionDefinition
{
   public abstract int getNumberOfKeys();

   public abstract boolean hasDrumPads();

   @Override
   public String getVersion()
   {
      return "2.3";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getName()
   {
      return "Keylab " + getNumberOfKeys();
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 7;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Arturia";
   }

   @Override
   public String getHardwareModel()
   {
      return "KeyLab " + getNumberOfKeys();
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/Arturia/KeyLab" + Math.min(61, getNumberOfKeys()) + ".html";
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
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ArturiaKeylabControllerExtension(this, host);
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      final String name1 = "KeyLab " + getNumberOfKeys();
      final String name2 = "KeyLab " + getNumberOfKeys() + " MIDI 1";

      list.add(new String[] {name1}, new String[] {name1});
      list.add(new String[] {name2}, new String[] {name2});
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }
}
