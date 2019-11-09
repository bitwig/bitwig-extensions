package com.bitwig.extensions.controllers.novation.looprecorder;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LoopRecorderExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("2497b423-d7a1-4d51-953e-294929f6aefd");

   public LoopRecorderExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Loop Recorder";
   }

   @Override
   public String getAuthor()
   {
      return "Ajja & Alex";
   }

   @Override
   public String getVersion()
   {
      return "0.3";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 4;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launchpad S/Mini/Pro";
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
   }

   @Override
   public LoopRecorderExtension createInstance(final ControllerHost host)
   {
      return new LoopRecorderExtension(this, host);
   }

   public static LoopRecorderExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   private static LoopRecorderExtensionDefinition INSTANCE = new LoopRecorderExtensionDefinition();
}
