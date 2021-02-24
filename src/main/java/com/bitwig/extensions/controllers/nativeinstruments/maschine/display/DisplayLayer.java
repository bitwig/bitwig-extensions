package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.BooleanValueObject;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;

public abstract class DisplayLayer extends MaschineLayer {

	public static final int TOP_LEFT = 0;
	public static final int TOP_RIGHT = 1;
	public static final int BOTTOM_LEFT = 2;
	public static final int BOTTOM_RIGHT = 3;

	private final BooleanValueObject active = new BooleanValueObject();
	private boolean infoModeActive = false;
	private int focusTouchIndex = -1;

	protected boolean isMacroDown = false;

	public DisplayLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
	}

	@Override
	final protected void onActivate() {
		doActivate();
		active.setValue(true);
	}

	protected void setKnobSensitivity(final double sensitivity) {
		if (!isActive()) {
			return;
		}
		final RelativeHardwareKnob[] knobs = getDriver().getDisplayKnobs();
		for (int i = 0; i < knobs.length; i++) {
			knobs[i].setStepSize(1 / 128.0);
			knobs[i].setSensitivity(sensitivity);
		}
	}

	protected void doActivate() {
		/* for subclasses */
	}

	protected void doDeactivate() {
		/* for subclasses */
	}

	@Override
	final protected void onDeactivate() {
		doDeactivate();
		infoModeActive = false;
		active.setValue(false);
	}

	public boolean isPadRelatedMode() {
		return false;
	}

	public boolean isInfoModeActive() {
		return infoModeActive;
	}

	public BooleanValue getActive() {
		return active;
	}

	public int getFocusTouchIndex() {
		return focusTouchIndex;
	}

	public void notifyTouched(final int index, final boolean touched) {
		if (touched) {
			focusTouchIndex = index;
		}
		if (isActive()) {
			notifyEncoderTouched(index, touched);
		}
	}

	protected void clearDisplay() {
		for (int i = TOP_LEFT; i <= BOTTOM_RIGHT; i++) {
			sendToDisplay(i, "");
		}
	}

	protected abstract void notifyEncoderTouched(final int index, final boolean v);

	/**
	 * Notify display that main knob has been touched.
	 *
	 * @param touched if main4d knob was touched
	 */
	public final void notifyMainTouched(final boolean touched) {
		if (isActive()) {
			infoModeActive = touched;
			doNotifyMainTouched(touched);
		}
	}

	protected abstract void doNotifyMainTouched(boolean touched);

	public final void notifyMacroDown(final boolean active) {
		isMacroDown = active;
		if (isActive()) {
			doNotifyMacroDown(active);
		}
	}

	protected void sendToDisplay(final int index, final String displayValue) {
		getDriver().sendToDisplayBuffered(index, displayValue);
	}

	protected abstract void doNotifyMacroDown(final boolean active);

}
