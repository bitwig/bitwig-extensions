package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.StepMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.StepViewPosition;

public class StepEditDisplayLayer extends DisplayLayer {

	private StepMode stepMode;
	private String trackName = "";
	private String clipName = "";

	private final boolean[] touched = new boolean[8];

	public StepEditDisplayLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
		final ControllerHost host = driver.getHost();
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
		bind(knobs[0], createIncrementBinder(host, this::changeFocusNote));
		bind(knobs[1], createIncrementBinder(host, this::changeRefVelocity));
		bind(knobs[2], createIncrementBinder(host, this::changeLoopLength));
		bind(knobs[3], createIncrementBinder(host, this::changeGrid));
		bind(knobs[4], createIncrementBinder(host, this::changeNoteLength));
		final ModeButton[] buttons = getDriver().getDisplayButtons();
		bindPressed(buttons[4], this::createEmptyClip);
		bindPressed(buttons[5], this::duplicateClip);
	}

	private void changeGrid(final int amount) {
		stepMode.getPositionHandler().modifyGrid(amount);
		updateDetails();
		updateBottomLeft();
	}

	private void changeNoteLength(final int amount) {
		stepMode.updateHeldNoteLength(amount);
		updateBottomRight();
	}

	private void changeLoopLength(final int amount) {
		final double loopLen = stepMode.getClip().getLoopLength().get();
		final double newLen = loopLen + amount;
		if (newLen >= 1) {
			stepMode.getClip().getLoopLength().set(newLen);
			getDriver().getApplication().zoomToFit();
		}
	}

	private void changeFocusNote(final int amount) {
		stepMode.changeFocusNote(amount);
		updateBottomLeft();
	}

	private void changeRefVelocity(final int amount) {
		final int value = stepMode.getRefVelocity();
		if (stepMode.hasPressedNotes()) {
			stepMode.changeNoteVelocity(amount);
		} else {
			final int newValue = value + amount;
			if (newValue >= 1 && newValue < 128) {
				stepMode.setRefVelocity(newValue);
				updateBottomLeft();
			}
		}
	}

	public void setStepLayer(final StepMode stepMode) {

		this.stepMode = stepMode;
		final Clip clip = this.stepMode.getClip();
		final StepViewPosition positionHandler = stepMode.getPositionHandler();

		clip.getTrack().name().addValueObserver(s -> {
			trackName = s.trim();
			updateTrackName();
		});
		clip.clipLauncherSlot().name().addValueObserver(s -> {
			clipName = DisplayUtil.padString(s, 8);
			updateClipName();
		});
		this.stepMode.setFocusChangerListener(s -> {
			this.updateClipName();
		});
		getDriver().getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
			this.updateClipName();
		});
		clip.getLoopLength().addValueObserver(loopLen -> {
			updateBottomLeft();
		});
		final ModeButton[] buttons = getDriver().getDisplayButtons();
		bindPressed(buttons[2], positionHandler.canScrollLeft(), this::scrollLeft);
		bindPressed(buttons[3], positionHandler.canScrollRight(), this::scrollRight);
	}

	@Override
	public void notifyEncoderTouched(final int index, final boolean v) {
		touched[index] = v;
		if (index < 4) {
			updateBottomLeft();
		}
	}

	@Override
	protected void doNotifyMacroDown(final boolean active) {
		// Do nothing
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
		updateDetails();
	}

	public void updateDetails() {
		if (!isActive()) {
			return;
		}
		String topLeft = "";
		String topRight = "";
		String bottomLeft = "";
		String bottomRight = "";
		if (isInfoModeActive()) {
			topLeft = " ---- | ---- | <POS | POS> ";
			topRight = "+CLIP | DUPL | ---- | ---- ";
			bottomLeft = getLeftValues(true);
			bottomRight = " ---- | ---- | ---- | ---- ";
		} else {
			topRight = "EDIT POS:" + DisplayUtil.beatsFormatted(stepMode.getPositionHandler().getPosition() * 16);
			topLeft = getTrackClipInfo();
			bottomLeft = getLeftValues(false);
			bottomRight = "";
		}
		sendToDisplay(TOP_LEFT, topLeft);
		sendToDisplay(TOP_RIGHT, topRight);
		sendToDisplay(BOTTOM_LEFT, bottomLeft);
		sendToDisplay(BOTTOM_RIGHT, bottomRight);

	}

	public String getRightValues(final boolean forceValue) {
		final StringBuilder sb = new StringBuilder();
		sb.append("I.NLEN ");
		return sb.toString();
	}

	public String getLeftValues(final boolean forceValue) {
		final StringBuilder sb = new StringBuilder();
		if (touched[0]) {
			sb.append(DisplayUtil.padString(stepMode.getFocus(), 27));
		} else {
			if (forceValue) {
				sb.append(DisplayUtil.padString(stepMode.getFocus(), 6));
			} else {
				sb.append("I.NOTE");
			}
			sb.append("|");
			if (touched[1] || forceValue) {
				sb.append(DisplayUtil.padValue(this.stepMode.getRefVelocity(), 6));
			} else {
				sb.append("I.VEL ");
			}
			sb.append("|");
			if (touched[2] || forceValue) {
				sb.append(
						DisplayUtil.padString(DisplayUtil.beatsFormatted(stepMode.getClip().getLoopLength().get()), 6));
			} else {
				sb.append("CL.LEN");
			}
			sb.append("|");
			if (touched[3] || forceValue) {
				sb.append(DisplayUtil.padString(stepMode.getPositionHandler().getGridValue(), 6));
			} else {
				sb.append("GR.RES");
			}
		}
		return sb.toString();
	}

	private void duplicateClip() {
		getDriver().getFocusClip().getMainCursoClip().duplicate();
	}

	private void createEmptyClip() {
		final CursorTrack cursorTrack = getDriver().getCursorTrack();
		final ClipLauncherSlotBank slotBank = cursorTrack.clipLauncherSlotBank();
		final int selIndex = getSelectedIndex(slotBank);
		final int nextEmpty = getNextEmpty(slotBank, selIndex);
		if (nextEmpty != -1) {
			slotBank.createEmptyClip(nextEmpty, 4);
		}

	}

	private int getSelectedIndex(final ClipLauncherSlotBank slotBank) {
		final int n = slotBank.getSizeOfBank();
		for (int i = 0; i < n; i++) {
			final ClipLauncherSlot clipSlot = slotBank.getItemAt(i);
			if (clipSlot.isSelected().get()) {
				return i;
			}
		}
		return 0;
	}

	private int getNextEmpty(final ClipLauncherSlotBank slotBank, final int selectedIndex) {
		final int n = slotBank.getSizeOfBank();
		for (int i = selectedIndex; i < n; i++) {
			final ClipLauncherSlot clipSlot = slotBank.getItemAt(i);
			if (!clipSlot.hasContent().get()) {
				return i;
			}
		}
		return -1;
	}

	private void scrollLeft() {
		stepMode.getPositionHandler().scrollLeft();
		updateDetails();
	}

	private void scrollRight() {
		stepMode.getPositionHandler().scrollRight();
		updateDetails();
	}

	private void updateTrackName() {
		if (isActive()) {
			sendToDisplay(TOP_LEFT, getTrackClipInfo());
		}
	}

	public String getTrackClipInfo() {
		return "TR:" + trackName + " C:" + clipName;
	}

	private void updateClipName() {
		if (isActive()) {
			sendToDisplay(TOP_LEFT, getTrackClipInfo());
		}
	}

	private void updateBottomLeft() {
		if (isActive()) {
			sendToDisplay(BOTTOM_LEFT, getLeftValues(false));
		}
	}

	private void updateBottomRight() {
		if (isActive()) {
			sendToDisplay(BOTTOM_RIGHT, getRightValues(false));
		}
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		setKnobSensitivity(1.0);
		final RelativeHardwareKnob[] knobs = getDriver().getDisplayKnobs();
		knobs[0].setStepSize(1 / 128.0);
		knobs[1].setStepSize(1 / 256.0);
		knobs[2].setStepSize(1 / 32.0);
		knobs[3].setStepSize(1 / 32.0);
		knobs[4].setStepSize(1 / 64.0);
		updateTrackName();
		updateClipName();
		updateDetails();
		updateBottomLeft();
		updateBottomRight();
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
	}

}
