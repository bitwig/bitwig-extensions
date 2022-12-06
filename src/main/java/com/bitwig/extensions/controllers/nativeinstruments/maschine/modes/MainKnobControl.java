package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.LayoutType;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;

public class MainKnobControl extends MaschineLayer {
	public enum Mode {
		NONE, VOLUME, SWING, TEMPO;
	}

	private Mode mode = Mode.NONE;
	private final MasterTrack masterTrack;
	private final Transport transport;
	private final Groove groove;
	private boolean displayActive = false;
	private boolean shiftDown = false;
	private MaschineLayer currentNavLayer;
	private LayoutType layoutType;
	private final MaschineLayer navSessionLauncherLayer;
	private final MaschineLayer navSessionArrangerLayer;

	public MainKnobControl(final MaschineExtension driver) {
		super(driver, "MAIN_KNOB_LAYER");
		this.masterTrack = driver.getMasterTrack();
		this.transport = driver.getTranport();
		this.groove = driver.getHost().createGroove();
		this.groove.getEnabled().markInterested();

		initValueObservation(driver);

		final RelativeHardwareKnob fourDKnob = driver.getSurface().createRelativeHardwareKnob("4D_WHEEL");
		fourDKnob.setAdjustValueMatcher(driver.getMidiIn().createRelative2sComplementCCValueMatcher(
				CcAssignment.DKNOB_TURN.getChannel(), CcAssignment.DKNOB_TURN.getCcNr(), 128));
		fourDKnob.setStepSize(1 / 128.0);

		final HardwareActionBindable incAction = driver.getHost().createAction(() -> dataChange(1, shiftDown),
				() -> "+");
		final HardwareActionBindable decAction = driver.getHost().createAction(() -> dataChange(-1, shiftDown),
				() -> "-");
		bind(fourDKnob, driver.getHost().createRelativeHardwareControlStepTarget(incAction, decAction));

		final HardwareButton volumeModeButton = createModeButton(driver, "VOLUME", CcAssignment.VOLUME, Mode.VOLUME);
		bindPressed(volumeModeButton, () -> toggleMode(Mode.VOLUME));
		final HardwareButton swingModeButton = createModeButton(driver, "SWING", CcAssignment.SWING, Mode.SWING);
		bindPressed(swingModeButton, () -> {
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

		final HardwareButton tempoModeButton = createModeButton(driver, "TEMPO", CcAssignment.TEMPO, Mode.TEMPO);
		bindPressed(tempoModeButton, () -> toggleMode(Mode.TEMPO));

		final HardwareButton navDownButton = createButtonNavButton(driver, "NAV4D_DOWN", CcAssignment.DKNOB_DOWN);
		final HardwareButton navUpButton = createButtonNavButton(driver, "NAV4D_UP", CcAssignment.DKNOB_UP);
		final HardwareButton navLeftButton = createButtonNavButton(driver, "NAV4D_LEFT", CcAssignment.DKNOB_LEFT);
		final HardwareButton navRightButton = createButtonNavButton(driver, "NAV4D_RIGHT", CcAssignment.DKNOB_RIGHT);
		final TrackBank trackBank = driver.getTrackBank();
		final HardwareButton pushButton = driver.getSurface().createHardwareButton("NAV4_PUSH");
		pushButton.pressedAction()
				.setActionMatcher(CcAssignment.DKNOB_PUSH.createActionMatcher(driver.getMidiIn(), 127));
		pushButton.releasedAction()
				.setActionMatcher(CcAssignment.DKNOB_PUSH.createActionMatcher(driver.getMidiIn(), 0));

		bindPressed(pushButton, () -> getDriver().getJogWheelDestination().jogWheelPush(true));
		bindReleased(pushButton, () -> getDriver().getJogWheelDestination().jogWheelPush(false));
		navSessionLauncherLayer = initLaunchNav(driver, trackBank, navDownButton, navUpButton, navLeftButton,
				navRightButton);

		navSessionArrangerLayer = initArrangeNav(driver, trackBank, navDownButton, navUpButton, navLeftButton,
				navRightButton);

		currentNavLayer = navSessionLauncherLayer;
		currentNavLayer.activate();
	}

	public MaschineLayer initLaunchNav(final MaschineExtension driver, final TrackBank trackBank,
			final HardwareButton navDownButton, final HardwareButton navUpButton, final HardwareButton navLeftButton,
			final HardwareButton navRightButton) {
		final MaschineLayer layer = new MaschineLayer(driver, "knobNavigateLaunchSessionLayer");

		layer.bindPressed(navLeftButton, () -> trackBank.scrollBackwards());
		layer.bindLightState(navLeftButton, trackBank.canScrollChannelsUp());
		layer.bindPressed(navRightButton, () -> trackBank.scrollForwards());
		layer.bindLightState(navRightButton, trackBank.canScrollChannelsDown());

		layer.bindPressed(navDownButton, () -> trackBank.sceneBank().scrollForwards());
		layer.bindLightState(navDownButton, trackBank.sceneBank().canScrollForwards());
		layer.bindPressed(navUpButton, () -> trackBank.sceneBank().scrollBackwards());
		layer.bindLightState(navUpButton, trackBank.sceneBank().canScrollBackwards());
		return layer;
	}

	public MaschineLayer initArrangeNav(final MaschineExtension driver, final TrackBank trackBank,
			final HardwareButton navDownButton, final HardwareButton navUpButton, final HardwareButton navLeftButton,
			final HardwareButton navRightButton) {
		final MaschineLayer layer = new MaschineLayer(driver, "knobNavigateArrangerSessionLayer");

		layer.bindPressed(navDownButton, () -> trackBank.scrollForwards());
		layer.bindLightState(navDownButton, trackBank.canScrollChannelsDown());

		layer.bindPressed(navUpButton, () -> trackBank.scrollBackwards());
		layer.bindLightState(navUpButton, trackBank.canScrollChannelsUp());

		layer.bindPressed(navLeftButton, () -> trackBank.sceneBank().scrollBackwards());
		layer.bindLightState(navLeftButton, trackBank.sceneBank().canScrollBackwards());
		layer.bindPressed(navRightButton, () -> trackBank.sceneBank().scrollForwards());
		layer.bindLightState(navRightButton, trackBank.sceneBank().canScrollForwards());
		return layer;
	}

	public void notifyPanelLayout(final LayoutType layout) {
		this.layoutType = layout;
		if (layoutType == LayoutType.LAUNCHER) {
			currentNavLayer.deactivate();
			currentNavLayer = navSessionLauncherLayer;
			currentNavLayer.activate();
		} else {
			currentNavLayer.deactivate();
			currentNavLayer = navSessionArrangerLayer;
			currentNavLayer.activate();
		}
	}

	public void initValueObservation(final MaschineExtension driver) {
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
	}

	private HardwareButton createButtonNavButton(final MaschineExtension driver, final String name,
			final CcAssignment assignment) {
		final MidiIn midiIn = driver.getMidiIn();
		final HardwareSurface surface = driver.getSurface();
		final HardwareButton navButton = surface.createHardwareButton(name);
		navButton.pressedAction().setActionMatcher(assignment.createActionMatcher(midiIn, 127));
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(name + "_LED");
		navButton.setBackgroundLight(led);
		return navButton;
	}

	private HardwareButton createModeButton(final MaschineExtension driver, final String name,
			final CcAssignment assignment, final Mode assgnMode) {
		final HardwareButton button = driver.create4DSelectButton(name + "_BUTTON", assignment);
		final OnOffHardwareLight backgroundLight = (OnOffHardwareLight) button.backgroundLight();
		backgroundLight.onUpdateHardware(() -> driver.sendLedUpdate(assignment, mode == assgnMode ? 127 : 0));
		backgroundLight.isOn().setValueSupplier(() -> mode == assgnMode);
		return button;
	}

	private void toggleMode(final Mode newMode) {
		this.mode = newMode == this.mode ? Mode.NONE : newMode;
	}

	public void setDisplayActive(final boolean specialModeActive) {
		this.displayActive = specialModeActive;
	}

	public void dataChange(final int diff, final boolean shiftDown) {
		this.shiftDown = shiftDown;
		switch (mode) {
		case VOLUME:
			if (shiftDown) {
				masterTrack.volume().value().inc(diff, 128);
			} else {
				masterTrack.volume().value().inc(diff * 4, 128);
			}
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
			getDriver().getJogWheelDestination().jogWheelAction(diff);
			break;
		}

	}
}
