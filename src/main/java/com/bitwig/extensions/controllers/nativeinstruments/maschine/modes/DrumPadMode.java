package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.Colors;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ModifierState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;

public class DrumPadMode extends BasicKeyPlayingMode implements JogWheelDestination {
	private int padOffset = 36;
	private final DrumPadBank drumPadBank;

	private boolean hasDrumPads = false;
	private PadMode altMode;
	private final boolean[] isSelected = new boolean[16];
	private final MaschineLayer muteLayer;
	private final MaschineLayer soloLayer;
	private final MaschineLayer eraseLayer;

	public DrumPadMode(final MaschineExtension driver, final String name, final NoteFocusHandler noteFocusHandler,
			final VeloctiyHandler velocityHandler, final DisplayLayer associatedDisplay) {
		super(driver, name, noteFocusHandler, velocityHandler, associatedDisplay);

		muteLayer = new MaschineLayer(driver, name + "-mute");
		soloLayer = new MaschineLayer(driver, name + "-solo");
		eraseLayer = new MaschineLayer(driver, name + "-erase");

		drumPadBank = driver.getPrimaryDevice().createDrumPadBank(16);
		drumPadBank.setIndication(true);

		drumPadBank.scrollPosition().addValueObserver(scrollPos -> {
			padOffset = scrollPos;
			selectPad(getSelectedIndex());
			applyScale();
		});

		final PadButton[] buttons = driver.getPadButtons();
		for (int i = 0; i < buttons.length; i++) {
			final int index = i;
			final PadButton button = buttons[i];
			final DrumPad pad = drumPadBank.getItemAt(index);
			pad.color().markInterested();
			pad.name().markInterested();
			pad.exists().markInterested();
			pad.solo().markInterested();
			pad.mute().markInterested();
			pad.addIsSelectedInEditorObserver(selected -> {
				if (selected) {
					noteFocusHandler.notifyDrumPadSelected(pad, padOffset, index);
					isSelected[index] = true;
				} else {
					isSelected[index] = false;
				}
			});
			pad.color().addValueObserver((r, g, b) -> padColorChanged(pad, index, r, g, b));
			bindLightState(() -> computeGridLedState(index, pad), button);
			bindShift(button);
			selectLayer.bindPressed(button, () -> selectPad(index));
			muteLayer.bindPressed(button, () -> mutePad(index));
			muteLayer.bindLightState(() -> computeGridLedStateMute(index, pad), button);
			soloLayer.bindPressed(button, () -> soloPad(index));
			soloLayer.bindLightState(() -> computeGridLedStateSolo(index, pad), button);
		}
	}

	@Override
	public void setModifierState(final ModifierState modstate, final boolean active) {
		if (modstate == ModifierState.SELECT) {
			enableLayer(selectLayer, active);
		} else if (modstate == ModifierState.SHIFT) {
			enableLayer(getShiftLayer(), active);
		} else if (modstate == ModifierState.MUTE) {
			enableLayer(muteLayer, active);
		} else if (modstate == ModifierState.SOLO) {
			enableLayer(soloLayer, active);
		} else if (modstate == ModifierState.ERASE) {
			enableLayer(eraseLayer, active);
		}
	}

	private void padColorChanged(final DrumPad pad, final int index, final float r, final float g, final float b) {
		if (noteFocusHandler == null) {
			return;
		}
		noteFocusHandler.notifyPadColorChanged(pad, index, r, g, b);
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

	private int getSelectedIndex() {
		for (int i = 0; i < 16; i++) {
			if (isSelected[i]) {
				return i;
			}
		}
		return 0;
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

	private void mutePad(final int index) {
		final DrumPad pad = drumPadBank.getItemAt(index);
		pad.mute().toggle();
	}

	private void soloPad(final int index) {
		final DrumPad pad = drumPadBank.getItemAt(index);
		pad.solo().toggle();
	}

	void selectPad(final int index) {
		final DrumPad pad = drumPadBank.getItemAt(index);
		pad.selectInEditor();

		noteFocusHandler.notifyDrumPadSelected(pad, padOffset, index);
	}

	private InternalHardwareLightState computeGridLedStateSolo(final int index, final DrumPad pad) {
		if (hasDrumPads) { // YELLOW
			if (pad.exists().get()) {
				int color = Colors.YELLOW.getIndexValue(ColorBrightness.DARKENED);
				if (pad.solo().get()) {
					color += 2;
				}
				if (playing[index]) {
					color += 1;
				}
				return RgbLedState.colorOf(color);
			}
			return RgbLedState.OFF;
		} else {
			return colorStateOfNoPads(index);
		}
	}

	private InternalHardwareLightState computeGridLedStateMute(final int index, final DrumPad pad) {
		if (hasDrumPads) {
			if (pad.exists().get()) {
				int color = Colors.LIGHT_ORANGE.getIndexValue(ColorBrightness.DARKENED);
				if (pad.mute().get()) {
					color += 2;
				}
				if (playing[index]) {
					color += 1;
				}
				return RgbLedState.colorOf(color);
			}
			return RgbLedState.OFF;
		} else {
			return colorStateOfNoPads(index);
		}
	}

	public InternalHardwareLightState colorStateOfNoPads(final int index) {
		int color = NIColorUtil.convertColor(getDriver().getCursorTrack().color());
		if (playing[index]) {
			color += 2;
		}
		return RgbLedState.colorOf(color); // TO this need to be done with lookup table
	}

	private InternalHardwareLightState computeGridLedState(final int index, final DrumPad pad) {
		if (hasDrumPads) {
			int color = NIColorUtil.convertColor(pad.color());
			if (playing[index] || isSelected[index]) {
				color += 2;
			}
			return RgbLedState.colorOf(color); // TO this need to be done with lookup table
		} else {
			return colorStateOfNoPads(index);
		}
	}

	@Override
	void applyScale() {
		if (!isActive()) {
			return;
		}
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
