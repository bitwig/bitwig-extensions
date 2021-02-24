package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.KeyboardMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.VeloctiyHandler;

public class ScaleLayer extends DisplayLayer {

	private KeyboardMode keyboardLayer;
	private final VeloctiyHandler velocityHandler;

	public ScaleLayer(final MaschineExtension driver, final String name, final VeloctiyHandler velocityHandler) {
		super(driver, name);
		this.velocityHandler = velocityHandler;
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
		final ModeButton[] buttons = driver.getDisplayButtons();

		final RelativeHardwareKnob scaleSelection = knobs[0];
		final RelativeHardwareKnob octaveSelection = knobs[1];
		final RelativeHardwareKnob semiSelection = knobs[2];
		final RelativeHardwareKnob fixedSelection = knobs[4];
		final ControllerHost host = driver.getHost();
		bind(scaleSelection,
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> updateScale(1), () -> "+"),
						host.createAction(() -> updateScale(-1), () -> "-")));
		bind(octaveSelection,
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> updateOctave(1), () -> "+"),
						host.createAction(() -> updateOctave(-1), () -> "-")));
		bind(semiSelection, host.createRelativeHardwareControlStepTarget(
				host.createAction(() -> updateSemi(1), () -> "+"), host.createAction(() -> updateSemi(-1), () -> "-")));
		bind(fixedSelection,
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> updateFixedVelocity(1), () -> "+"),
						host.createAction(() -> updateFixedVelocity(-1), () -> "-")));
		bindPressed(buttons[0], () -> updateScale(-1));
		bindPressed(buttons[1], () -> updateScale(1));
		bindPressed(buttons[2], () -> updateOctave(-1));
		bindPressed(buttons[3], () -> updateOctave(1));
		bindPressed(buttons[4], () -> updateSemi(-1));
		bindPressed(buttons[5], () -> updateSemi(1));
	}

	public void displayInfo() {
		if (!isActive()) {
			return;
		}
		String infoLeft = "";
		String infoRight = "";
		if (isInfoModeActive()) {
			infoLeft = "<SCALE|SCALE>| <OCT | OCT>";
			infoRight = "<ROOT | ROOT>| ---- | ////";
		} else {
			infoLeft = keyboardLayer.getCurrentScale().getName() + " "
					+ DisplayUtil.toNote(keyboardLayer.getBaseNote());
			// infoLeft = DisplayUtil.padValue(velocityHandler.getFixedVelocity(), 3);
			infoRight = "Fixed = " + DisplayUtil.padValue(velocityHandler.getFixedVelocity(), 3);
		}
		sendToDisplay(TOP_LEFT, infoLeft);
		sendToDisplay(TOP_RIGHT, infoRight);
	}

	public void displayEncoder() {
		final String encoderLeft = "SCALE |OCTAVE|ROOT  | ----";
		final String encoderRight = "FXD VL| ---- | ---- | ----";
		sendToDisplay(BOTTOM_LEFT, encoderLeft);
		sendToDisplay(BOTTOM_RIGHT, encoderRight);
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
	protected void doActivate() {
		super.doActivate();
		final RelativeHardwareKnob[] knobs = getDriver().getDisplayKnobs();
		for (int i = 0; i < knobs.length; i++) {
			knobs[i].setStepSize(1 / 64.0);
		}
		displayInfo();
		displayEncoder();
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
	}

	public void setKeyboardLayer(final KeyboardMode keyboardMode) {
		this.keyboardLayer = keyboardMode;
	}

	public void updateScale(final int incval) {
		keyboardLayer.incScale(incval);
		displayInfo();
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
		displayInfo();
	}

	@Override
	public void notifyEncoderTouched(final int index, final boolean v) {
	}

	public void updateOctave(final int incval) {
		keyboardLayer.incOctave(incval);
		displayInfo();
	}

	public void updateSemi(final int incval) {
		keyboardLayer.incSemi(incval);
		displayInfo();
	}

	public void updateFixedVelocity(final int incval) {
		velocityHandler.inc(incval);
		displayInfo();
	}

}
