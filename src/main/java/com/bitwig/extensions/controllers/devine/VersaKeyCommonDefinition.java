package com.bitwig.extensions.controllers.devine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

abstract public class VersaKeyCommonDefinition extends ControllerExtensionDefinition
{
   public VersaKeyCommonDefinition(UUID driverID, String modelName)
   {
      mDriverID = driverID;
      mModelName= modelName;
   }

   @Override
   public String getName()
   {
      return mModelName;
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
      return mDriverID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Devine";
   }

   @Override
   public String getHardwareModel()
   {
      return mModelName;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return EzCreatorCommon.REQUIRED_API_VERSION;
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
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      switch (platformType)
      {
         case LINUX:
            list.add(new String[] {mModelName+" MIDI 1"}, new String[] {mModelName+" MIDI 1"});
            break;

         case WINDOWS:
         case MAC:
            list.add(new String[] {mModelName}, new String[] {mModelName});
            break;
      }
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Devine/"+mModelName+".html";
   }

   private UUID   mDriverID;
   private String mModelName;
}
