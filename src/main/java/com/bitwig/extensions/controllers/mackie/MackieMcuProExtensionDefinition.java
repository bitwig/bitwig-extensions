package com.bitwig.extensions.controllers.mackie;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MackieMcuProExtensionDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-81ad-1de77ffa2dab");

	private static final int MCU_API_VERSION = 15;
	private static final String SOFTWARE_VERSION = "0.9";
	private static final String DEVICE_NAME = "Mackie Control";

	protected int nrOfExtenders;
	protected String[] inMidiPortNames;
	protected String[] outMidiPortNames;

	public MackieMcuProExtensionDefinition() {
		this(0);
	}

	public MackieMcuProExtensionDefinition(final int nrOfExtenders) {
		this.nrOfExtenders = nrOfExtenders;
		inMidiPortNames = new String[nrOfExtenders + 1];
		outMidiPortNames = new String[nrOfExtenders + 1];
		inMidiPortNames[0] = "MCU Pro USB v3.1";
		outMidiPortNames[0] = "MCU Pro USB v3.1";
		for (int i = 1; i < nrOfExtenders + 1; i++) {
			inMidiPortNames[i] = String.format("MIDIIN%d (MCU Pro USB v3.1)", i);
			outMidiPortNames[i] = String.format("MIDIOUT%d (MCU Pro USB v3.1)", i);
		}
	}

	@Override
	public String getName() {
		if (nrOfExtenders == 0) {
			return DEVICE_NAME;
		}
		return String.format("%s +%d EXTENDER", DEVICE_NAME, nrOfExtenders);
	}

	@Override
	public String getAuthor() {
		return "Bitwig";
	}

	@Override
	public String getVersion() {
		return SOFTWARE_VERSION;
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
		return "Mackie Control";
	}

	@Override
	public int getRequiredAPIVersion() {
		return MCU_API_VERSION;
	}

	@Override
	public int getNumMidiInPorts() {
		return nrOfExtenders + 1;
	}

	@Override
	public int getNumMidiOutPorts() {
		return nrOfExtenders + 1;
	}

	@Override
	public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
			final PlatformType platformType) {
		if (platformType == PlatformType.WINDOWS) {
			list.add(inMidiPortNames, outMidiPortNames);
		} else if (platformType == PlatformType.MAC) {
			list.add(inMidiPortNames, outMidiPortNames);
		} else if (platformType == PlatformType.LINUX) {
			list.add(inMidiPortNames, outMidiPortNames);
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
		return new MackieMcuProExtension(this, host, nrOfExtenders);
	}
}
