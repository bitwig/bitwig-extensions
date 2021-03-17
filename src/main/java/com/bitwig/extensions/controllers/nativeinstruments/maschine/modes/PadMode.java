package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.BooleanValueObject;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ModifierState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;

public abstract class PadMode extends MaschineLayer {

	private final MaschineLayer shiftLayer;
	private final BooleanValueObject active = new BooleanValueObject();
	protected DisplayLayer associatedDisplay;
	protected final boolean prefersControlDisplay;

	public PadMode(final MaschineExtension driver, final String name) {
		this(driver, name, false);
	}

	public PadMode(final MaschineExtension driver, final String name, final boolean prefersControlDisplay) {
		super(driver, name);
		shiftLayer = new MaschineLayer(driver, "shift-" + name);
		this.prefersControlDisplay = prefersControlDisplay;
	}

	protected abstract String getModeDescription();

	protected void bindShift(final PadButton button) {
		shiftLayer.bindPressed(button, () -> getDriver().handleShiftAction(button.getIndex()));
	}

	public DisplayLayer getAssociatedDisplay() {
		return associatedDisplay;
	}

	public void setModifierState(final ModifierState modstate, final boolean active) {
		switch (modstate) {
		case SHIFT:
			if (active) {
				getShiftLayer().activate();
			} else {
				getShiftLayer().deactivate();
			}
			break;
		default:
			break;
		}
	}

	protected MaschineLayer getShiftLayer() {
		return shiftLayer;
	}

	@Override
	protected void onActivate() {
		doActivate();
		active.setValue(true);
	}

	protected void doActivate() {
		/* for subclasses */
	}

	@Override
	protected void onDeactivate() {
		doDeactivate();
		active.setValue(false);
	}

	protected void doDeactivate() {
		shiftLayer.deactivate();
	}

	public BooleanValue getActive() {
		return active;
	}

	public boolean hasMomentarySelectMode() {
		return false;
	}

	public PadMode getMomentarySwitchMode() {
		return null;
	}

	public boolean isPrefersControlDisplay() {
		return prefersControlDisplay;
	}
}
