package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.VPotMode;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTracker;
import com.bitwig.extensions.controllers.mackie.devices.Devices;
import com.bitwig.extensions.controllers.mackie.devices.EqDevice;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class MixControl {
	private final MixerSectionHardware hwControls;
	final MackieMcuProExtension driver;

	final MixerLayerGroup mainGroup;
	final MixerLayerGroup globalGroup;

	private final LayerState currentState;

	private LayerConfiguration currentConfiguration;

	private final LayerConfiguration panConfiguration = new MixerLayerConfiguration("PAN", this, ParamElement.PAN);
	private final LayerConfiguration sendConfiguration = new MixerLayerConfiguration("SEND", this,
			ParamElement.SENDMIXER);
	private final TrackLayerConfiguration sendTrackConfiguration;
	private final TrackLayerConfiguration instrumentTrackConfiguration;
	private final TrackLayerConfiguration pluginTrackConfiguration;
	private final TrackLayerConfiguration eqTrackConfiguration;

	private final BooleanValueObject fadersTouched = new BooleanValueObject();
	private int touchCount = 0;
	private final SectionType type;

	public MixControl(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
			final int sectionIndex, final SectionType type) {
		this.driver = driver;
		this.type = type;
		hwControls = new MixerSectionHardware(driver, midiIn, midiOut, sectionIndex, type);
		for (int i = 0; i < 8; i++) {
			hwControls.assignFaderTouchAction(i, touched -> handleTouch(touched));
		}

		mainGroup = new MixerLayerGroup("MN", this);
		globalGroup = new MixerLayerGroup("GL", this);
		sendConfiguration.setNavigateHorizontalHandler(direction -> {
			mainGroup.navigateHorizontally(direction);
			globalGroup.navigateHorizontally(direction);
		});

		sendTrackConfiguration = new TrackLayerConfiguration("SN_TR", this);
		instrumentTrackConfiguration = new TrackLayerConfiguration("INSTRUMENT", this);
		pluginTrackConfiguration = new TrackLayerConfiguration("AUDIOFX", this);
		eqTrackConfiguration = new TrackLayerConfiguration("EQ_DEVICE", this);

		currentConfiguration = panConfiguration;
		currentState = new LayerState(panConfiguration);

		driver.getFlipped()
				.addValueObserver(flipped -> currentState.updateState(currentConfiguration, getActiveDisplayLayer()));
		driver.getGlobalViewActive().addValueObserver(
				globalView -> currentState.updateState(currentConfiguration, getActiveDisplayLayer()));
		driver.getTrackChannelMode().addValueObserver(v -> notifyModeChange(driver.getTrackChannelMode().getMode()));
		fadersTouched.addValueObserver(v -> reactToFaderTouched(v));
	}

	private void reactToFaderTouched(final boolean touched) {
		if (touched) {
			currentState.updateDisplayState(getActiveDisplayLayer());
		} else {
			driver.scheduleAction("TOUCH", 1500, () -> {
				currentState.updateDisplayState(getActiveDisplayLayer());
			});
		}
	}

	public DisplayLayer getActiveDisplayLayer() {
		final boolean flipped = driver.getFlipped().get();
		final boolean touched = fadersTouched.get();
		final int displayer = !flipped && touched || flipped && !touched ? 1 : 0;
		return currentConfiguration.getDisplayLayer(displayer);
	}

	MixerSectionHardware getHwControls() {
		return hwControls;
	}

	public ModifierValueObject getModifier() {
		return driver.getModifier();
	}

	public void resetFaders() {
		hwControls.resetFaders();
	}

	public LcdDisplay getDisplay() {
		return hwControls.getMainDisplay();
	}

	public void clearAll() {
		hwControls.getMainDisplay().clearAll();
	}

	public void exitMessage() {
		hwControls.getMainDisplay().exitMessage();
	}

	public void applyVuMode(final VuMode mode) {
		hwControls.getMainDisplay().appyVuMode(mode);
	}

	public MackieMcuProExtension getDriver() {
		return driver;
	}

	public void navigateLeftRight(final int direction) {
		currentConfiguration.navigateHorizontal(direction);
	}

	public void navigateUpDown(final int direction) {
		currentConfiguration.navigateVertical(direction);
	}

	public void notifyModeAdvance() {
		switch (driver.getVpotMode()) {
		case EQ:
			switch (driver.getVpotMode()) {
			case EQ:
				final boolean hasEq = driver.getEqDevice().exists().get();
				if (!hasEq) {
					final InsertionPoint ip = driver.getCursorTrack().endOfDeviceChainInsertionPoint();
					ip.insertBitwigDevice(Devices.EQ_PLUS.getUuid());
				}
				break;
			case PLUGIN:
				break;
			case INSTRUMENT:
				break;
			default:
			}
			break;
		case PLUGIN:
			break;
		case INSTRUMENT:
			break;
		default:
		}
	}

	public void notifyModeChange(final VPotMode mode) {
		switch (mode) {
		case EQ:
			currentConfiguration = eqTrackConfiguration;
			break;
		case INSTRUMENT:
			currentConfiguration = instrumentTrackConfiguration;
			break;
		case PAN:
			currentConfiguration = panConfiguration;
			break;
		case PLUGIN:
			currentConfiguration = pluginTrackConfiguration;
			break;
		case SEND:
			if (type != SectionType.MAIN) {
				currentConfiguration = sendConfiguration;
			} else if (driver.getTrackChannelMode().get()) {
				currentConfiguration = sendTrackConfiguration;
			} else {
				currentConfiguration = sendConfiguration;
			}
			break;
		default:
			break;
		}
		currentState.updateState(currentConfiguration, getActiveDisplayLayer());
	}

	private void handleTouch(final boolean touched) {
		if (touched) {
			touchCount++;
		} else if (touchCount > 0) {
			touchCount--;
		}
		if (touchCount > 0 && !fadersTouched.get()) {
			fadersTouched.set(true);
		} else if (touchCount == 0 && fadersTouched.get()) {
			fadersTouched.set(false);
		}
	}

	public void fullHardwareUpdate() {
		hwControls.fullHardwareUpdate();
	}

	public void resetLeds() {
		hwControls.resetLeds();
	}

	public void initMainControl(final TrackBank mixerTrackBank, final TrackBank globalTrackBank) {
		mainGroup.init(mixerTrackBank);
		globalGroup.init(globalTrackBank);
	}

	public void initTrackControl(final CursorTrack cursorTrack, final DeviceTracker instrumentDevice,
			final DeviceTracker pluginDevice, final EqDevice eqDevice) {
		final SendBank sendBank = cursorTrack.sendBank();
		for (int i = 0; i < 8; i++) {
			sendTrackConfiguration.addBinding(i, sendBank.getItemAt(i), RingDisplayType.FILL_LR);
			instrumentTrackConfiguration.addBinding(i, instrumentDevice.getParameter(i), RingDisplayType.FILL_LR);
			pluginTrackConfiguration.addBinding(i, pluginDevice.getParameter(i), RingDisplayType.FILL_LR);
			eqTrackConfiguration.addBinding(i, eqDevice.getEqBands().get(i),
					(pindex, pslot) -> eqDevice.handleResetInvoked(pindex, driver.getModifier()));
		}
		sendTrackConfiguration.setNavigateHorizontalHandler(direction -> {
			if (direction < 0) {
				sendBank.scrollBackwards();
			} else {
				sendBank.scrollForwards();
			}
		});
		instrumentTrackConfiguration
				.setNavigateHorizontalHandler(direction -> navigateDeviceParameters(instrumentDevice, direction));
		instrumentTrackConfiguration
				.setNavigateVerticalHandler(direction -> navigateDevices(instrumentDevice, direction));
		instrumentTrackConfiguration.getDisplayLayer(0).setCenteredText("no instrument on track",
				"<< select instrument device from Browser >>");
		pluginTrackConfiguration.setNavigateHorizontalHandler(direction -> navigateDevices(pluginDevice, direction));
		pluginTrackConfiguration.setNavigateVerticalHandler(direction -> navigateDevices(pluginDevice, direction));
		pluginTrackConfiguration.getDisplayLayer(0).setCenteredText("no audio fx on track",
				"<< select device from Browser >>");

		eqTrackConfiguration.setNavigateHorizontalHandler(eqDevice::navigateParameterBanks);
		eqTrackConfiguration.getDisplayLayer(0).setCenteredText("no EQ+ device on track",
				"<< press EQ button to insert EQ+ device >>");

		pluginDevice.getDevice().exists()
				.addValueObserver(exist -> pluginTrackConfiguration.getDisplayLayer(0).enableFullTextMode(!exist));

		instrumentDevice.getDevice().exists()
				.addValueObserver(exist -> instrumentTrackConfiguration.getDisplayLayer(0).enableFullTextMode(!exist));

		eqDevice.getDevice().exists()
				.addValueObserver(exist -> eqTrackConfiguration.getDisplayLayer(0).enableFullTextMode(!exist));

		driver.getCursorDevice().deviceType().markInterested();
//			driver.getCursorDevice().name().addValueObserver(name -> handleDeviceNameChanged(name));
	}

	public void navigateDeviceParameters(final DeviceTracker device, final int direction) {
		if (direction < 0) {
			device.selectPreviousParameterPage();
		} else {
			device.selectNextParameterPage();
		}
	}

	public void navigateDevices(final DeviceTracker device, final int direction) {
		if (direction < 0) {
			device.selectPreviousDevice();
		} else {
			device.selectNextDevice();
		}
	}

	public void handleSoloAction(final Track channel) {
		if (!channel.exists().get()) {
			return;
		}
		if (channel.solo().get()) {
			channel.solo().set(false);
		} else {
			// project.unsoloAll();
			channel.solo().set(true);
		}
	}

	void handleTrackSelection(final Track channel) {
		if (channel.exists().get()) {
			if (driver.getModifier().isControl()) {
				channel.deleteObject();
			} else if (driver.getModifier().isAlt()) {
				channel.stop();
			} else if (driver.getModifier().isOption()) {
				driver.getApplication().navigateIntoTrackGroup(channel);
			} else {
				channel.selectInMixer();
			}
		} else {
			if (driver.getModifier().isShift()) {
				driver.getApplication().createAudioTrack(-1);
			} else if (driver.getModifier().isSet(ModifierValueObject.ALT)) {
				driver.getApplication().createEffectTrack(-1);
			} else {
				driver.getApplication().createInstrumentTrack(-1);
			}
		}
	}

}
