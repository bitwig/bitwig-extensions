package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MaschinePlusExtensionDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("1e15d9ea-7dbb-4653-a76c-e96fbbff5ceb");

	public MaschinePlusExtensionDefinition() {
	}

	@Override
	public String getName() {
		return "Maschine Plus";
	}

	@Override
	public String getAuthor() {
		return "Bitwig";
	}

	@Override
	public String getVersion() {
		return "1.0";
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
		return "MaschinePlus";
	}

	@Override
	public int getRequiredAPIVersion() {
		return 14;
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
			list.add(new String[] { "Maschine Plus Virtual" }, new String[] { "Maschine Plus Virtual" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "Maschine Plus Virtual" }, new String[] { "Maschine Plus Virtual" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "Maschine Plus Virtual" }, new String[] { "Maschine Plus Virtual" });
		}
	}

	@Override
	public String getHelpFilePath() {
		return "Controllers/Native Instruments/Maschine MK3 Plus/Maschine MK3 Plus.pdf";
	}

	@Override
	public String getSupportFolderPath() {
		return "Controllers/Native Instruments/Maschine MK3 Plus";
	}

	@Override
	public MaschineExtension createInstance(final ControllerHost host) {
		return new MaschineExtension(this, host, MaschineMode.PLUS);
	}
}
