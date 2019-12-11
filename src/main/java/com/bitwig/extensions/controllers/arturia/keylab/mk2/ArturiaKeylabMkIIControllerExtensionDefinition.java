package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class ArturiaKeylabMkIIControllerExtensionDefinition extends ControllerExtensionDefinition
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
      return "KeyLab " + getNumberOfKeys() + " mkII";
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
      return "KeyLab " + getNumberOfKeys() + " mkII";
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/Arturia/KeyLab mkII.pdf";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 2;
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
      if (platformType == PlatformType.WINDOWS)
      {
         final String name = "KeyLab mkII " + getNumberOfKeys();

         final String in2 = "MIDIIN2 (KeyLab mkII " + getNumberOfKeys() + ")";
         final String out2 = "MIDIOUT2 (KeyLab mkII " + getNumberOfKeys() + ")";

         list.add(new String[] {name, in2}, new String[] {out2});

      }
      else if (platformType == PlatformType.MAC)
      {
         final String name = "KeyLab mkII " + getNumberOfKeys();

         list.add(
            new String[] {name + " MIDI", name + " DAW"},
            new String[] {name + " DAW"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         final String name = "KeyLab mkII " + getNumberOfKeys();

         final String[] inNames = new String[] {name + " MIDI 1", name + " MIDI 2"};
         final String[] outNames = new String[] {name + " MIDI 2"};

         list.add(inNames, outNames);
      }
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }
}
