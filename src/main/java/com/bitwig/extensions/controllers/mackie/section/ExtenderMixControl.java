package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.VPotMode;

public class ExtenderMixControl extends MixControl {

	public ExtenderMixControl(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
			final int sectionIndex) {
		super(driver, midiIn, midiOut, sectionIndex, SectionType.XTENDER);
	}

	@Override
	void doModeChange(final VPotMode mode, final boolean focus) {
		switch (mode) {
		case PAN:
			switchActiveConfiguration(panConfiguration);
			break;
		case SEND:
			determineSendTrackConfig(VPotMode.SEND);
			break;
		default:
			return;
		}
		activeVPotMode = mode;
		getDriver().getBrowserConfiguration().endUserBrowsing();
		layerState.updateState(this);
	}
}
