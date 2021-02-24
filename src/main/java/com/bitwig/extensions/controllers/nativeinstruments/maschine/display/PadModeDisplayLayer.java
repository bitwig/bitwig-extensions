package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.VeloctiyHandler;

public class PadModeDisplayLayer extends DisplayLayer {

	private final VeloctiyHandler velocityHandler;

	public PadModeDisplayLayer(final MaschineExtension driver, final String name,
			final VeloctiyHandler velocityHandler) {
		super(driver, name);
		this.velocityHandler = velocityHandler;
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();

		final RelativeHardwareKnob fixedSelection = knobs[0];
		final ControllerHost host = driver.getHost();
		bind(fixedSelection,
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> updateFixedVelocity(1), () -> "+"),
						host.createAction(() -> updateFixedVelocity(-1), () -> "-")));
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		final RelativeHardwareKnob[] knobs = getDriver().getDisplayKnobs();
		for (int i = 0; i < knobs.length; i++) {
			knobs[i].setStepSize(1 / 128.0);
		}
		displayInfo();
		updateValues();
	}

	public void updateValues() {
		final String info = "Fixed Vel=" + DisplayUtil.padValue(velocityHandler.getFixedVelocity(), 3);
		sendToDisplay(2, info);
		sendToDisplay(3, "");
	}

	public void displayInfo() {
		if (!isActive()) {
			return;
		}
		String infoLeft = "";
		String infoRight = "";
		if (isInfoModeActive()) {
			infoLeft = " ---- | ---- | ---- | ---- ";
			infoRight = " ---- | ---- | ---- | ---- ";
		}
		sendToDisplay(0, infoLeft);
		sendToDisplay(1, infoRight);
	}

	@Override
	public boolean isPadRelatedMode() {
		return true;
	}

	@Override
	protected void doNotifyMacroDown(final boolean active) {
		// Do nothing
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
		displayInfo();
	}

	@Override
	public void notifyEncoderTouched(final int index, final boolean v) {
	}

	public void updateFixedVelocity(final int incval) {
		velocityHandler.inc(incval);
		updateValues();
	}

}
