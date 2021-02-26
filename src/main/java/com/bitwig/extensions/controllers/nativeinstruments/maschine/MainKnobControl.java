package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;

public class MainKnobControl {
	public enum Mode {
		NONE, VOLUME, SWING, TEMPO;
	}

	private Mode mode = Mode.NONE;
	private final MasterTrack masterTrack;
	private final Transport transport;
	private final Groove groove;
	private boolean displayActive = false;
	private boolean shiftDown = false;

	public MainKnobControl(final MaschineExtension driver) {
		this.masterTrack = driver.getMasterTrack();
		this.transport = driver.getTranport();
		this.groove = driver.getHost().createGroove();
		this.groove.getEnabled().markInterested();
		masterTrack.volume().displayedValue().markInterested();
		transport.tempo().displayedValue().markInterested();
		groove.getAccentAmount().displayedValue().markInterested();
		groove.getShuffleAmount().displayedValue().markInterested();

		transport.tempo().displayedValue().addValueObserver(s -> {
			if (mode == Mode.TEMPO && displayActive) {
				driver.sendToDisplayTemp(DisplayLayer.TOP_LEFT, "Tempo = " + s);
			}
		});
		masterTrack.volume().displayedValue().addValueObserver(s -> {
			if (mode == Mode.VOLUME && displayActive) {
				driver.sendToDisplayTemp(DisplayLayer.TOP_LEFT, "Main = " + s);
			}
		});
		groove.getAccentAmount().displayedValue().addValueObserver(s -> {
			if (mode == Mode.SWING && displayActive && shiftDown) {
				driver.sendToDisplayTemp(DisplayLayer.TOP_LEFT, "Groov Accent = " + s);
			}
		});
		groove.getShuffleAmount().displayedValue().addValueObserver(s -> {
			if (mode == Mode.SWING && displayActive && !shiftDown) {
				driver.sendToDisplayTemp(DisplayLayer.TOP_LEFT, "Groove Shuffle = " + s);
			}
		});

		createModeButton(driver, "VOLUME", CcAssignment.VOLUME, Mode.VOLUME, () -> toggleMode(Mode.VOLUME));
		createModeButton(driver, "SWING", CcAssignment.SWING, Mode.SWING, () -> {
			if (driver.isShiftDown()) {
				if (groove.getEnabled().value().get() == 0) {
					groove.getEnabled().value().set(1);
				} else {
					groove.getEnabled().value().set(0);
				}
			} else {
				toggleMode(Mode.SWING);
			}
		});
		createModeButton(driver, "TEMPO", CcAssignment.TEMPO, Mode.TEMPO, () -> toggleMode(Mode.TEMPO));
	}

	private void createModeButton(final MaschineExtension driver, final String name, final CcAssignment assignment,
			final Mode assgnMode, final Runnable action) {
		final HardwareButton button = driver.create4DSelectButton(name + "_BUTTON", assignment);
		final OnOffHardwareLight backgroundLight = (OnOffHardwareLight) button.backgroundLight();
		backgroundLight.onUpdateHardware(() -> driver.sendLedUpdate(assignment, mode == assgnMode ? 127 : 0));
		backgroundLight.isOn().setValueSupplier(() -> mode == assgnMode);
		button.isPressed().addValueObserver(v -> {
			if (v) {
				action.run();
			}
		});
	}

	private void toggleMode(final Mode newMode) {
		this.mode = newMode == this.mode ? Mode.NONE : newMode;
	}

	public void setDisplayActive(final boolean specialModeActive) {
		this.displayActive = specialModeActive;
	}

	public void dataChange(final int diff, final boolean shiftDown) {
		// RemoteConsole.out.println("Change {}", diff);
		this.shiftDown = shiftDown;
		switch (mode) {
		case VOLUME:
			masterTrack.volume().value().inc(diff, 128);
			break;
		case TEMPO:
			transport.tempo().value().incRaw(shiftDown ? diff * 0.1 : diff);
			break;
		case SWING:
			if (shiftDown) {
				groove.getAccentAmount().value().inc(diff, 100);
			} else {
				groove.getShuffleAmount().value().inc(diff, 100);
			}
			break;
		default:
			break;
		}

	}
}
