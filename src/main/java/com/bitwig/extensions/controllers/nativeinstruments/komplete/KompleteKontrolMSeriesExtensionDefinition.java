package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class KompleteKontrolMSeriesExtensionDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("963457d6-fa2f-4974-b61d-8c6881dc097e");

	public KompleteKontrolMSeriesExtensionDefinition() {
	}

	@Override
	public String getName() {
		return "Komplete Kontrol M32";
	}

	@Override
	public String getAuthor() {
		return "Bitwig";
	}

	@Override
	public String getVersion() {
		return "1.1";
	}

	@Override
	public UUID getId() {
		return DRIVER_ID;
	}

	@Override
	public String getHardwareVendor() {
		return "Native Instruments";
	}

	@Override
	public String getHardwareModel() {
		return "Komplete Kontrol M32";
	}

	@Override
	public int getRequiredAPIVersion() {
		return 12;
	}

	@Override
	public int getNumMidiInPorts() {
		return 2;
	}

	@Override
	public int getNumMidiOutPorts() {
		return 2;
	}

   @java.lang.Override
   public java.lang.String getHelpFilePath()
   {
      return "Controllers/Native Instruments/Komplete Kontrol A_M-Series/Komplete Kontrol A_M-Series.pdf";
   }

	@Override
	public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
			final PlatformType platformType) {
		if (platformType == PlatformType.WINDOWS) {
			list.add(new String[] { "Komplete Kontrol M DAW", "KOMPLETE KONTROL M32 MIDI" },
					new String[] { "Komplete Kontrol M DAW", "KOMPLETE KONTROL M32 MIDI" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "Komplete Kontrol M DAW", "KOMPLETE KONTROL M32" },
					new String[] { "Komplete Kontrol M DAW", "KOMPLETE KONTROL M32" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "Komplete Kontrol M DAW", "KOMPLETE KONTROL M32" },
					new String[] { "Komplete Kontrol M DAW", "KOMPLETE KONTROL M32" });
		}
	}

	@Override
	public KompleteKontrolAExtension createInstance(final ControllerHost host) {
		return new KompleteKontrolAExtension(this, host);
	}
}
