package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import java.util.Arrays;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.GroupButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.ArpDisplayLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.BrowserLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DeviceLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.MixerLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.PadModeDisplayLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.ScaleLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.StepEditDisplayLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.DrumPadMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.GroupLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.JogWheelDestination;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.KeyboardMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.MainKnobControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.PadMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.SceneLaunchMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.SessionMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.StepMode;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.VeloctiyHandler;
import com.bitwig.extensions.framework.Layers;

public class MaschineExtension extends ControllerExtension implements JogWheelDestination {

	private final byte[] displayBuffer = { (byte) 240, 0, 0, 102, 23, 18, 0, // 6: the grid number 0-3 * 28
			0, 0, 0, 0, 0, 0, 0, 0, // 7: 27 Chars
			0, 0, 0, 0, 0, 0, 0, 0, // 15:
			0, 0, 0, 0, 0, 0, 0, 0, // 22:
			0, 0, 0, 0, // 28:
			(byte) 247 };
	private final String[] lastSenGrids = new String[4];
	private Transport transport;
	private MaschineLayer mainLayer;
	private MaschineLayer globalShiftLayer;

	private MidiOut midiOut;
	private MidiIn midiIn;
	private HardwareSurface surface;
	private Layers layers;
	private PadButton[] padButtons;
	private GroupButton[] groupButtons;
	private TrackBank trackBank;
	private TrackBank mixerTrackBank;
	private CursorTrack cursorTrack;
	private MasterTrack masterTrack;

	private PadMode fallbackAfterMomentaryMode;
	private PadMode currentMode;

	private DisplayLayer currentDisplayMode;
	private DisplayLayer previousDisplayMode;

	private ControllerHost host;
	private MainKnobControl mainKnobControl;
	private RelativeHardwareKnob[] displayKnobs;

	private HardwareButton[] touchButtons;
	private ModeButton[] displayButtons;
	private DeviceBank deviceBank;

	private int blinkState = 0;
	private boolean shiftDown = false;
	private boolean eraseButtonDown = false;
	private Application application;
	private GroupLayer groupLayer;
	private NoteInput noteInput;

	private ModeButton noteRepeatButton;
	private ModeButton navLeftButton;
	private ModeButton navRightButton;

	private PinnableCursorDevice cursorDevice;
	private FocusClip focusClip;

	private final MaschineMode maschineMode;
	private DrumPadMode drumPadMode;
	private KeyboardMode keyboardMode;
	private PinnableCursorDevice primaryDevice;
	private LayoutType currentLayoutType;
	private final BooleanValueObject inArrangeMode = new BooleanValueObject();
	private TouchHandler touchHandler;

	private final String[] displayBackupFields = new String[4];
	private long lastTempUpdate = 0;
	private PopupBrowser browser;
	private BrowserLayer browserLayer;

