package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class PresonusFaderPortDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "PreSonus";
   }

   @Override
   public String getHardwareModel()
   {
      return "Faderport " + channelCount();
   }

   abstract int channelCount();

   abstract String sysexDeviceID();

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
      if (platformType == PlatformType.WINDOWS)
      {
         final String[] midiNameList = { "PreSonus FP" + channelCount() };

         list.add(midiNameList, midiNameList);
      }
      else if (platformType == PlatformType.LINUX)
      {
         final String[] midiNameList = { "PreSonus FP" + channelCount() + " MIDI 1" };

         list.add(midiNameList, midiNameList);
      }
      else if (platformType == PlatformType.MAC)
      {
         final String[] midiNameList = { "PreSonus FP" + channelCount() + " Port 1" };

         list.add(midiNameList, midiNameList);
      }
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/PreSonus/FaderPort.pdf";
   }

   @Override
   public String getName()
   {
      return getHardwareVendor() + " " + getHardwareModel();
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
   public int getRequiredAPIVersion()
   {
      return 10;
   }
}
