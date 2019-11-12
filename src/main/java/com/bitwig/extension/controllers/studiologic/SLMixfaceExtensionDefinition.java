package com.bitwig.extension.controllers.studiologic;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class SLMixfaceExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("752b8890-09b8-44b1-bf0d-041d0a80489b");

   public SLMixfaceExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "SL Mixface";
   }

   @Override
   public String getAuthor()
   {
      return "Bitwig";
   }

   @Override
   public String getVersion()
   {
      return "1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Studiologic";
   }

   @Override
   public String getHardwareModel()
   {
      return "SL Mixface";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 10;
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
      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[] { "MIDIIN3 (SLMIXUSB)" }, new String[] { "MIDIOUT3 (SLMIXUSB)" });
      }
      else if (platformType == PlatformType.MAC)
      {
         list.add(new String[] { "SLMIXUSB Port 3" }, new String[] { "SLMIXUSB Port 3" });
         list.add(new String[] { "SLMIXUSB Anschluss 3" }, new String[] { "SLMIXUSB Anschluss 3" });
      }
      else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[] { "SLMIXUSB MIDI 3" }, new String[] { "SLMIXUSB MIDI 3" });
      }
   }

   @Override
   public SLMixfaceExtension createInstance(final ControllerHost host)
   {
      return new SLMixfaceExtension(this, host);
   }
}
