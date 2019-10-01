package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

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
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      String[] midiNameList = {"PreSonus FP" + channelCount()};

      list.add(midiNameList, midiNameList);
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new PresonusFaderPort(this, host);
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
