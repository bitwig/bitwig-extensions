package com.bitwig.extensions.controllers.midiplus;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class X8ProControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "MIDIPLUS";
   }

   @Override
   public String getHardwareModel()
   {
      return "X8pro";
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
      switch (platformType)
      {
         case LINUX:
            list.add(new String[]{"X8pro MIDI 1"}, new String[]{"X8pro MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[]{"X8pro"}, new String[]{"X8pro"});
            break;

         case MAC:
            list.add(new String[]{"X8pro"}, new String[]{"X8pro"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new XControllerExtension(this, host, 8, 9, INIT_SYSEX, DEINIT_SYSEX);
   }

   @Override
   public String getName()
   {
      return getHardwareModel();
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
   public UUID getId()
   {
      return EXTENSION_UUID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 7;
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }

   public static X8ProControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Documentation/Controllers/MIDIPLUS/XPro Keyboards.html";
   }

   final private static X8ProControllerExtensionDefinition INSTANCE = new X8ProControllerExtensionDefinition();
   final private static UUID EXTENSION_UUID = UUID.fromString("1eb03c5b-8ffd-4dd9-9b75-6cae51f96245");

   final private static byte[] INIT_SYSEX = new byte[] {
      (byte) 0xF0, // Sysex Header
      0x00, 0x00, 0x74, // MIDI Plus bytes
      0x02, 0x02, // X2 product
      0x00, 0x01, // MIDI config number
      0x00, 0x01, // Protocol version
      0x01, // Mode switch command
      0x00, 0x01, // Data length Msb/Lsb
      0x01, // Bitwig Mode
      (byte) 0xF7, // Sysex End
   };

   final private static byte[] DEINIT_SYSEX = new byte[] {
      (byte) 0xF0, // Sysex Header
      0x00, 0x00, 0x74, // MIDI Plus bytes
      0x02, 0x02, // X2 product
      0x00, 0x01, // MIDI config number
      0x00, 0x01, // Protocol version
      0x01, // Mode switch command
      0x00, 0x01, // Data length Msb/Lsb
      0x00, // Play Mode
      (byte) 0xF7, // Sysex End
   };
}
