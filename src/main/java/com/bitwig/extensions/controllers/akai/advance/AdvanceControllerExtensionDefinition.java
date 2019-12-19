package com.bitwig.extensions.controllers.akai.advance;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class AdvanceControllerExtensionDefinition extends ControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("fbfd5fd9-1e34-4a38-9306-12e4f83fb8a8");

   @Override
   public String getHardwareVendor()
   {
      return "Akai";
   }

   @Override
   public String getHardwareModel()
   {
      return "ADVANCE 25/49/61";
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
            list.add(new String[]{"ADVANCE25 MIDI 1"}, new String[]{"ADVANCE25 MIDI 1"});
            list.add(new String[]{"ADVANCE49 MIDI 1"}, new String[]{"ADVANCE49 MIDI 1"});
            list.add(new String[]{"ADVANCE61 MIDI 1"}, new String[]{"ADVANCE61 MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[]{"ADVANCE25"}, new String[]{"ADVANCE25"});
            list.add(new String[]{"ADVANCE49"}, new String[]{"ADVANCE49"});
            list.add(new String[]{"ADVANCE61"}, new String[]{"ADVANCE61"});
            break;

         case MAC:
            list.add(new String[]{"ADVANCE25 Port 1"}, new String[]{"ADVANCE25 Port 1"});
            list.add(new String[]{"ADVANCE49 Port 1"}, new String[]{"ADVANCE49 Port 1"});
            list.add(new String[]{"ADVANCE61 Port 1"}, new String[]{"ADVANCE61 Port 1"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new AdvanceControllerExtension(this, host);
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
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 3;
   }

   public static AdvanceControllerExtensionDefinition getInstance()
   {
      return mInstance;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Akai/Advance Keyboards.html";
   }

   private static AdvanceControllerExtensionDefinition mInstance = new AdvanceControllerExtensionDefinition();
}
