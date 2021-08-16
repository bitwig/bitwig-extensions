package com.bitwig.extensions.controllers.mackie;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MackieMcuProExtensionDefinition extends ControllerExtensionDefinition {
	public static final int MCU_API_VERSION = 15;
	private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-81ad-1de77ffa2dab");

	public MackieMcuProExtensionDefinition() {
	}

	@Override
	public String getName() {
		return "Mackie MCU Pro V3";
	}

	@Override
	public String getAuthor() {
		return "Bitwig";
	}

	@Override
	public String getVersion() {
		return "0.2";
	}

	@Override
	public UUID getId() {
		return DRIVER_ID;
	}

	@Override
	public String getHardwareVendor() {
		return "Mackie";
	}

	@Override
	public String getHardwareModel() {
		return "MCU Pro UCS";
	}

	@Override
	public int getRequiredAPIVersion() {
		return MCU_API_VERSION;
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
			list.add(new String[] { "MCU Pro USB v3.1" }, new String[] { "MCU Pro USB v3.1" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "MCU Pro USB v3.1" }, new String[] { "MCU Pro USB v3.1" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "MCU Pro USB v3.1" }, new String[] { "MCU Pro USB v3.1" });
		}
	}

	@Override
	public String getHelpFilePath() {
		return "";
	}

	@Override
	public String getSupportFolderPath() {
		return "";
	}

	@Override
	public MackieMcuProExtension createInstance(final ControllerHost host) {
		return new MackieMcuProExtension(this, host, 0);
	}
}
