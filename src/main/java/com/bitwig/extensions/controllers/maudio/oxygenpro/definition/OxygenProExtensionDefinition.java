package com.bitwig.extensions.controllers.maudio.oxygenpro.definition;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxyConfig;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxygenProExtension;

import java.util.UUID;

public abstract class OxygenProExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("b75a10e5-3f23-4b17-9f32-23c0ff99e0bc");

   private static final String STD_MIDI_IN_FORMAT = "MIDIIN3 (Oxygen Pro %s)";
   private static final String STD_MIDI_OUT_FORMAT = "MIDIOUT3 (Oxygen Pro %s)";
   private static final String KEY_FORMAT = "Oxygen Pro %s";

   private static final String VERSION = "0.02";

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
      return "Oxygen Pro ";
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
         addPorts(list, STD_MIDI_IN_FORMAT, STD_MIDI_OUT_FORMAT, getKeys());
      } else if (platformType == PlatformType.MAC) {
         addPorts(list, STD_MIDI_IN_FORMAT, STD_MIDI_OUT_FORMAT, getKeys());
      } else if (platformType == PlatformType.LINUX) {
         addPorts(list, STD_MIDI_IN_FORMAT, STD_MIDI_OUT_FORMAT, getKeys());
      }
   }

   private void addPorts(final AutoDetectionMidiPortNamesList list, final String inFormat, final String outFormat,
                         final String keyVersion) {
      list.add(new String[]{String.format(inFormat, keyVersion), String.format(KEY_FORMAT, keyVersion)},
         new String[]{String.format(outFormat, keyVersion), String.format(KEY_FORMAT, keyVersion)});
   }

   @Override
   public OxygenProExtension createInstance(final ControllerHost host) {
      return new OxygenProExtension(this, host, new OxyConfig(8, true, true));
   }
}
