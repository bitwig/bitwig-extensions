package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class KompleteKontrolSMk2ExtensionDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("5348f355-862e-4674-bbab-dd15f5342d99");

	public KompleteKontrolSMk2ExtensionDefinition() {
	}

	@Override
	public String getName() {
		return "Komplete Kontrol S Mk2";
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
		return "Komplete Kontrol S Mk2";
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

	@Override
	public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
			final PlatformType platformType) {
		if (platformType == PlatformType.WINDOWS) {
			list.add(new String[] { "Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1" },
					new String[] { "Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "Komplete Kontrol DAW - 1", "KOMPLETE KONTROL S49 MK2" },
					new String[] { "Komplete Kontrol DAW - 1", "KOMPLETE KONTROL S49 MK2" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1" },
					new String[] { "Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1" });
		}
	}

	@Override
	public String getHelpFilePath() {
		return "Controllers/Native Instruments/Komplete Kontrol MK2/Komplete Kontrol MK2.pdf";
	}

	@Override
	public KompleteKontrolSMk2Extension createInstance(final ControllerHost host) {
		return new KompleteKontrolSMk2Extension(this, host);
	}
}
