package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.MixerMode;
import com.bitwig.extensions.controllers.mackie.VPotMode;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mackie.devices.SpecialDevices;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.layer.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;

public class MixControl implements LayerStateHandler {
	private final MixerSectionHardware hwControls;
	final MackieMcuProExtension driver;

	final MixerLayerGroup mainGroup;
	final MixerLayerGroup globalGroup;
	final MixerLayerGroup drumGroup;

	private final LayerState layerState;

	private LayerConfiguration currentConfiguration;

	private final LayerConfiguration panConfiguration = new MixerLayerConfiguration("PAN", this, ParamElement.PAN);
	private final LayerConfiguration sendConfiguration = new MixerLayerConfiguration("SEND", this,
			ParamElement.SENDMIXER);
	private final TrackLayerConfiguration sendTrackConfiguration;
	private final TrackLayerConfiguration cursorDeviceConfiguration;
	private final TrackLayerConfiguration eqTrackConfiguration;

	private final BooleanValueObject fadersTouched = new BooleanValueObject();
	private int touchCount = 0;

	private final SectionType type;
	private final ClipLaunchButtonLayer launchButtonLayer;
	private final BooleanValueObject isMenuHoldActive = new BooleanValueObject();
	private final DisplayLayer infoLayer;
	private DeviceTypeBank deviceTypeBank;
	private DisplayLayer activeDisplayLayer;

	private VPotMode activeMode = VPotMode.PAN;

	public MixControl(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
			final int sectionIndex, final SectionType type) {
		this.driver = driver;
		this.type = type;
		hwControls = new MixerSectionHardware(driver, midiIn, midiOut, sectionIndex, type);
		for (int i = 0; i < 8; i++) {
			hwControls.assignFaderTouchAction(i, touched -> handleTouch(touched));
		}
		infoLayer = new DisplayLayer("HINT_DISP_LAYER", this);

		mainGroup = new MixerLayerGroup("MN", this);
		globalGroup = new MixerLayerGroup("GL", this);
		drumGroup = new MixerLayerGroup("DR", this);
		sendConfiguration.setNavigateHorizontalHandler(direction -> {
			if (driver.getMixerMode().get() == MixerMode.DRUM) {
				drumGroup.navigateHorizontally(direction);
			} else {
				mainGroup.navigateHorizontally(direction);
				globalGroup.navigateHorizontally(direction);
			}
		});

		sendTrackConfiguration = new TrackLayerConfiguration("SN_TR", this);

		cursorDeviceConfiguration = new TrackLayerConfiguration("INSTRUMENT", this);

		eqTrackConfiguration = new TrackLayerConfiguration("EQ_DEVICE", this);

		launchButtonLayer = new ClipLaunchButtonLayer("CLIP_LAUNCH", this);

		currentConfiguration = panConfiguration;
		layerState = new LayerState(this);
		activeDisplayLayer = infoLayer;

		driver.getFlipped().addValueObserver(flipped -> layerState.updateState(this));
		driver.getMixerMode().addValueObserver(globalView -> layerState.updateState(this));
		driver.getGroupViewActive().addValueObserver(groupView -> layerState.updateState(this));
		driver.getTrackChannelMode().addValueObserver(trackMode -> doModeChange(driver.getVpotMode().getMode(), true));

		fadersTouched.addValueObserver(v -> reactToFaderTouched(v));
		if (type == SectionType.MAIN) {
			setUpModifierHandling(driver.getModifier());
		}
	}

	private void setUpModifierHandling(final ModifierValueObject modifier) {
		modifier.addValueObserver(modvalue -> {
			// TODO this will have to change to accommodate different modes
			if (modvalue > 0 && launchButtonLayer.isActive()) {
				infoLayer.setMainText("Clip mods:  Shft+Opt=delete  Shft+Alt=double content",
						"Opt=duplicate alt=stop track", false);
				infoLayer.enableFullTextMode(true);
				infoLayer.setIsActive(true);
				layerState.updateState(this);
			} else if (infoLayer.isActive()) {
				infoLayer.setIsActive(false);
				layerState.updateState(this);
			}
		});
	}

	private void reactToFaderTouched(final boolean touched) {
		if (touched) {
			layerState.updateDisplayState(getActiveDisplayLayer());
		} else {
			driver.scheduleAction("TOUCH", 1500, () -> {
				layerState.updateDisplayState(getActiveDisplayLayer());
			});
		}
	}

