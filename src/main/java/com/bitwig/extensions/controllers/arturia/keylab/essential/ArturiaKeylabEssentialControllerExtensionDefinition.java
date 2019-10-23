package com.bitwig.extensions.controllers.arturia.keylab.essential;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class ArturiaKeylabEssentialControllerExtensionDefinition extends ControllerExtensionDefinition
{
   public abstract int getNumberOfKeys();

   @Override
   public String getVersion()
   {
      return "3.1";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getName()
   {
      return "KeyLab Essential " + getNumberOfKeys();
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 10;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Arturia";
   }

   @Override
   public String getHardwareModel()
   {
      return "KeyLab Essential " + getNumberOfKeys();
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/Arturia/KeyLab Essential.pdf";
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
      return new ArturiaKeylabEssentialControllerExtension(this, host);
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         final String name = "Arturia KeyLab Essential " + getNumberOfKeys();

         final String in2 = "MIDIIN2 (Arturia KeyLab Essenti";
         final String out2 = "MIDIOUT2 (Arturia KeyLab Essent";

         list.add(new String[] {name, in2}, new String[] {name, out2});

      }
      else if (platformType == PlatformType.MAC)
      {
         final String name = "Arturia KeyLab Essential " + getNumberOfKeys();

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
