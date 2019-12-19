package com.bitwig.extensions.controllers.midiplus;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class X2MiniControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Midiplus";
   }

   @Override
   public String getHardwareModel()
   {
      return "X2mini";
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
            list.add(new String[]{"X2mini MIDI 1"}, new String[]{"X2mini MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[]{"X2mini"}, new String[]{"X2mini"});
            break;

         case MAC:
            list.add(new String[]{"X2mini"}, new String[]{"X2mini"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new XControllerExtension(this, host, 0, 4, INIT_SYSEX, DEINIT_SYSEX);
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
      return XControllerExtension.REQUIRED_API_VERSION;
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }

   public static X2MiniControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/MIDIPLUS/Xmini Keyboards.html";
   }

   final private static X2MiniControllerExtensionDefinition INSTANCE = new X2MiniControllerExtensionDefinition();
   final private static UUID EXTENSION_UUID = UUID.fromString("6333510b-63a7-42e8-b9a0-281c9915c56a");
   final private static byte[] INIT_SYSEX = new byte[] {
      (byte) 0xF0, // Sysex Header
      0x00, 0x00, 0x74, // MIDI Plus bytes
      0x00, 0x00, // X2 product
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
      0x00, 0x00, // X2 product
      0x00, 0x01, // MIDI config number
      0x00, 0x01, // Protocol version
      0x01, // Mode switch command
      0x00, 0x01, // Data length Msb/Lsb
      0x00, // Play Mode
      (byte) 0xF7, // Sysex End
   };
}
