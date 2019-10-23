package com.bitwig.extensions.controllers.presonus.atom;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class PresonusAtomDefinition extends ControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("474bba86-c116-4d4a-a3c3-4c230ab4d012");

   @Override
   public String getHardwareVendor()
   {
      return "PreSonus";
   }

   @Override
   public String getHardwareModel()
   {
      return "ATOM";
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
      String[] midiNameList = {"ATOM"};

      list.add(midiNameList, midiNameList);
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new PresonusAtom(this, host);
   }

   @Override
   public String getName()
   {
      return "PreSonus ATOM";
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/PreSonus/ATOM.pdf";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getVersion()
   {
      return "3.1";
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 10;
   }
}
