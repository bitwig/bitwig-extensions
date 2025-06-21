package com.bitwig.extensions.controllers.intuitive_instruments.exquis;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class ExquisControllerExtensionDefinition extends ControllerExtensionDefinition
{
   private final static UUID ID = UUID.fromString("b694f535-f022-416c-b714-f0b5924128f9");

   @Override
   public String getHardwareVendor()
   {
      return "Intuitive Instruments";
   }

   @Override
   public String getHardwareModel()
   {
      return "Exquis";
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
      final String inputNames[] = new String[1];
      final String outputNames[] = new String[1];

      switch (platformType)
      {
      case LINUX:
         inputNames[0] = "Exquis MIDI 1";
         outputNames[0] = "Exquis MIDI 1";
         break;

      case WINDOWS:
      case MAC:
         inputNames[0] = "Exquis";
         outputNames[0] = "Exquis";
         break;
      }

      list.add(inputNames, outputNames);
   }

   @Override
   public ControllerExtension createInstance(ControllerHost host)
   {
      return new ExquisControllerExtension(this, host);
   }

   @Override
   public String getName()
   {
      return "Exquis";
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
      return 18;
   }

}
