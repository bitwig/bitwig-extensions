package com.bitwig.extensions.controllers.generic;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MIDIKeyboardWith8KnobsExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("BADEC0B0-806E-46CB-AB8C-06209F78F6F9");

   public MIDIKeyboardWith8KnobsExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "MIDI Keyboard";
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
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Generic";
   }

   @Override
   public String getHardwareModel()
   {
      return "Keyboard + 8 Device Knobs (CC 20-27)";
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
      return 0;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
   }

   @Override
   public MIDIKeyboardWith8KnobsExtension createInstance(final ControllerHost host)
   {
      return new MIDIKeyboardWith8KnobsExtension(this, host);
   }
}
