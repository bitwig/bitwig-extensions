package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxyConfig;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxygenProExtension;

public abstract class OxygenProExtensionDefinition extends ControllerExtensionDefinition {

   private static final String STD_MIDI_IN_FORMAT = "MIDIIN3 (Oxygen Pro %s)";
   private static final String STD_MIDI_OUT_FORMAT = "MIDIOUT3 (Oxygen Pro %s)";
   private static final String STD_MAC_FORMAT_FORMAT = "Oxygen Pro %s Mackie/HUI";
   private static final String KEY_FORMAT_WIN = "Oxygen Pro %s";
   private static final String KEY_FORMAT_MAC = "Oxygen Pro %s USB MIDI";

   private static final String VERSION = "1.01";

   public OxygenProExtensionDefinition() {
   }

   protected abstract String getKeys();

   @Override
   public String getName() {
      return String.format("M-Audio Oxygen Pro %s", getKeys());
   }

   @Override
   public String getAuthor() {
      return "Bitwig";
   }

   @Override
   public String getVersion() {
      return VERSION;
   }


   @Override
   public String getHardwareVendor() {
      return "M-Audio";
   }

   @Override
   public String getHardwareModel() {
      return String.format("Oxygen Pro %s", getKeys());
   }

   @Override
   public int getRequiredAPIVersion() {
      return 18;
   }

   @Override
   public int getNumMidiInPorts() {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 2;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                              final PlatformType platformType) {
      if (platformType == PlatformType.WINDOWS) {
         addPorts(list, STD_MIDI_IN_FORMAT, STD_MIDI_OUT_FORMAT, KEY_FORMAT_WIN, getKeys());
      } else if (platformType == PlatformType.MAC) {
         addPorts(list, STD_MAC_FORMAT_FORMAT, STD_MAC_FORMAT_FORMAT, KEY_FORMAT_MAC, getKeys());
      } else if (platformType == PlatformType.LINUX) {
         addPorts(list, STD_MIDI_IN_FORMAT, STD_MIDI_OUT_FORMAT, KEY_FORMAT_MAC, getKeys());
      }
   }

   private void addPorts(final AutoDetectionMidiPortNamesList list, final String inFormat, final String outFormat,
                         String keyFormat, final String keyVersion) {
      list.add(new String[]{String.format(keyVersion, inFormat), String.format(keyVersion, keyFormat)},
         new String[]{String.format(keyVersion, outFormat), String.format(keyVersion, keyFormat)});
   }

   @Override
   public OxygenProExtension createInstance(final ControllerHost host) {
      return new OxygenProExtension(this, host, new OxyConfig(8, true, true, true));
   }
}
