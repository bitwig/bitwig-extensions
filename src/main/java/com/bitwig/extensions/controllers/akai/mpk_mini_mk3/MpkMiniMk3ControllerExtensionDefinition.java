package com.bitwig.extensions.controllers.akai.mpk_mini_mk3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MpkMiniMk3ControllerExtensionDefinition extends ControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("e8c217e1-52c1-4c64-b69e-a26cdfc03320");

   @Override
   public String getHardwareVendor()
   {
      return "Akai";
   }

   @Override
   public String getHardwareModel()
   {
      return "MPK mini MK3";
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
   public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType)
   {
      final String inputNames[] = new String[1];
      final String outputNames[] = new String[1];

      switch (platformType)
      {
         case LINUX:
            inputNames[0] = "MPK mini 3 MIDI 1";
            outputNames[0] = "MPK mini 3 MIDI 1";
            break;

         case WINDOWS:
         case MAC:
            inputNames[0] = "MPK mini 3";
            outputNames[0] = "MPK mini 3";
            break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public ControllerExtension createInstance(ControllerHost host)
   {
      return new MpkMiniMk3ControllerExtension(this, host);
   }

   @Override
   public String getName()
   {
      return "MPK mini MK3";
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
   public java.util.UUID getId()
   {
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 11;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Akai/MPK mini MK3.html";
   }
}
