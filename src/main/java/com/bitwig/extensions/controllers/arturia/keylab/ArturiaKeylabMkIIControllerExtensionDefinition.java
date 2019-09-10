package com.bitwig.extensions.controllers.arturia.keylab;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class ArturiaKeylabMkIIControllerExtensionDefinition extends ControllerExtensionDefinition
{
   public abstract int getNumberOfKeys();

   @Override
   public String getVersion()
   {
      return "3.0";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getName()
   {
      return "Keylab " + getNumberOfKeys() + " mkII";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 8;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Arturia";
   }

   @Override
   public String getHardwareModel()
   {
      return "KeyLab " + getNumberOfKeys() + " mkII";
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/Arturia/KeyLab " + getNumberOfKeys() + " mkII.html";
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
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new ArturiaKeylabMkIIControllerExtension(this, host);
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         final String name = "KeyLab mkII " + getNumberOfKeys();

         final String in2 = "MIDIIN2 (KeyLab mkII " + getNumberOfKeys() + ")";
         final String out2 = "MIDIOUT2 (KeyLab mkII " + getNumberOfKeys() + ")";

         list.add(new String[] {name, in2}, new String[] {name, out2});

      }
      else if (platformType == PlatformType.MAC)
      {
         final String name = "KeyLab mkII " + getNumberOfKeys();

         list.add(
            new String[] {name + " MIDI In", name + " DAW In"},
            new String[] {name + " MIDI Out", name + " DAW Out"});
      }
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }
}
