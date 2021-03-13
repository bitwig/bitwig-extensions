package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;

public class DrumPadMode extends BasicKeyPlayingMode implements JogWheelDestination {
	private final int padOffset = 36;
	private final DrumPadBank drumPadBank;

	private boolean hasDrumPads = false;
	private PadMode altMode;
	private final boolean[] isSelected = new boolean[16];

	public DrumPadMode(final MaschineExtension driver, final String name, final NoteFocusHandler noteFocusHandler,
			final VeloctiyHandler velocityHandler, final DisplayLayer associatedDisplay) {
		super(driver, name, noteFocusHandler, velocityHandler, associatedDisplay);

		drumPadBank = driver.getPrimaryDevice().createDrumPadBank(16);

		final PadButton[] buttons = driver.getPadButtons();
		for (int i = 0; i < buttons.length; i++) {
			final int index = i;
			final PadButton button = buttons[i];
			final DrumPad pad = drumPadBank.getItemAt(index);
			pad.color().markInterested();
			pad.name().markInterested();
			pad.exists().markInterested();
			pad.addIsSelectedInEditorObserver(selected -> {
				if (selected) {
					noteFocusHandler.notifyDrumPadSelected(pad, padOffset, index);
					isSelected[index] = true;
				} else {
					isSelected[index] = false;
				}
			});
			bindLightState(() -> computeGridLedState(index, pad), button);
			bindShift(button);
			selectLayer.bindPressed(button, () -> selectPad(index));
		}
	}

	@Override
	public void jogWheelAction(final int increment) {
		drumPadBank.scrollBy(increment * 4);
	}

	@Override
	public void jogWheelPush(final boolean push) {
	}

	public void setAltMode(final PadMode altMode) {
		this.altMode = altMode;
		getDriver().getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
			this.hasDrumPads = hasDrumPads;
			if (isActive() && !hasDrumPads) {
				forceAltmode();
			}
		});
	}

	@Override
	protected void onActivate() {
		if (!hasDrumPads) {
			forceAltmode();
		} else {
			super.onActivate();
		}
	}

	public void forceAltmode() {
		final MaschineExtension driver = getDriver();
		driver.setMode(altMode);
		if (altMode.getAssociatedDisplay() != null //
				&& driver.getCurrentDisplayMode().isPadRelatedMode()) {
			driver.setDisplayMode(altMode.getAssociatedDisplay());
		}
	}

	void selectPad(final int index) {
		final DrumPad pad = drumPadBank.getItemAt(index);
		pad.selectInEditor();
		noteFocusHandler.notifyDrumPadSelected(pad, padOffset, index);
	}

	private InternalHardwareLightState computeGridLedState(final int index, final DrumPad pad) {
		if (hasDrumPads) {
			int color = NIColorUtil.convertColor(pad.color());
			if (playing[index] || isSelected[index]) {
				color += 2;
			}
			return new RgbLedState(color); // TO this need to be done with lookup table
		} else {
			int color = NIColorUtil.convertColor(getDriver().getCursorTrack().color());
			if (playing[index]) {
				color += 2;
			}
			return new RgbLedState(color); // TO this need to be done with lookup table
		}
	}

	@Override
	void applyScale() {
		for (int i = 0; i < 128; i++) {
			noteToPad[i] = -1;
		}
		for (int i = 0; i < 16; i++) {
			noteTable[i + PadButton.PAD_NOTE_OFFSET] = padOffset + i;
			noteToPad[padOffset + i] = i;
		}
		getDriver().getNoteInput().setKeyTranslationTable(noteTable);
	}

	@Override
	protected String getModeDescription() {
		return "Drum Pad Mode";
	}

}
