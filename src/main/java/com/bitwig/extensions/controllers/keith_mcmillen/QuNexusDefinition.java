package com.bitwig.extensions.controllers.keith_mcmillen;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class QuNexusDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Keith McMillen";
   }

   @Override
   public String getHardwareModel()
   {
      return "QuNexus";
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
      list.add(new String[] {"QuNexus"}, new String[] {"QuNexus"});
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new QuNexus(this, host);
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

   public static QuNexusDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final QuNexusDefinition
      INSTANCE = new QuNexusDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("fce2090b-8b90-4c63-aa7e-18c6e57c9324");
}
