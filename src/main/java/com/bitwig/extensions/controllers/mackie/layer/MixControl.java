package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.CursorDeviceLayer;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.ButtonViewState;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.MixerMode;
import com.bitwig.extensions.controllers.mackie.NoteHandler;
import com.bitwig.extensions.controllers.mackie.VPotMode;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.configurations.GlovalViewLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.MixerLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.TrackLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mackie.devices.SpecialDevices;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;

public class MixControl implements LayerStateHandler {
	private final MixerSectionHardware hwControls;
	final MackieMcuProExtension driver;

	final MixerLayerGroup mainGroup;
	final MixerLayerGroup globalGroup;
	final MixerLayerGroup drumGroup;

	protected final LayerState layerState;

	protected LayerConfiguration currentConfiguration;

	protected final LayerConfiguration panConfiguration = new MixerLayerConfiguration("PAN", this, ParamElement.PAN);
	private final LayerConfiguration sendConfiguration = new MixerLayerConfiguration("SEND", this,
			ParamElement.SENDMIXER);
	private final TrackLayerConfiguration sendTrackConfiguration;
	private final TrackLayerConfiguration sendDrumTrackConfiguration;
	private final TrackLayerConfiguration cursorDeviceConfiguration;
	private final TrackLayerConfiguration eqTrackConfiguration;
	private final GlovalViewLayerConfiguration globalViewLayerConfiguration;

	private final BooleanValueObject fadersTouched = new BooleanValueObject();
	private int touchCount = 0;

	private final SectionType type;
	private final ClipLaunchButtonLayer launchButtonLayer;
	private final BooleanValueObject isMenuHoldActive = new BooleanValueObject();
	private final DisplayLayer infoLayer;
	private DeviceTypeBank deviceTypeBank;
	private DisplayLayer activeDisplayLayer;

	protected VPotMode activeMode = VPotMode.PAN;
	private NoteHandler noteHandler;

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

		sendDrumTrackConfiguration = new TrackLayerConfiguration("SEND_DRUM_TRACK", this);

		cursorDeviceConfiguration = new TrackLayerConfiguration("INSTRUMENT", this);

		eqTrackConfiguration = new TrackLayerConfiguration("EQ_DEVICE", this);

		globalViewLayerConfiguration = new GlovalViewLayerConfiguration("GLOBAL_VIEW", this);

		launchButtonLayer = new ClipLaunchButtonLayer("CLIP_LAUNCH", this);

		currentConfiguration = panConfiguration;
		panConfiguration.setActive(true);

		layerState = new LayerState(this);
		activeDisplayLayer = infoLayer;

		driver.getFlipped().addValueObserver(flipped -> layerState.updateState(this));
		driver.getMixerMode().addValueObserver(globalView -> changeMixerMode());
		driver.getButtonView().addValueObserver(this::handleButtonViewChanged);
		driver.getTrackChannelMode().addValueObserver(trackMode -> doModeChange(driver.getVpotMode().getMode(), true));

