package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class MiniLab3ExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("6d0d76fc-b9b0-11ec-8422-0242ac120002");

   private static final String PORT_NAME_MIDI = "Minilab3 MIDI";
   private static final String PORT_NAME = "Minilab3";
   private static final String PORT_NAME_LINUX = "Minilab3 Minilab3 MIDI";

   public MiniLab3ExtensionDefinition() {
   }

   @Override
   public String getHelpFilePath() {
      return "Controllers/Arturia/Arturia MiniLab 3.pdf";
   }

   @Override
   public String getName() {
      return "MiniLab 3";
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
      return "MiniLab 3";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 18;
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
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         list.add(new String[]{PORT_NAME_MIDI}, new String[]{PORT_NAME_MIDI});
         list.add(new String[]{PORT_NAME}, new String[]{PORT_NAME});
         appendRenamedPorts(4, list);
      } else if (platformType == PlatformType.MAC) {
         list.add(new String[]{PORT_NAME_MIDI}, new String[]{PORT_NAME_MIDI});
      } else if (platformType == PlatformType.LINUX) {
         list.add(new String[]{PORT_NAME_LINUX}, new String[]{PORT_NAME_LINUX});
      }
   }

   private void appendRenamedPorts(int count, AutoDetectionMidiPortNamesList list) {
      for (int i = 2; i < count + 2; i++) {
         list.add(getRenamedPorts(i), getRenamedPorts(i));
      }
   }

   private String[] getRenamedPorts(int index) {
      return new String[]{"%d- %s".formatted(index, PORT_NAME_MIDI)};
   }

   @Override
   public MiniLab3Extension createInstance(final ControllerHost host) {
      return new MiniLab3Extension(this, host);
   }
}