	public MixerLayerGroup getActiveMixGroup() {
		final MixerMode group = driver.getMixerMode().get();
		switch (group) {
		case MAIN:
			return mainGroup;
		case GLOBAL:
			return globalGroup;
		case DRUM:
			return drumGroup;
		default:
			return mainGroup;
		}
	}

	@Override
	public LayerConfiguration getCurrentConfig() {
		return currentConfiguration;
	}

	@Override
	public Layer getButtonLayer() {
		if (driver.getGroupViewActive().get()) {
			return launchButtonLayer;
		}
		return currentConfiguration.getButtonLayer();
	}

	@Override
	public DisplayLayer getActiveDisplayLayer() {
		if (infoLayer.isActive()) {
			activeDisplayLayer = infoLayer;
		} else {
			final boolean flipped = driver.getFlipped().get();
			final boolean touched = fadersTouched.get();
			final int displayer = !flipped && touched || flipped && !touched ? 1 : 0;
			activeDisplayLayer = currentConfiguration.getDisplayLayer(displayer);
		}
		return activeDisplayLayer;
	}

	public BooleanValueObject getIsMenuHoldActive() {
		return isMenuHoldActive;
	}

	public MixerSectionHardware getHwControls() {
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
		hwControls.getMainDisplay().setVuMode(mode);
	}

	public MackieMcuProExtension getDriver() {
		return driver;
	}

	public void navigateLeftRight(final int direction, final boolean isPressed) {
		if (launchButtonLayer.isActive()) {
			launchButtonLayer.navigateHorizontal(direction, isPressed);
		} else {
			if (isPressed) {
				currentConfiguration.navigateHorizontal(direction);
				if (currentConfiguration.enableInfo(InfoSource.NAV_HORIZONTAL)) {
					layerState.updateState(this);
				}
			} else {
				if (currentConfiguration.disableInfo()) {
					layerState.updateState(this);
				}
			}
		}
	}

	public void navigateUpDown(final int direction, final boolean isPressed) {
		if (launchButtonLayer.isActive()) {
			launchButtonLayer.navigateVertical(direction, isPressed);
		} else {
			if (isPressed) {
				currentConfiguration.navigateVertical(direction);
				if (currentConfiguration.enableInfo(InfoSource.NAV_VERTICAL)) {
					layerState.updateState(this);
				}
			} else {
				if (currentConfiguration.disableInfo()) {
					layerState.updateState(this);
				}
			}
		}
	}

	public void notifyModeAdvance(final VPotMode mode, final boolean pressed) {
		if (!pressed) {
			currentConfiguration.disableInfo();
			isMenuHoldActive.set(false);
		} else {
			isMenuHoldActive.set(true);

			final DeviceManager deviceTracker = currentConfiguration.getDeviceManager();

			switch (activeMode) {
			case EQ:
				if (deviceTracker != null && !deviceTracker.isSpecificDevicePresent()) {
					final InsertionPoint ip = driver.getCursorTrack().endOfDeviceChainInsertionPoint();
					ip.insertBitwigDevice(SpecialDevices.EQ_PLUS.getUuid());
				}
				break;
			case PLUGIN:
			case INSTRUMENT:
			case MIDI_EFFECT:
				if (deviceTracker != null && !deviceTracker.isSpecificDevicePresent()) {
					deviceTracker.initiateBrowsing(driver.getBrowserConfiguration(), Type.DEVICE);
				}
				break;
			default:
			}
		}
		layerState.updateState(this);
	}

	public void notifyModeChange(final VPotMode mode, final boolean down) {
		if (down) {
			doModeChange(mode, true);
		} else {
			currentConfiguration.disableInfo();
			layerState.updateState(this);
		}
	}

