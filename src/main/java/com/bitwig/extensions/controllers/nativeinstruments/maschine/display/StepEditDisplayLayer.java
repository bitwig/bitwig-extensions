package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
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
		bind(knobs[0],
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> changeFocusNote(1), () -> "+"),
						host.createAction(() -> changeFocusNote(-1), () -> "-")));
		bind(knobs[1],
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> changeRefVelocity(1), () -> "+"),
						host.createAction(() -> changeRefVelocity(-1), () -> "-")));
		bind(knobs[2],
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> changeLoopLength(1), () -> "+"),
						host.createAction(() -> changeLoopLength(-1), () -> "-")));
		bind(knobs[3], host.createRelativeHardwareControlStepTarget(host.createAction(() -> changeGrid(1), () -> "+"),
				host.createAction(() -> changeGrid(-1), () -> "-")));
	}

	private void changeGrid(final int amount) {
		stepMode.getPositionHandler().modifyGrid(amount);
		updateDetails();
		updateRightBottom();
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
		updateRightBottom();
	}

	private void changeRefVelocity(final int amount) {
		final int value = stepMode.getRefVelocity();
		final int newValue = value + amount;
		if (newValue >= 1 && newValue < 128) {
			stepMode.setRefVelocity(newValue);
			updateRightBottom();
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
			updateRightBottom();
		});
		final ModeButton[] buttons = getDriver().getDisplayButtons();
		bindPressed(buttons[2], positionHandler.canScrollLeft(), this::scrollLeft);
		bindPressed(buttons[3], positionHandler.canScrollRight(), this::scrollRight);
	}

	@Override
	public void notifyEncoderTouched(final int index, final boolean v) {
		touched[index] = v;
		if (index < 4) {
			updateRightBottom();
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
			topRight = " ---- | ---- | ---- | ---- ";
			bottomLeft = getValueDisplay(true);
			bottomRight = " ---- | ---- | ---- | ---- ";
		} else {
			topRight = "EDIT POS:" + DisplayUtil.beatsFormatted(stepMode.getPositionHandler().getPosition() * 16);
			topLeft = getTrackClipInfo();
			bottomLeft = getValueDisplay(false);
			bottomRight = "";
		}
		sendToDisplay(TOP_LEFT, topLeft);
		sendToDisplay(TOP_RIGHT, topRight);
		sendToDisplay(BOTTOM_LEFT, bottomLeft);
		sendToDisplay(BOTTOM_RIGHT, bottomRight);

	}

	public String getValueDisplay(final boolean forceValue) {
		final StringBuilder sb = new StringBuilder();
		if (touched[0] || forceValue) {
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
			sb.append(DisplayUtil.padString(DisplayUtil.beatsFormatted(stepMode.getClip().getLoopLength().get()), 6));
		} else {
			sb.append("CL.LEN");
		}
		sb.append("|");
		if (touched[3] || forceValue) {
			sb.append(DisplayUtil.padString(stepMode.getPositionHandler().getGridValue(), 6));
		} else {
			sb.append("GR.RES");
		}
		return sb.toString();
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

	private void updateRightBottom() {
		if (isActive()) {
			sendToDisplay(BOTTOM_LEFT, getValueDisplay(false));
		}
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		final RelativeHardwareKnob[] knobs = getDriver().getDisplayKnobs();
		for (int i = 0; i < knobs.length; i++) {
			knobs[i].setStepSize(1 / 64.0);
		}
		updateTrackName();
		updateClipName();
		updateDetails();
		updateRightBottom();
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
	}

}