	protected MaschineExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
			final MaschineMode mode) {
		super(definition, host);
		this.maschineMode = mode;
	}

	public HardwareSurface getSurface() {
		return surface;
	}

	public MidiOut getMidiOut() {
		return midiOut;
	}

	public MidiIn getMidiIn() {
		return midiIn;
	}

	public Application getApplication() {
		return application;
	}

	@Override
	public void init() {
		host = getHost();

		surface = host.createHardwareSurface();
		transport = host.createTransport();
		application = host.createApplication();

		midiOut = host.getMidiOutPort(0);
		midiIn = host.getMidiInPort(0);

		noteInput = midiIn.createNoteInput("Maschine MK3 Ctrl MIDI", "80????", "90????", "A0????");
		noteInput.setShouldConsumeEvents(false);

		padButtons = new PadButton[16];
		for (int i = 0; i < 16; i++) {
			padButtons[i] = new PadButton(i, this);
		}
		groupButtons = new GroupButton[8];
		for (int i = 0; i < 8; i++) {
			groupButtons[i] = new GroupButton(i, this);
		}

		navLeftButton = new ModeButton(this, "LEFT_NAV_BUTTON", CcAssignment.NAV_LEFT);
		navRightButton = new ModeButton(this, "RIGHT_NAV_BUTTON", CcAssignment.NAV_RIGHT);

		initCursorTrack();
		initTrackBank();
		initDisplaySection();

		createLayers();
		setUpPreferences();

		mainKnobControl = new MainKnobControl(this);

		setUpMidiSysExCommands();
		currentDisplayMode.activate();
		currentMode.activate();
		groupLayer.activate();
		mainLayer.activate();
		mainKnobControl.activate();

		host.showPopupNotification(maschineMode.getDescriptor() + " Initialized");
		host.scheduleTask(this::handlBlink, 100);
		host.scheduleTask(this::handleTempDisplay, 200);
	}

	private void setUpMidiSysExCommands() {
		midiIn.setSysexCallback(data -> {
			final SysexCommandType command = maschineMode.toCommandType(data);
			switch (command) {
			case SHIFT_DOWN:
				mainLayer.deactivate();
				globalShiftLayer.activate();
				shiftDown = true;
				currentMode.setModifierState(ModifierState.SHIFT, true);
				groupLayer.setModifierState(ModifierState.SHIFT, true);
				break;
			case SHIFT_UP:
				shiftDown = false;
				globalShiftLayer.deactivate();
				mainLayer.activate();
				currentMode.setModifierState(ModifierState.SHIFT, false);
				groupLayer.setModifierState(ModifierState.SHIFT, false);
				break;
			case RETURN_FROM_HOST:
				// RemoteConsole.out.println(" Return from Host Mode");
				surface.invalidateHardwareOutputState();
				host.requestFlush();
				break;
			case UNKNOWN:
				break;
			default:
				break;

			}
		});
	}

	public void setUpPreferences() {
		final Preferences preferences = getHost().getPreferences(); // THIS
		final SettableEnumValue focusMode = preferences.getEnumSetting("Focus", //
				"Recording/Automation",
				new String[] { FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor() },
				FocusMode.LAUNCHER.getDescriptor());
		focusMode.markInterested();
		final HardwareButton recordButton = createTransportButton("RECD", CcAssignment.RECORD);
		final HardwareButton autoButton = createTransportButton("AUTO", CcAssignment.AUTO);
		final MaschineLayer arrangeFocusLayer = new MaschineLayer(this, "ArrangeFocus");
		final MaschineLayer sessionFocusLayer = new MaschineLayer(this, "SeesionFocus");

		arrangeFocusLayer.bindToggle(recordButton, transport.isArrangerRecordEnabled());
		arrangeFocusLayer.bindToggle(autoButton, transport.isArrangerAutomationWriteEnabled());

		sessionFocusLayer.bindPressed(recordButton, () -> {
			if (shiftDown) {
				transport.isClipLauncherOverdubEnabled().toggle();
			} else {
				focusClip.invokeRecord();
			}
		});
		sessionFocusLayer.bindLightState(recordButton, transport.isClipLauncherOverdubEnabled());
		sessionFocusLayer.bindToggle(autoButton, transport.isClipLauncherAutomationWriteEnabled());

		focusMode.addValueObserver(newValue -> {
			final FocusMode newMode = FocusMode.toMode(newValue);
			switch (newMode) {
			case ARRANGER:
				sessionFocusLayer.deactivate();
				arrangeFocusLayer.activate();
				break;
			case LAUNCHER:
				arrangeFocusLayer.deactivate();
				sessionFocusLayer.activate();
				break;
			default:
				break;
			}
		});
	}

	void setUpArrangerHandling(final SessionMode sessionMode) {
		final HardwareButton arrangerButton = createTransportButton("ARRANGE", CcAssignment.ARRANGER);
		final HardwareButton followButton = createTransportButton("FOLLOW", CcAssignment.FOLLOWGRID);
		final HardwareButton saveButton = createTransportButton("SAVE", CcAssignment.SAVE);

		currentLayoutType = LayoutType.LAUNCHER;
		application.panelLayout().addValueObserver(v -> {
			currentLayoutType = LayoutType.toType(v);
			inArrangeMode.setValue(currentLayoutType == LayoutType.ARRANGER);
			sessionMode.notifyPanelLayout(currentLayoutType);
			mainKnobControl.notifyPanelLayout(currentLayoutType);
		});
		mainLayer.bindIsPressed(arrangerButton, v -> {
			application.setPanelLayout(currentLayoutType.other().getName());
		});
		mainLayer.bindLightState(arrangerButton, inArrangeMode);
		final Arranger arranger = host.createArranger();
		arranger.isClipLauncherVisible().markInterested();

		final Action followPlayHeadAction = application.getAction("toggle_playhead_follow");
		mainLayer.bindPressed(followButton, followPlayHeadAction);

		final Action saveAction = application.getAction("Save");
		final Action openAction = application.getAction("Open");
		globalShiftLayer.bindPressed(saveButton, openAction);
		mainLayer.bindPressed(saveButton, saveAction);
		globalShiftLayer.bindPressed(followButton, arranger.isClipLauncherVisible());
	}

	public void initBrowserSection() {
		browser = getHost().createPopupBrowser();
		browser.exists().markInterested();
		browserLayer = new BrowserLayer(this, "browser-display-mode");

		final HardwareButton browserButton = createTransportButton("BROWSERBUTTON", CcAssignment.BROWSER);
		mainLayer.bindPressed(browserButton, pressed -> {
			if (browser.exists().get()) {
				browser.cancel();
			} else {
				cursorDevice.afterDeviceInsertionPoint().browse();
			}
		});
		globalShiftLayer.bindPressed(browserButton, pressed -> {
			if (browser.exists().get()) {
				browser.cancel();
			} else {
				cursorDevice.replaceDeviceInsertionPoint().browse();
			}
		});
		browser.exists().addValueObserver(exists -> {
			if (exists) {
				setDisplayMode(browserLayer);
			} else {
				backToPreviousDisplayMode();
			}
		});
		mainLayer.bindLightState(browserButton, browser.exists());
		globalShiftLayer.bindLightState(browserButton, browser.exists());
	}

	void handlBlink() {
		blinkState = (blinkState + 1) % 8;
		for (int i = 0; i < padButtons.length; i++) {
			final PadButton button = padButtons[i];
			final RgbLed state = (RgbLed) button.getLight().state().currentValue();
			if (state != null && state.isBlinking()) {
				if (blinkState % 2 == 0) {
					midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getColor());
				} else {
					midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getOffColor());
				}
			}
		}
		for (int i = 0; i < groupButtons.length; i++) {
			final GroupButton button = groupButtons[i];
			final RgbLed state = (RgbLed) button.getLight().state().currentValue();
			if (state != null && state.isBlinking()) {
				if (blinkState % 2 == 0) {
					midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getColor());
				} else {
					midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getOffColor());
				}
			}
		}
		host.scheduleTask(this::handlBlink, 100);
	}

	public void backToPreviousDisplayMode() {
		if (previousDisplayMode == currentDisplayMode) {
			return;
		}
		currentDisplayMode.deactivate();
		currentDisplayMode = previousDisplayMode;
		currentDisplayMode.activate();
	}

	public void setDisplayMode(final DisplayLayer mode) {
		if (currentDisplayMode == mode) {
			return;
		}
		previousDisplayMode = currentDisplayMode;
		currentDisplayMode.deactivate();
		currentDisplayMode = mode;
		currentDisplayMode.activate();
	}

	final public void setMode(final PadMode mode) {
		if (currentMode == mode || mode == null) {
			return;
		}
		currentMode.deactivate();
		currentMode = mode;
		currentMode.activate();

		// updateKeyTranslationTable();
	}

	public DisplayLayer getCurrentDisplayMode() {
		return currentDisplayMode;
	}

	public DeviceBank getDeviceBank() {
		return deviceBank;
	}

	public ModeButton getNavLeftButton() {
		return navLeftButton;
	}

	public ModeButton getNavRightButton() {
		return navRightButton;
	}

	public NoteInput getNoteInput() {
		return noteInput;
	}

	public boolean isShiftDown() {
		return shiftDown;
	}

	public PadButton[] getPadButtons() {
		return padButtons;
	}

	public GroupButton[] getGroupButtons() {
		return groupButtons;
	}

	public TrackBank getTrackBank() {
		return trackBank;
	}

	public TrackBank getMixerTrackBank() {
		return mixerTrackBank;
	}

	public PinnableCursorDevice getCursorDevice() {
		return cursorDevice;
	}

	public PopupBrowser getBrowser() {
		return browser;
	}

	public void handleShiftAction(final int padButtonIndex) {
		switch (padButtonIndex) {
		case PadButton.UNDO:
			application.undo();
			host.showPopupNotification("Undo");
			break;
		case PadButton.REDO:
			application.redo();
			host.showPopupNotification("Redo");
			break;
		case PadButton.QUANTIZE:
			application.focusPanelBelow();
			application.selectAll();
			host.showPopupNotification("Quantize");
			focusClip.quantize(1.0);
			break;
		case PadButton.QUANTIZE_50:
			application.focusPanelBelow();
			application.selectAll();
			host.showPopupNotification("Quantize");
			focusClip.quantize(0.5);
			break;
		case PadButton.COPY:
			application.copy();
			break;
		case PadButton.PASTE:
			application.paste();
			break;
		case PadButton.CLEAR:
			focusClip.clearSteps();
			break;
		case PadButton.CLEAR_AUTO:
			break;
		case PadButton.SEMI_PLUS:
			focusClip.transpose(1);
			break;
		case PadButton.SEMI_MINUS:
			focusClip.transpose(-1);
			break;
		case PadButton.OCT_PLUS:
			focusClip.transpose(12);
			break;
		case PadButton.OCT_MINUS:
			focusClip.transpose(-12);
			break;
		default:
			break;
		}
	}

	public FocusClip getFocusClip() {
		return focusClip;
	}

	private void createLayers() {
		// We create all the layers here because the main layer might bind actions to
		// activate other layers.
		final Track rootTrack = host.getProject().getRootTrackGroup();

		layers = new Layers(this);
		mainLayer = new MaschineLayer(this, "Main");
		globalShiftLayer = new MaschineLayer(this, "Shift");

		final HardwareButton playButton = createTransportButton("PLAY", CcAssignment.PLAY);
		final HardwareButton stopButton = createTransportButton("STOP", CcAssignment.STOP);
		final HardwareButton tapButton = createTransportButton("TAP", CcAssignment.TAPMETRO);
		final HardwareButton restartButton = createTransportButton("RESTART", CcAssignment.RESTART);

		mainLayer.bindToggle(playButton, transport.isPlaying());
		mainLayer.bindPressed(stopButton, transport.stopAction());
		mainLayer.bindPressed(tapButton, transport.tapTempoAction());
		mainLayer.bindPressed(restartButton, this::invokeRestart);

		globalShiftLayer.bindPressed(playButton, this::invokeRestart);
		globalShiftLayer.bindPressed(restartButton, transport.isArrangerLoopEnabled());
		globalShiftLayer.bindPressed(tapButton, transport.isMetronomeEnabled());
		globalShiftLayer.bind(transport.isMetronomeEnabled(), tapButton);
		globalShiftLayer.bind(transport.isArrangerLoopEnabled(), restartButton);
		globalShiftLayer.bindPressed(stopButton, rootTrack.stopAction());

		groupLayer = new GroupLayer(this, "group-layer");

		focusClip = new FocusClip(this);
		final SessionMode sessionMode = new SessionMode(this, "session-mode");
		final SceneLaunchMode sceneMode = new SceneLaunchMode(this, "scene-mode");

		final MixerLayer mixerDisplayMode = new MixerLayer(this, "mixer-display-mode");
		final DeviceLayer deviceDisplayMode = new DeviceLayer(this, "device-display-mode");
		final ArpDisplayLayer arpDisplayMode = new ArpDisplayLayer(this, "arp-display-mode");

		final ModeButton patternButton = new ModeButton(this, "PATTERN_MODE", CcAssignment.PATTERN);
		final ModeButton sceneButton = new ModeButton(this, "SCENE_MODE", CcAssignment.SCENE);

		final ModeButton duplicateButton = new ModeButton(this, "DUPLICATE", CcAssignment.DUPLICATE).bindToPressed();
		final ModeButton selectButton = new ModeButton(this, "SELECT", CcAssignment.SELECT).bindToPressed();
		final ModeButton soloButton = new ModeButton(this, "SOLO", CcAssignment.SOLO).bindToPressed();
		final ModeButton muteButton = new ModeButton(this, "MUTE", CcAssignment.MUTE).bindToPressed();
		final ModeButton eraseButton = new ModeButton(this, "ERASE", CcAssignment.ERASE).bindToPressed();

		final ModeButton mixerButton = new ModeButton(this, "MIXER", CcAssignment.MIXER);
		final ModeButton deviceButton = new ModeButton(this, "DEVICE", CcAssignment.PLUGIN);

		final ModeButton macroButton = new ModeButton(this, "MACRO", CcAssignment.MACRO);

		macroButton.getHwButton().isPressed().addValueObserver(active -> {
			currentDisplayMode.notifyMacroDown(active);
		});
		mainLayer.bindLightState(macroButton, macroButton.getHwButton().isPressed());

		duplicateButton.getHwButton().isPressed().addValueObserver(active -> {
			if (shiftDown && active) {
				focusClip.duplicateContent();
			} else if (!shiftDown) {
				setModifierState(ModifierState.DUPLICATE, active);
			}
		});
		selectButton.getHwButton().isPressed().addValueObserver(active -> setSelectModifierState(active));
		soloButton.getHwButton().isPressed().addValueObserver(active -> setModifierState(ModifierState.SOLO, active));
		muteButton.getHwButton().isPressed().addValueObserver(active -> setModifierState(ModifierState.MUTE, active));
		eraseButton.getHwButton().isPressed().addValueObserver(active -> {
			eraseButtonDown = active;
			setModifierState(ModifierState.ERASE, active);
		});

		mainLayer.bindMode(patternButton, sessionMode);
		mainLayer.bindMode(sceneButton, sceneMode);

		setStepAndPlayingLayers();

		mainLayer.bindMode(deviceButton, deviceDisplayMode, false);
		mainLayer.bindMode(mixerButton, mixerDisplayMode, false);

		final HardwareButton touch4DButton = createTouchButton(midiIn, "NAV4D_TOUCH",
				CcAssignment.DKNOB_TOUCH.getCcNr());
		touch4DButton.isPressed().addValueObserver(touched -> {
			currentDisplayMode.notifyMainTouched(touched);
			mainKnobControl.setDisplayActive(touched);
		});

		currentMode = sessionMode;
		currentDisplayMode = mixerDisplayMode;
		previousDisplayMode = mixerDisplayMode;

		initNoteRepeat(arpDisplayMode);
		setUpArrangerHandling(sessionMode);
		initBrowserSection();
	}

	public HardwareButton create4DSelectButton(final String name, final CcAssignment assignment) {
		final HardwareButton navdownButton = surface.createHardwareButton(name);
		navdownButton.pressedAction().setActionMatcher(assignment.createActionMatcher(midiIn, 127));
		navdownButton.releasedAction().setActionMatcher(assignment.createActionMatcher(midiIn, 0));
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(name + "_LED");
		navdownButton.setBackgroundLight(led);
		return navdownButton;
	}

	private void initNoteRepeat(final ArpDisplayLayer arpDisplayMode) {
		noteRepeatButton = new ModeButton(this, "NOTE_REPEAT", CcAssignment.NOTE_REPEAT);
		mainLayer.bindModeMomentary(noteRepeatButton, arpDisplayMode, false);
		noteInput.arpeggiator().isEnabled().markInterested();
		noteRepeatButton.getHwButton().isPressed().addValueObserver(pressed -> {
			if (!primaryDevice.exists().get()) {
				return;
			}
			noteInput.arpeggiator().isEnabled().set(pressed);
			if (pressed) {
				fallbackAfterMomentaryMode = currentMode;
				if (primaryDevice.hasDrumPads().get()) {
					setMode(drumPadMode);
				} else {
					setMode(keyboardMode);
				}
			} else {
				if (fallbackAfterMomentaryMode != null && fallbackAfterMomentaryMode != currentMode) {
					setMode(fallbackAfterMomentaryMode);
					fallbackAfterMomentaryMode = null;
				}
			}
		});
		noteRepeatButton.getLed().isOn().setValueSupplier(() -> noteInput.arpeggiator().isEnabled().get());
	}

	private void setStepAndPlayingLayers() {
		final VeloctiyHandler velocityHandler = new VeloctiyHandler();
		final ScaleLayer scaleDisplayMode = new ScaleLayer(this, "scale-display-mode", velocityHandler);
		final PadModeDisplayLayer padDisplayLayer = new PadModeDisplayLayer(this, "pad-display-mode", velocityHandler);
		final StepEditDisplayLayer stepDisplayLayer = new StepEditDisplayLayer(this, "step-display-mode");

		final ModeButton padModeButton = new ModeButton(this, "PAD_MODE", CcAssignment.PADMODE);
		final ModeButton keyboardButton = new ModeButton(this, "KEYBOARD_MODE", CcAssignment.KEYBOARD);
		final ModeButton stepButton = new ModeButton(this, "STEP_MODE", CcAssignment.STEP);

		final StepMode stepMode = new StepMode(this, "step-mode");
		drumPadMode = new DrumPadMode(this, "pad-mode", stepMode, velocityHandler, padDisplayLayer);
		keyboardMode = new KeyboardMode(this, "keyboard-mode", stepMode, velocityHandler, scaleDisplayMode);

		drumPadMode.setAltMode(keyboardMode);
		keyboardMode.setAltMode(drumPadMode);

		stepDisplayLayer.setStepLayer(stepMode);
		stepMode.setSelectLayers(drumPadMode, keyboardMode);

		final ModeButton fixedVelButton = new ModeButton(this, "FIXED_VELOCITY", CcAssignment.FIXEDVEL);

		mainLayer.bindPressed(fixedVelButton, () -> velocityHandler.toggleFixedValue(noteInput));
		mainLayer.bindLightState(fixedVelButton, velocityHandler.getFixed());

		mainLayer.bindMode(padModeButton, drumPadMode);
		mainLayer.bindMode(keyboardButton, keyboardMode);
		mainLayer.bindMode(stepButton, stepMode);

		mainLayer.bindMode(keyboardButton, scaleDisplayMode, true); // Decoupled light
		mainLayer.bindMode(padModeButton, padDisplayLayer, true); // Decoupled light
		mainLayer.bindMode(stepButton, stepDisplayLayer, false);

	}

	private HardwareButton createTransportButton(final String name, final CcAssignment assignment) {
		final HardwareButton playButton = surface.createHardwareButton(name + "_BUTTON");
		final HardwareAction action = playButton.pressedAction();
		action.setActionMatcher(assignment.createActionMatcher(midiIn, 127));
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(name + "BUTTON_LED");
		playButton.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(assignment, led.isOn().currentValue() ? 127 : 0);
		});
		return playButton;
	}

	private void setModifierState(final ModifierState modstate, final boolean active) {
		currentMode.setModifierState(modstate, active);
		groupLayer.setModifierState(modstate, active);
	}

	private void setSelectModifierState(final boolean active) {
		if (currentMode.hasMomentarySelectMode() || fallbackAfterMomentaryMode != null) {
			if (active) {
				fallbackAfterMomentaryMode = currentMode;
				setMode(currentMode.getMomentarySwitchMode());
				currentMode.setModifierState(ModifierState.SELECT, true);
			} else {
				currentMode.setModifierState(ModifierState.SELECT, false);
				setMode(fallbackAfterMomentaryMode);
				fallbackAfterMomentaryMode = null;
			}
		} else {
			currentMode.setModifierState(ModifierState.SELECT, active);
		}
		groupLayer.setModifierState(ModifierState.SELECT, active);
	}

	public ModeButton getNoteRepeatButton() {
		return noteRepeatButton;
	}

	public CursorTrack getCursorTrack() {
		return cursorTrack;
	}

	public RelativeHardwareKnob[] getDisplayKnobs() {
		return displayKnobs;
	}

	public ModeButton[] getDisplayButtons() {
		return displayButtons;
	}

	public HardwareButton[] getTouchButtons() {
		return touchButtons;
	}

	public TouchHandler getTouchHandler() {
		return touchHandler;
	}

	private void initDisplaySection() {
		for (int i = 0; i < lastSenGrids.length; i++) {
			lastSenGrids[i] = "----------------------";
		}

		displayKnobs = new RelativeHardwareKnob[8];
		displayButtons = new ModeButton[8];
		touchButtons = new HardwareButton[8];

		final CcAssignment[] buttonAssingments = { CcAssignment.MODE_BUTTON_1, CcAssignment.MODE_BUTTON_2,
				CcAssignment.MODE_BUTTON_3, CcAssignment.MODE_BUTTON_4, CcAssignment.MODE_BUTTON_5,
				CcAssignment.MODE_BUTTON_6, CcAssignment.MODE_BUTTON_7, CcAssignment.MODE_BUTTON_8 };

		for (int i = 0; i < 8; i++) {
			final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("ENDLESKNOB_" + i);
			displayKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0, 70 + i, 128));
			knob.setStepSize(1 / 64.0);
		}

		touchHandler = new TouchHandler();
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final HardwareButton touchButton = createTouchButton(midiIn, "DISPLAY_TOUCH_" + i, 10 + i);
			touchButtons[i] = touchButton;
			touchButton.isPressed().addValueObserver(v -> {
				touchHandler.notifyTouched(index, v);
				currentDisplayMode.notifyTouched(index, v);
			});
		}

		for (int i = 0; i < displayButtons.length; i++) {
			displayButtons[i] = new ModeButton(this, "DISPLAY_BUTTON_" + i, buttonAssingments[i]);
		}
	}

	private void initCursorTrack() {
		cursorTrack = getHost().createCursorTrack(8, 8);
		cursorTrack.color().markInterested();
		cursorTrack.hasPrevious().markInterested();
		cursorTrack.hasNext().markInterested();
		cursorTrack.playingNotes().markInterested();
		cursorTrack.arm().markInterested();
		cursorTrack.exists().markInterested();
		cursorTrack.mute().markInterested();
		cursorTrack.solo().markInterested();
		cursorTrack.arm().markInterested();
		cursorTrack.volume().value().markInterested();
		cursorTrack.pan().value().markInterested();
		cursorTrack.color().markInterested();
		cursorTrack.isStopped().markInterested();
		cursorTrack.isQueuedForStop().markInterested();
		cursorTrack.isActivated().markInterested();

		masterTrack = getHost().createMasterTrack(8);
		masterTrack.volume().markInterested();

		deviceBank = cursorTrack.createDeviceBank(4);
		cursorDevice = cursorTrack.createCursorDevice();
		primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 0,
				CursorDeviceFollowMode.FIRST_INSTRUMENT);
		primaryDevice.hasDrumPads().markInterested();
		primaryDevice.exists().markInterested();
	}

	private void initTrackBank() {
		// mixerTrackBank = getHost().createTrackBank(8, 1, 0);
		mixerTrackBank = getHost().createMainTrackBank(8, 1, 1);
		mixerTrackBank.cursorIndex().markInterested();
		mixerTrackBank.setSkipDisabledItems(true);
		mixerTrackBank.canScrollChannelsDown().markInterested();
		mixerTrackBank.canScrollChannelsUp().markInterested();

		trackBank = getHost().createMainTrackBank(4, 4, 4);
		// trackBank.followCursorTrack(cursorTrack);
		trackBank.cursorIndex().markInterested();
		trackBank.setSkipDisabledItems(true);
		trackBank.canScrollChannelsDown().markInterested();
		trackBank.canScrollChannelsUp().markInterested();

		for (int i = 0; i < 8; i++) {
			final Track channel = mixerTrackBank.getItemAt(i);
			channel.exists().markInterested();
			channel.mute().markInterested();
			channel.solo().markInterested();
			channel.arm().markInterested();
			channel.volume().value().markInterested();
			channel.pan().value().markInterested();
			channel.color().markInterested();
			channel.isStopped().markInterested();
			channel.isQueuedForStop().markInterested();
			channel.isActivated().markInterested();
			channel.canHoldAudioData().markInterested();
			channel.canHoldNoteData().markInterested();
		}

		for (int i = 0; i < 4; ++i) {
			final Track channel = trackBank.getItemAt(i);
			channel.exists().markInterested();
			channel.mute().markInterested();
			channel.solo().markInterested();
			channel.arm().markInterested();
			channel.volume().value().markInterested();
			channel.pan().value().markInterested();
			channel.color().markInterested();
			channel.isStopped().markInterested();
			channel.isQueuedForStop().markInterested();

			final ClipLauncherSlotBank clipLauncherSlots = channel.clipLauncherSlotBank();
			clipLauncherSlots.setIndication(false);

			final SendBank sendBank = channel.sendBank();
			sendBank.setSkipDisabledItems(true);

			for (int j = 0; j < 4; ++j) {
				final ClipLauncherSlot slot = clipLauncherSlots.getItemAt(j);
				slot.color().markInterested();
				slot.isPlaybackQueued().markInterested();
				slot.hasContent().markInterested();
				slot.isRecording().markInterested();
				slot.isPlaying().markInterested();
				slot.isRecordingQueued().markInterested();
				slot.isStopQueued().markInterested();
				slot.isSelected().markInterested();

				final Send send = sendBank.getItemAt(j);
				send.value().markInterested();
				send.sendChannelColor().markInterested();
				send.exists().markInterested();
			}
		}
	}

	private HardwareButton createTouchButton(final MidiIn midiIn, final String name, final int ccNr) {
		final HardwareButton button = surface.createHardwareButton(name);
		button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 127));
		button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 0));
		return button;
	}

	public void sendLedUpdate(final CcAssignment assingment, final int value) {
		midiOut.sendMidi(assingment.getType(), assingment.getCcNr(), value);
	}

	public void updatePadLed(final RgbButton button) {
		final RgbLed state = (RgbLed) button.getLight().state().currentValue();
		if (state != null) {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getColor());
		} else {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		}
	}

	public void handleTempDisplay() {
		if (lastTempUpdate > 0 && System.currentTimeMillis() - lastTempUpdate > 1000) {
			this.sendToDisplay(0, displayBackupFields[0]);
			lastTempUpdate = 0;
		}
		this.host.scheduleTask(this::handleTempDisplay, 300);
	}

	public void sendToDisplayTemp(final int grid, final String text) {
		lastTempUpdate = System.currentTimeMillis();
		sendToDisplay(grid, text);
	}

	public void sendToDisplayBuffered(final int grid, final String text) {
		displayBackupFields[grid] = text;
		sendToDisplay(grid, text);
		lastTempUpdate = 0;
	}

	public void sendToDisplay(final int grid, final String text) {
		if (text.equals(lastSenGrids[grid])) {
			return;
		}
		lastSenGrids[grid] = text;
		displayBuffer[6] = (byte) (Math.min(grid, 3) * 28);
		final char[] ca = text.toCharArray();
		for (int i = 0; i < 28; i++) {
			displayBuffer[i + 7] = i < ca.length ? (byte) ca[i] : 32;
		}
		midiOut.sendSysex(displayBuffer);
	}

	@Override
	public void exit() {
		for (int i = 0; i < 4; i++) {
			sendToDisplay(i, "");
		}
		Arrays.stream(CcAssignment.values()).forEach(assignment -> {
			sendLedUpdate(assignment, 0);
		});
		for (final PadButton button : padButtons) {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		}
		for (final GroupButton button : groupButtons) {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		}
		getHost().showPopupNotification(maschineMode.getDescriptor() + " Exited");
		surface.updateHardware();
		// For the shutdown process to clear the display, we currently need
		// to call sleep() with 400ms, which is quite disruptive.
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	public Layers getLayers() {
		return layers;
	}

	public MasterTrack getMasterTrack() {
		return masterTrack;
	}

	public Transport getTranport() {
		return transport;
	}

	public PinnableCursorDevice getPrimaryDevice() {
		return primaryDevice;
	}

	private void invokeRestart() {
		transport.stop();
		getHost().scheduleTask(() -> {
			transport.stop();
		}, 10);
		getHost().scheduleTask(() -> {
			transport.play();
		}, 20);
	}

	public boolean isEraseDown() {
		return eraseButtonDown;
	}

	public JogWheelDestination getJogWheelDestination() {
		if (browser.exists().get()) {
			return browserLayer;
		}
		if (currentMode instanceof JogWheelDestination) {
			return (JogWheelDestination) currentMode;
		}
		return this;
	}

	@Override
	public void jogWheelPush(final boolean push) {
	}

	@Override
	public void jogWheelAction(final int increment) {
		if (increment > 0) {
			transport.fastForward();
		} else {
			transport.rewind();
		}
	}

}
