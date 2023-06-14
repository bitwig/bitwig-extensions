package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class KeyLabEssential3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("0b9962d9-5c32-4d5a-942c-594dbe0c64ca");

   private static final String MIDI_NAME_FORMAT_WINDOWS = "KL Essential %d mk3 MIDI";
   private static final String MIDI_NAME_FORMAT_MAC = "KeyLab Essential %d mk3 MIDI";

   private static final int[] KEY_VARS = {49, 61, 88};

   public KeyLabEssential3ExtensionDefinition() {
   }

   @Override
   public String getName() {
      return "KeyLab Essential Mk3";
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return "1.01";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor() {
      return "Arturia";
   }

   @Override
   public String getHardwareModel() {
      return "KeyLab Essential Mk3";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 17;
   }

   @Override
   public int getNumMidiInPorts() {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 1;
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Arturia/Arturia KeyLab Essential Mk3.pdf";
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         addPorts(list, MIDI_NAME_FORMAT_WINDOWS, KEY_VARS);
         addPorts(list, MIDI_NAME_FORMAT_WINDOWS, KEY_VARS);
      } else if (platformType == PlatformType.MAC) {
         addPorts(list, MIDI_NAME_FORMAT_MAC, KEY_VARS);
         addPorts(list, MIDI_NAME_FORMAT_WINDOWS, KEY_VARS);
      } else if (platformType == PlatformType.LINUX) {
         addPorts(list, MIDI_NAME_FORMAT_MAC, KEY_VARS);
      }
   }

   private void addPorts(final AutoDetectionMidiPortNamesList list, final String format, final int[] keynumbers) {
      for (final int keyNumber : keynumbers) {
         addPorts(list, format, keyNumber);
      }
   }

   private void addPorts(final AutoDetectionMidiPortNamesList list, final String format, final int keynumbers) {
      final String portName = String.format(format, keynumbers);
      list.add(new String[]{portName}, new String[]{portName});
   }

   @Override
   public KeylabEssential3Extension createInstance(final ControllerHost host) {
      return new KeylabEssential3Extension(this, host);
   }
}
