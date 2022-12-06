package com.bitwig.extensions.controllers.devine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class EzCreatorPadDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Devine";
   }

   @Override
   public String getHardwareModel()
   {
      return "EZ-Creator Pad";
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
            list.add(new String[] {"EZ-Creator Pad MIDI 1"}, new String[] {"EZ-Creator Pad MIDI 1"});
            break;

         case WINDOWS:
            list.add(new String[] {"EZ-Creator Pad"}, new String[] {"EZ-Creator Pad"});
            break;

         case MAC:
            list.add(new String[] {"EZ-Creator Pad"}, new String[] {"EZ-Creator Pad"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new EzCreatorPad(this, host);
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
      return EzCreatorCommon.REQUIRED_API_VERSION;
   }

   public static EzCreatorPadDefinition getInstance()
   {
      return INSTANCE;
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Devine/EZ-Creator Pads.html";
   }

   private static final EzCreatorPadDefinition INSTANCE = new EzCreatorPadDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("d3722127-0d31-4b4b-9d46-0f585c3ccdef");
}
