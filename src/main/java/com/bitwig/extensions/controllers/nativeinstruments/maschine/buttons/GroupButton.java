package com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.Midi;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;

public class GroupButton implements RgbButton {

	public static final int CC_OFFSET = 100;

	private final HardwareButton hwButton;
	private final int index;
	private final MultiStateHardwareLight light;

	public GroupButton(final int index, final MaschineExtension driver) {
		final HardwareSurface surface = driver.getSurface();
		final MidiIn midiIn = driver.getMidiIn();
		this.index = index;
		final String id = "GROUP_BUTTON_" + (index + 1);
		hwButton = surface.createHardwareButton(id);
		hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, CC_OFFSET + index, 127));
		hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, CC_OFFSET + index, 0));

		light = surface.createMultiStateHardwareLight(id + "-light");
		light.state().setValue(RgbLedState.OFF);
		light.state().onUpdateHardware(hwState -> driver.updatePadLed(this));
		hwButton.setBackgroundLight(light);
	}

	public int getGroupCC() {
		return index + CC_OFFSET;
	}

	@Override
	public MultiStateHardwareLight getLight() {
		return light;
	}

	@Override
	public HardwareButton getHwButton() {
		return hwButton;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public int getMidiDataNr() {
		return index + CC_OFFSET;
	}

	@Override
	public int getMidiStatus() {
		return Midi.CC;
	}
}