	void doModeChange(final VPotMode mode, final boolean focus) {
		switch (mode) {
		case EQ:
			currentConfiguration = eqTrackConfiguration;
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case MIDI_EFFECT:
			currentConfiguration = cursorDeviceConfiguration;
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case INSTRUMENT:
			currentConfiguration = cursorDeviceConfiguration;
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case PAN:
			currentConfiguration = panConfiguration;
			break;
		case PLUGIN:
			currentConfiguration = cursorDeviceConfiguration;
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
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
		activeMode = mode;
		if (currentConfiguration.getDeviceManager() != null && focus) {
			focusDevice(currentConfiguration.getDeviceManager());
		} else {
			ensureDevicePointer(currentConfiguration.getDeviceManager());
		}
		getDriver().getBrowserConfiguration().forceClose();
		layerState.updateState(this);
	}

	private void ensureDevicePointer(final DeviceManager deviceManager) {
		if (deviceManager == null) {
			return;
		}
		deviceManager.getCurrentFollower().ensurePosition();
	}

	private void focusDevice(final DeviceManager deviceManager) {
		final Device device = deviceManager.getCurrentFollower().getFocusDevice();
		getDriver().getCursorDeviceControl().selectDevice(device);
	}

	public void applyUpdate() {
		layerState.updateState(this);
	}

	public void setConfiguration(final LayerConfiguration config) {
		currentConfiguration = config;
		layerState.updateState(this);
	}

	public void notifyBlink(final int ticks) {
		launchButtonLayer.notifyBlink(ticks);
		activeDisplayLayer.triggerTimer();
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

	public LayerConfiguration getCurrentConfiguration() {
		return currentConfiguration;
	}

	public void fullHardwareUpdate() {
		hwControls.fullHardwareUpdate();
	}

	public void resetLeds() {
		hwControls.resetLeds();
	}

	public void initMainControl(final TrackBank mixerTrackBank, final TrackBank globalTrackBank,
			final DrumPadBank drumPadBank) {
		mainGroup.init(mixerTrackBank);
		globalGroup.init(globalTrackBank);
		drumGroup.init(drumPadBank);
		launchButtonLayer.initTrackBank(this.getHwControls(), mixerTrackBank);
	}

	public void initTrackControl(final CursorTrack cursorTrack, final DeviceTypeBank deviceTypeBank) {
		this.deviceTypeBank = deviceTypeBank;
		final SendBank sendBank = cursorTrack.sendBank();

		final CursorDeviceControl cursorDeviceControl = getDriver().getCursorDeviceControl();

		final DeviceManager cursorDeviceManager = deviceTypeBank.getDeviceManager(VPotMode.INSTRUMENT);
		final DeviceManager eqDevice = deviceTypeBank.getDeviceManager(VPotMode.EQ);

		for (int i = 0; i < 8; i++) {
			sendTrackConfiguration.addBinding(i, sendBank.getItemAt(i), RingDisplayType.FILL_LR);
			cursorDeviceConfiguration.addBinding(i, cursorDeviceManager.getParameter(i), RingDisplayType.FILL_LR);
			eqTrackConfiguration.addBinding(i, eqDevice.getParameterPage(i),
					(pindex, pslot) -> eqDevice.handleResetInvoked(pindex, driver.getModifier()));
		}

		cursorDeviceConfiguration.setDeviceManager(cursorDeviceManager);
		cursorDeviceConfiguration.registerFollowers(deviceTypeBank.getStandardFollowers());
		eqTrackConfiguration.setDeviceManager(eqDevice);
		eqTrackConfiguration.registerFollowers(deviceTypeBank.getFollower(VPotMode.EQ));

		sendTrackConfiguration.setNavigateHorizontalHandler(direction -> {
			if (direction < 0) {
				sendBank.scrollBackwards();
			} else {
				sendBank.scrollForwards();
			}
		});
		cursorDeviceConfiguration.setNavigateHorizontalHandler(cursorDeviceManager::navigateDeviceParameters);
		cursorDeviceConfiguration.setNavigateVerticalHandler(cursorDeviceControl::navigateDevice);

		eqTrackConfiguration.setNavigateHorizontalHandler(eqDevice::navigateDeviceParameters);
		eqTrackConfiguration.setNavigateVerticalHandler(cursorDeviceControl::navigateDevice);

		cursorTrack.name().addValueObserver(trackName -> ensureModeFocus());

		final PinnableCursorDevice cursorDevice = driver.getCursorDeviceControl().getCursorDevice();
		cursorDevice.position().addValueObserver(p -> {
			final VPotMode fittingMode = VPotMode.fittingMode(cursorDevice);
			if (p == -1) {
				// RemoteConsole.out.println("No Cursor Set = {}",
				// driver.getVpotMode().getMode());
			} else if (fittingMode != null && activeMode.isDeviceMode()
					&& !getDriver().getBrowserConfiguration().isActive()) {
				driver.getVpotMode().setMode(fittingMode);
				doModeChange(fittingMode, false);
			}
		});

		deviceTypeBank.addListenter((type, exists) -> {
			if (driver.getVpotMode().getMode() == type) {
				focusDevice(currentConfiguration.getDeviceManager());
			}
		});
	}

	private void ensureModeFocus() {

	}

	public void handleSoloAction(final Channel channel) {
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