		fadersTouched.addValueObserver(v -> handleFadersTouched(v));
		if (type == SectionType.MAIN) {
			setUpModifierHandling(driver.getModifier());
		}
	}

	private void handleButtonViewChanged(final ButtonViewState newState) {
		if (newState == ButtonViewState.GLOBAL_VIEW) {
			switchActiveConfiguration(globalViewLayerConfiguration);
			layerState.updateState(this);
		} else {
			doModeChange(activeMode, true);
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

	private void handleFadersTouched(final boolean touched) {
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
		switch (driver.getButtonView().get()) {
		case MIXER:
		case GLOBAL_VIEW:
			return currentConfiguration.getButtonLayer();
		case GROUP_LAUNCH:
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

	public boolean isFlipped() {
		return driver.getFlipped().get();
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

	private void changeMixerMode() {
		determineSendTrackConfig(activeMode);
		layerState.updateState(this);
	}

	void doModeChange(final VPotMode mode, final boolean focus) {
		switch (mode) {
		case EQ:
			switchActiveConfiguration(eqTrackConfiguration);
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case MIDI_EFFECT:
			switchActiveConfiguration(cursorDeviceConfiguration);
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case INSTRUMENT:
			switchActiveConfiguration(cursorDeviceConfiguration);
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case PAN:
			switchActiveConfiguration(panConfiguration);
			break;
		case PLUGIN:
			switchActiveConfiguration(cursorDeviceConfiguration);
			currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
			break;
		case SEND:
			determineSendTrackConfig(VPotMode.SEND);
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

	protected void determineSendTrackConfig(final VPotMode mode) {
		if (mode != VPotMode.SEND) {
			return;
		}
		if (type != SectionType.MAIN) {
			switchActiveConfiguration(sendConfiguration);
		} else if (driver.getTrackChannelMode().get()) {
			if (driver.getMixerMode().get() == MixerMode.DRUM) {
				switchActiveConfiguration(sendDrumTrackConfiguration);
			} else {
				switchActiveConfiguration(sendTrackConfiguration);
			}
		} else {
			switchActiveConfiguration(sendConfiguration);
		}
	}

	public void setConfiguration(final LayerConfiguration config) {
		switchActiveConfiguration(config);
		layerState.updateState(this);
	}

	protected void switchActiveConfiguration(final LayerConfiguration nextConfiguration) {
		if (nextConfiguration == currentConfiguration) {
			return;
		}
		currentConfiguration.setActive(false);
		currentConfiguration = nextConfiguration;
		currentConfiguration.setActive(true);
	}

	private void ensureDevicePointer(final DeviceManager deviceManager) {
		if (deviceManager == null) {
			return;
		}
		deviceManager.getCurrentFollower().ensurePosition();
	}

	private void focusDevice(final DeviceManager deviceManager) {
		if (driver.getMixerMode().get() == MixerMode.DRUM) {
			getDriver().getCursorDeviceControl().focusOnDrumDevice();
		} else {
			final Device device = deviceManager.getCurrentFollower().getFocusDevice();
			getDriver().getCursorDeviceControl().selectDevice(device);
		}
	}

	public void applyUpdate() {
		layerState.updateState(this);
	}

	public void notifyBlink(final int ticks) {
		launchButtonLayer.notifyBlink(ticks);
		globalViewLayerConfiguration.notifyBlink(ticks);
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

	public void handleNameDisplay(final boolean pressed) {
		final MixerLayerGroup activeMixerGroup = getActiveMixGroup();
		if (activeMixerGroup.notifyDisplayName(pressed)) {
			layerState.updateState(this);
		} else if (currentConfiguration.notifyDisplayName(pressed)) {
			layerState.updateState(this);
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
		noteHandler = new NoteHandler(this.getHwControls().getMidiIn(), drumPadBank, getDriver().getCursorTrack());
		mainGroup.init(mixerTrackBank);
		globalGroup.init(globalTrackBank);
		drumGroup.init(drumPadBank, noteHandler);
		launchButtonLayer.initTrackBank(this.getHwControls(), mixerTrackBank);
		globalViewLayerConfiguration.init(mixerTrackBank, globalTrackBank);

	}

	public NoteHandler getNoteHandler() {
		return noteHandler;
	}

	public void initTrackControl(final CursorTrack cursorTrack, final CursorDeviceLayer drumCursor,
			final DeviceTypeBank deviceTypeBank) {
		this.deviceTypeBank = deviceTypeBank;

		final SendBank sendBank = cursorTrack.sendBank();
		final SendBank drumSendBank = drumCursor.sendBank();

		final CursorDeviceControl cursorDeviceControl = getDriver().getCursorDeviceControl();

		final DeviceManager cursorDeviceManager = deviceTypeBank.getDeviceManager(VPotMode.INSTRUMENT);
		final DeviceManager eqDevice = deviceTypeBank.getDeviceManager(VPotMode.EQ);

		for (int i = 0; i < 8; i++) {
			sendTrackConfiguration.addBinding(i, sendBank.getItemAt(i), RingDisplayType.FILL_LR);
			sendDrumTrackConfiguration.addBinding(i, drumSendBank.getItemAt(i), RingDisplayType.FILL_LR);
			cursorDeviceConfiguration.addBinding(i, cursorDeviceManager.getParameter(i), RingDisplayType.FILL_LR);
			eqTrackConfiguration.addBinding(i, eqDevice.getParameterPage(i),
					(pindex, pslot) -> eqDevice.handleResetInvoked(pindex, driver.getModifier()));
		}

		cursorDeviceConfiguration.setDeviceManager(cursorDeviceManager);
		cursorDeviceConfiguration.registerFollowers(deviceTypeBank.getStandardFollowers());
		eqTrackConfiguration.setDeviceManager(eqDevice);
		eqTrackConfiguration.registerFollowers(deviceTypeBank.getFollower(VPotMode.EQ));

		sendTrackConfiguration.setNavigateHorizontalHandler(direction -> handleSendNavigation(sendBank, direction));
		sendDrumTrackConfiguration
				.setNavigateHorizontalHandler(direction -> handleSendNavigation(drumSendBank, direction));

		cursorDeviceConfiguration.setNavigateHorizontalHandler(cursorDeviceManager::navigateDeviceParameters);
		cursorDeviceConfiguration.setNavigateVerticalHandler(
				direction -> cursorDeviceControl.navigateDevice(direction, driver.getModifier()));

		eqTrackConfiguration.setNavigateHorizontalHandler(eqDevice::navigateDeviceParameters);
		eqTrackConfiguration.setNavigateVerticalHandler(
				direction -> cursorDeviceControl.navigateDevice(direction, driver.getModifier()));

		cursorTrack.name().addValueObserver(trackName -> ensureModeFocus());
		setUpDeviceModeHandling(deviceTypeBank);
	}

	private void setUpDeviceModeHandling(final DeviceTypeBank deviceTypeBank) {
		final BrowserConfiguration browserConfiguration = getDriver().getBrowserConfiguration();

		final PinnableCursorDevice cursorDevice = driver.getCursorDeviceControl().getCursorDevice();
		cursorDevice.position().addValueObserver(p -> updateDeviceMode(p, browserConfiguration, cursorDevice));
		cursorDevice.isNested().addValueObserver(
				nested -> updateDeviceMode(cursorDevice.position().get(), browserConfiguration, cursorDevice));

		deviceTypeBank.addListenter((type, exists) -> {
			if (driver.getVpotMode().getMode() == type) {
				focusDevice(currentConfiguration.getDeviceManager());
			}
		});
	}

	private void updateDeviceMode(final int p, final BrowserConfiguration browserConfiguration,
			final PinnableCursorDevice cursorDevice) {
		final VPotMode fittingMode = VPotMode.fittingMode(cursorDevice);
		if (p >= 0 && fittingMode != null && activeMode.isDeviceMode()) {
			if (!browserConfiguration.isMcuBrowserActive()) {
				driver.getVpotMode().setMode(fittingMode);
				doModeChange(fittingMode, false);
			}
		}
	}

	private void handleSendNavigation(final SendBank sendBank, final int direction) {
		if (direction < 0) {
			sendBank.scrollBackwards();
		} else {
			sendBank.scrollForwards();
		}
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

	void handleTrackSelection(final Track track) {
		if (track.exists().get()) {
			if (driver.getModifier().isShift()) {
				track.isGroupExpanded().toggle();
			} else if (driver.getModifier().isControl()) {
				track.deleteObject();
			} else if (driver.getModifier().isAlt()) {
				track.stop();
			} else if (driver.getModifier().isOption()) {
				driver.getApplication().navigateIntoTrackGroup(track);
			} else {
				track.selectInMixer();
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

	public void handlePadSelection(final DrumPad pad) {
		pad.selectInEditor();
		pad.selectInMixer();
		if (activeMode == VPotMode.INSTRUMENT) {
			driver.getCursorDeviceControl().focusOnDrumDevice();
		}
	}

}
