package com.bitwig.extensions.controllers.mackie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mackie.devices.SpecialDevices;
import com.bitwig.extensions.controllers.mackie.display.MotorFader;
import com.bitwig.extensions.controllers.mackie.display.TimeCodeLed;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.ExtenderMixControl;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.SectionType;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.LayoutType;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.controllers.mackie.value.TrackModeValue;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MackieMcuProExtension extends ControllerExtension {

	private static final String MAIN_UNIT_SYEX_HEADER = "F0 00 00 66 14 ";
	private static final String SYSEX_DEVICE_RELOAD = "f0000066140158595a";
	private static final double[] FFWD_SPEEDS = { 0.0625, 0.25, 1.0, 4.0 };
	private static final double[] FFWD_SPEEDS_SHIFT = { 0.25, 1.0, 4.0, 16.0 };
	private static final long[] FFWD_TIMES = { 500, 1000, 2000, 3000, 4000 };

	private Layers layers;
	private Layer mainLayer;
	private Layer shiftLayer;

	private HardwareSurface surface;
	private Transport transport;
	private Application application;
	private Project project;
	private MidiOut midiOut;
	private MidiIn midiIn;
	private CursorTrack cursorTrack;
	private TrackBank mixerTrackBank;
	private TrackBank globalTrackBank;
	private ControllerHost host;
	private TimeCodeLed ledDisplay;
	private MasterTrack masterTrack;
	private final BooleanValueObject flipped = new BooleanValueObject();
	private final BooleanValueObject zoomActive = new BooleanValueObject();
	// private final BooleanValueObject scrubActive = new BooleanValueObject();

	private final ValueObject<MixerMode> mixerMode = new ValueObject<>(MixerMode.MAIN);
	private MixerMode previousOverallMode = MixerMode.MAIN;

	private final ValueObject<ButtonViewState> buttonViewMode = new ValueObject<>(ButtonViewState.MIXER);
	private int blinkTicks = 0;
	private final BooleanValueObject transportClick = new BooleanValueObject();

	private final ModifierValueObject modifier = new ModifierValueObject();
	private final TrackModeValue trackChannelMode = new TrackModeValue();

	private VuMode vuMode = VuMode.LED;
	private final int nrOfExtenders;
	private DelayAction delayedAction = null; // TODO this needs to be a queue

	private final HoldMenuButtonState holdAction = new HoldMenuButtonState();
	private final int[] lightStatusMap = new int[127];

	private LayoutType currentLayoutType;

	private MixControl mainSection;
	private final List<MixControl> sections = new ArrayList<>();

	private final HoldCapture holdState = new HoldCapture();
	private Arranger arranger;
	private PopupBrowser browser;
	private BrowserConfiguration browserConfiguration;

	private CursorDeviceControl cursorDeviceControl;
	private HardwareButton enterButton;
	private HardwareButton cancelButton;
	private DeviceMatcher drumMatcher;
	private DrumNoteHandler noteHandler;
	private MotorFader masterFaderResponse;

	protected MackieMcuProExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
			final int extenders) {
		super(definition, host);
		this.nrOfExtenders = extenders;
	}

	@Override
	public void init() {
		host = getHost();
		surface = host.createHardwareSurface();
		transport = host.createTransport();
		application = host.createApplication();
		arranger = host.createArranger();
		project = host.getProject();
		layers = new Layers(this);
		mainLayer = new Layer(layers, "MainLayer");
		shiftLayer = new Layer(layers, "GlobalShiftLayer");

		for (int i = 0; i < lightStatusMap.length; i++) {
			lightStatusMap[i] = -1;
		}

		midiOut = host.getMidiOutPort(0);
		midiIn = host.getMidiInPort(0);
		ledDisplay = new TimeCodeLed(midiOut);
		drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());

		enterButton = createPressButton(NoteOnAssignment.ENTER);
		cancelButton = createPressButton(NoteOnAssignment.CANCEL);

		browser = host.createPopupBrowser();

		initJogWheel();
		initMasterSection();
		initChannelSections();
		browserConfiguration = new BrowserConfiguration("BROWSER", mainSection, host, browser);
		intiVPotModes();

		initTransport();
		initTrackBank(4);
		initModifiers();

		initCursorSection();

		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));

		setUpMidiSysExCommands();
		mainLayer.activate();
		host.showPopupNotification(" Initialized Mackie MCU Pro");
		sections.forEach(MixControl::resetFaders);
		sections.forEach(MixControl::clearAll);
		ledDisplay.refreschMode();
		host.scheduleTask(this::handlePing, 100);
//		final Action[] as = application.getActions();
//		for (final Action action : as) {
//			RemoteConsole.out.println("ACTION > [{}]", action.getId());
//		}
	}

	public HardwareButton getEnterButton() {
		return enterButton;
	}

	public HardwareButton getCancelButton() {
		return cancelButton;
	}

	public void initChannelSections() {
		mainSection = new MixControl(this, midiIn, midiOut, nrOfExtenders, SectionType.MAIN);
		sections.add(mainSection);
		for (int i = 0; i < nrOfExtenders; i++) {
			final MidiOut extMidiOut = host.getMidiOutPort(i + 1);
			final MidiIn extMidiIn = host.getMidiInPort(i + 1);
			if (extMidiIn != null && extMidiOut != null) {
				final MixControl extenderSection = new ExtenderMixControl(this, extMidiIn, extMidiOut, i);
				sections.add(extenderSection);
			} else {
				// RemoteConsole.out.println(" CREATE Extender Section {} failed due to missing
				// ports", i + 1);
			}
		}
	}

	public void doActionImmediate(final String actionId) {
		if (delayedAction != null && actionId.equals(delayedAction.getActionId())) {
			delayedAction.run();
			delayedAction = null;
		}
	}

	public void cancelAction(final String actionId) {
		if (delayedAction != null && actionId.equals(delayedAction.getActionId())) {
			delayedAction = null;
		}
	}

	public void scheduleAction(final String actionId, final int duration, final Runnable action) {
		delayedAction = new DelayAction(duration, actionId, action);
	}

	private void handlePing() {
		if (delayedAction != null && delayedAction.isReady()) {
			delayedAction.run();
			delayedAction = null;
		}
		if (holdAction.isRunning()) {
			holdAction.execute();
		}
		sections.forEach(section -> section.notifyBlink(blinkTicks));
		blinkTicks++;
		host.scheduleTask(this::handlePing, 100);
	}

	private void initJogWheel() {
		final RelativeHardwareKnob fourDKnob = surface.createRelativeHardwareKnob("JOG_WHEEL");
		fourDKnob.setAdjustValueMatcher(midiIn.createRelativeSignedBitCCValueMatcher(0, 60, 128));
		fourDKnob.setStepSize(1 / 128.0);

		final HardwareActionBindable incAction = host.createAction(() -> jogWheelPlayPosition(1), () -> "+");
		final HardwareActionBindable decAction = host.createAction(() -> jogWheelPlayPosition(-1), () -> "-");
		mainLayer.bind(fourDKnob, host.createRelativeHardwareControlStepTarget(incAction, decAction));
	}

	private void jogWheelPlayPosition(final int dir) {
		double resolution = 0.25;
		if (modifier.isAltSet()) {
			resolution = 4.0;
		} else if (modifier.isShiftSet()) {
			resolution = 1.0;
		}
		changePlayPosition(dir, resolution, !modifier.isOptionSet(), !modifier.isControlSet());
	}

	private void changePlayPosition(final int inc, final double resolution, final boolean restrictToStart,
			final boolean quantize) {

		final double position = transport.playStartPosition().get();
		double newPos = position + resolution * inc;

		if (restrictToStart && newPos < 0) {
			newPos = 0;
		}

		if (position != newPos) {
			if (quantize) {
				final double intup = Math.floor(newPos / resolution);
				newPos = intup * resolution;
			}
			transport.playStartPosition().set(newPos);
			if (transport.isPlaying().get()) {
				transport.jumpToPlayStartPosition();
			}
		}
	}

	public VuMode getVuMode() {
		return vuMode;
	}

	public DeviceMatcher getDrumMatcher() {
		return drumMatcher;
	}

	private void initMasterSection() {
		masterTrack = getHost().createMasterTrack(8);
		masterTrack.volume().markInterested();
		final AbsoluteHardwareKnob masterFader = surface.createAbsoluteHardwareKnob("MASTER_FADER_");
		masterFader.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(8));
		masterFader.addBinding(masterTrack.volume());
		masterFaderResponse = new MotorFader(midiOut, 8);
		mainLayer.addBinding(new FaderBinding(masterTrack.volume(), masterFaderResponse));

		final HardwareButton masterTouchButton = surface.createHardwareButton("MASTER_TOUCH");
		masterTouchButton.pressedAction()
				.setActionMatcher(midiIn.createNoteOnActionMatcher(0, NoteOnAssignment.TOUCH_VOLUME.getNoteNo() + 8));
		masterTouchButton.releasedAction()
				.setActionMatcher(midiIn.createNoteOffActionMatcher(0, NoteOnAssignment.TOUCH_VOLUME.getNoteNo() + 8));
		masterTouchButton.isPressed().addValueObserver(v -> {
			// RemoteConsole.out.println("TOUCHED MASTER {}", v);
		});
	}

	private void intiVPotModes() {
		createOnOfBoolButton(NoteOnAssignment.V_TRACK, trackChannelMode);
//		final Action[] allActions = application.getActions();
//		for (final Action action : allActions) {
//			RemoteConsole.out.println(" ACTION [{}] name={} id=[{}] ", action.getCategory().getName(), action.getName(),
//					action.getId());
//		}
		// createOnOfBoolButton(NoteOnAssignment.SOLO, soloExclusive);

		createModeButton(VPotMode.SEND);
		createModeButton(VPotMode.PAN);
		createModeButton(VPotMode.PLUGIN);
		createModeButton(VPotMode.EQ);
		createModeButton(VPotMode.INSTRUMENT, VPotMode.MIDI_EFFECT);
		createModeButton(VPotMode.MIDI_EFFECT);
	}

	private void initCursorSection() {
		final HardwareButton trackLeftButton = createPressButton(NoteOnAssignment.TRACK_LEFT);
		mainLayer.bindIsPressed(trackLeftButton, v -> navigateTrack(-1, v));
		final HardwareButton trackRightButton = createPressButton(NoteOnAssignment.TRACK_RIGHT);
		mainLayer.bindIsPressed(trackRightButton, v -> navigateTrack(1, v));
		final HardwareButton bankRightButton = createPressButton(NoteOnAssignment.BANK_RIGHT);
		mainLayer.bindIsPressed(bankRightButton, v -> navigateBank(1, v));
		final HardwareButton bankLeftButton = createPressButton(NoteOnAssignment.BANK_LEFT);
		mainLayer.bindIsPressed(bankLeftButton, v -> navigateBank(-1, v));
		final HardwareButton upButton = createPressButton(NoteOnAssignment.CURSOR_UP);
		mainLayer.bindIsPressed(upButton, v -> navigateUpDown(1, v));
		final HardwareButton downButton = createPressButton(NoteOnAssignment.CURSOR_DOWN);
		mainLayer.bindIsPressed(downButton, v -> navigateUpDown(-1, v));
		final HardwareButton leftButton = createPressButton(NoteOnAssignment.CURSOR_LEFT);
		mainLayer.bindIsPressed(leftButton, v -> navigateLeftRight(-1, v));
		final HardwareButton rightButton = createPressButton(NoteOnAssignment.CURSOR_RIGHT);
		mainLayer.bindIsPressed(rightButton, v -> navigateLeftRight(1, v));
		createOnOfBoolButton(NoteOnAssignment.ZOOM, zoomActive);
		// createOnOfBoolButton(NoteOnAssignment.SCRUB, scrubActive);
	}

	private void navigateTrack(final int direction, final boolean isPressed) {
		if (!isPressed) {
			return;
		}
		if (direction > 0) {
			if (mixerMode.get() == MixerMode.DRUM) {
				cursorDeviceControl.getDrumPadBank().scrollForwards();
			} else {
				mixerTrackBank.scrollForwards();
				globalTrackBank.scrollForwards();
			}
		} else {
			if (mixerMode.get() == MixerMode.DRUM) {
				cursorDeviceControl.getDrumPadBank().scrollBackwards();
			} else {
				mixerTrackBank.scrollBackwards();
				globalTrackBank.scrollBackwards();
			}
		}
	}

	private void navigateBank(final int direction, final boolean isPressed) {
		if (!isPressed) {
			return;
		}
		final int numberOfTracksToControl = 8 * (nrOfExtenders + 1);
		if (mixerMode.get() == MixerMode.DRUM) {
			cursorDeviceControl.getDrumPadBank().scrollBy(direction * numberOfTracksToControl);
		} else {
			mixerTrackBank.scrollBy(direction * numberOfTracksToControl);
			globalTrackBank.scrollBy(direction * numberOfTracksToControl);
		}
	}

	private void navigateUpDown(final int direction, final boolean isPressed) {
		if (!zoomActive.get()) {
			sections.forEach(section -> section.navigateUpDown(direction, isPressed));
		} else {
			if (!isPressed) {
				return;
			}
			if (direction > 0) {
				arranger.zoomOutLaneHeightsAll();
			} else {
				arranger.zoomInLaneHeightsAll();
			}
		}
	}

	private void navigateLeftRight(final int direction, final boolean isPressed) {
		if (!zoomActive.get()) {
			sections.forEach(section -> section.navigateLeftRight(direction, isPressed));
		} else {
			if (!isPressed) {
				return;
			}
			if (direction < 0) {
				arranger.zoomOut();
			} else {
				arranger.zoomIn();
			}
		}
	}

	private void initModifiers() {
		final HardwareButton shiftButton = createPressButton(NoteOnAssignment.SHIFT);
		shiftButton.isPressed().addValueObserver(v -> {
			modifier.setShift(v);
			if (v) {
				shiftLayer.activate();
			} else {
				shiftLayer.deactivate();
			}
		});
		final HardwareButton optionButton = createPressButton(NoteOnAssignment.OPTION);
		optionButton.isPressed().addValueObserver(v -> {
			modifier.setOption(v);
		});
		final HardwareButton controlButton = createPressButton(NoteOnAssignment.CONTROL);
		controlButton.isPressed().addValueObserver(v -> {
			modifier.setControl(v);
		});
		final HardwareButton altButton = createPressButton(NoteOnAssignment.ALT);
		altButton.isPressed().addValueObserver(v -> {
			modifier.setAlt(v);
		});
	}

	private void initTransport() {
		transport.playStartPosition().markInterested();
		final HardwareButton playButton = createButton(NoteOnAssignment.PLAY);
		final HardwareButton recordButton = createButton(NoteOnAssignment.RECORD);
		final HardwareButton stopButton = createButtonWState(NoteOnAssignment.STOP, transport.isPlaying(), true);
		final HardwareButton rewindButton = createHoldButton(NoteOnAssignment.REWIND);
		final HardwareButton fastForwardButton = createHoldButton(NoteOnAssignment.FFWD);

		initCycleSection();
		initClickSection();

		final HardwareButton autoWriteButton = createButton(NoteOnAssignment.AUTO_READ_OFF);
		mainLayer.bindPressed(autoWriteButton, transport.isArrangerAutomationWriteEnabled());
		mainLayer.bind(transport.isArrangerAutomationWriteEnabled(),
				(OnOffHardwareLight) autoWriteButton.backgroundLight());
		shiftLayer.bindPressed(autoWriteButton, transport.isClipLauncherAutomationWriteEnabled());
		shiftLayer.bind(transport.isClipLauncherAutomationWriteEnabled(),
				(OnOffHardwareLight) autoWriteButton.backgroundLight());

		final HardwareButton touchButton = createButton(NoteOnAssignment.TOUCH);
		mainLayer.bindPressed(touchButton, () -> {
			transport.automationWriteMode().set("touch");
		});
		final HardwareButton latchButton = createButton(NoteOnAssignment.LATCH);
		mainLayer.bindPressed(latchButton, () -> {
			transport.automationWriteMode().set("latch");
		});
		final HardwareButton trimButton = createButton(NoteOnAssignment.AUTO_WRITE);
		mainLayer.bindPressed(trimButton, () -> {
			transport.automationWriteMode().set("write");
		});
		transport.automationWriteMode().addValueObserver(v -> {
			switch (v) {
			case "latch":
				setLed(latchButton, true);
				setLed(touchButton, false);
				setLed(trimButton, false);
				break;
			case "touch":
				setLed(latchButton, false);
				setLed(touchButton, true);
				setLed(trimButton, false);
				break;
			case "write":
				setLed(latchButton, false);
				setLed(touchButton, false);
				setLed(trimButton, true);
				break;
			}
		});

		final HardwareButton punchInButton = createButton(NoteOnAssignment.DROP);
		final HardwareButton punchOutButton = createButton(NoteOnAssignment.REPLACE);
		mainLayer.bindToggle(punchInButton, transport.isPunchInEnabled());
		mainLayer.bindToggle(punchOutButton, transport.isPunchOutEnabled());

		mainLayer.bindPressed(playButton, transport.continuePlaybackAction());
		mainLayer.bind(transport.isPlaying(), (OnOffHardwareLight) playButton.backgroundLight());
		mainLayer.bindPressed(stopButton, transport.stopAction());

		mainLayer.bindToggle(recordButton, transport.isArrangerRecordEnabled());
		shiftLayer.bindToggle(recordButton, transport.isClipLauncherOverdubEnabled());

		mainLayer.bindIsPressed(fastForwardButton, pressed -> notifyHoldForwardReverse(pressed, 1));

		mainLayer.bindIsPressed(rewindButton, pressed -> notifyHoldForwardReverse(pressed, -1));
		initUndoRedo();

		transport.timeSignature().addValueObserver(sig -> {
			ledDisplay.setDivision(sig);
		});

		transport.playPosition()
				.addValueObserver(v -> ledDisplay.updatePosition(v, transport.playPosition().getFormatted()));
		transport.playPositionInSeconds().addValueObserver(ledDisplay::updateTime);

		createOnOfBoolButton(NoteOnAssignment.FLIP, flipped);

		final HardwareButton vuModeButton = createButton(NoteOnAssignment.DIPLAY_NAME);
		vuModeButton.isPressed().addValueObserver(v -> {
			if (modifier.isShift()) {
				toogleVuMode(v);
			} else {
				sections.forEach(section -> section.handleNameDisplay(v));
			}
		});

		final HardwareButton modeButton = createButton(NoteOnAssignment.DISPLAY_SMPTE);
		modeButton.isPressed().addValueObserver(v -> {
			if (v) {
				ledDisplay.toggleMode();
			}
		});
	}

	private void initUndoRedo() {
		final HardwareButton undoButton = createPressButton(NoteOnAssignment.UNDO);
		final OnOffHardwareLight undoLight = (OnOffHardwareLight) undoButton.backgroundLight();
		application.canRedo().markInterested();
		undoLight.onUpdateHardware(() -> {
			sendLedUpdate(NoteOnAssignment.UNDO, undoLight.isOn().currentValue() ? 127 : 0);
		});
		mainLayer.bindIsPressed(undoButton, v -> {
			if (v) {
				application.undo();
				host.showPopupNotification("Undo");
			}
		});
		mainLayer.bind(application.canUndo(), undoLight);
		shiftLayer.bindIsPressed(undoButton, v -> {
			if (v) {
				application.redo();
				host.showPopupNotification("Redo");
			}
		});
		shiftLayer.bind(() -> {
			if (application.canRedo().get() && blinkTicks % 8 < 3) {
				return true;
			}
			return false;
		}, undoLight);
	}

	public void notifyHoldForwardReverse(final Boolean pressed, final int dir) {
		if (pressed) {
			holdAction.start(stage -> {
				if (modifier.isShiftSet()) {
					changePlayPosition(dir, FFWD_SPEEDS_SHIFT[Math.min(stage, FFWD_SPEEDS_SHIFT.length - 1)], true,
							true);
				} else {
					changePlayPosition(dir, FFWD_SPEEDS[Math.min(stage, FFWD_SPEEDS.length - 1)], true, true);
				}
			}, FFWD_TIMES);
		} else {
			holdAction.stop();
		}
	}

	public void createOnOfBoolButton(final NoteOnAssignment assignment, final SettableBooleanValue valueState) {
		final HardwareButton button = surface.createHardwareButton(assignment.toString() + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment.toString() + "_BUTTON_LED");
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(assignment, led.isOn().currentValue() ? 127 : 0);
		});
		mainLayer.bind(valueState, led);
		mainLayer.bindPressed(button, () -> {
			valueState.toggle();
		});
	}

	private void setLed(final HardwareButton button, final boolean onoff) {
		final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();
		light.isOn().setValue(onoff);
	}

	private void toogleVuMode(final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (this.vuMode == VuMode.LED) {
			this.vuMode = VuMode.LED_LCD_VERTICAL;
		} else if (this.vuMode == VuMode.LED_LCD_VERTICAL) {
			this.vuMode = VuMode.LED_LCD_HORIZONTAL;
		} else {
			this.vuMode = VuMode.LED;
		}
		sections.forEach(section -> section.applyVuMode(this.getVuMode()));
	}

	private HardwareButton createHoldButton(final NoteOnAssignment assignment) {
		final HardwareButton button = surface.createHardwareButton(assignment + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);

		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment + "BUTTON_LED");
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(assignment, led.isOn().currentValue() ? 127 : 0);
		});
		button.isPressed().addValueObserver(v -> {
			led.isOn().setValue(v);
		});
		return button;
	}

	private HardwareButton createButtonWState(final NoteOnAssignment assignment,
			final SettableBooleanValue settableBooleanValue, final boolean reversedLed) {
		final HardwareButton button = surface.createHardwareButton(assignment + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment + "BUTTON_LED");
		button.setBackgroundLight(led);
		if (reversedLed) {
			led.onUpdateHardware(() -> sendLedUpdate(assignment, settableBooleanValue.get() ? 0 : 127));
		} else {
			led.onUpdateHardware(() -> sendLedUpdate(assignment, settableBooleanValue.get() ? 127 : 0));
		}
		settableBooleanValue.addValueObserver(v -> {
			led.isOn().setValue(!v);
		});
		return button;
	}

	/**
	 * Creates modes button
	 *
	 * @param modes first mode is the button, the second represents the light
	 * @return the button
	 */
	private HardwareButton createModeButton(final VPotMode... modes) {
		assert modes.length > 0;
		final VPotMode mode = modes[0];

		final HardwareButton button = surface.createHardwareButton(mode.getName() + "_BUTTON");
		mode.getButtonAssignment().holdActionAssign(midiIn, button);

		final OnOffHardwareLight led = surface.createOnOffHardwareLight(mode.getName() + "BUTTON_LED");
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(mode.getButtonAssignment(), led.isOn().currentValue() ? 127 : 0);
		});
		mainLayer.bindPressed(button, () -> setVPotMode(mode, true));
		mainLayer.bindReleased(button, () -> setVPotMode(mode, false));
		if (modes.length == 1) {
			mainLayer.bind(() -> lightState(mode), led);
		} else if (modes.length == 2) {
			mainLayer.bind(() -> lightState(mode, modes[1]), led);
		}

		return button;
	}

	private boolean lightState(final VPotMode mode) {
		if (this.trackChannelMode.getMode() == mode) {
			return true;
		}
		return false;
	}

	private boolean lightState(final VPotMode mode, final VPotMode shiftmode) {
		if (this.trackChannelMode.getMode() == mode) {
			return true;
		} else if (this.trackChannelMode.getMode() == shiftmode) {
			if (blinkTicks % 8 < 3) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	public void setVPotMode(final VPotMode mode, final boolean down) {
		final VPotMode cmode = this.trackChannelMode.getMode();
		if (cmode != mode && cmode != null) {
			if (down) {
				this.trackChannelMode.setMode(mode);
			}
			sections.forEach(section -> section.notifyModeChange(mode, down));
		} else {
			sections.forEach(control -> control.notifyModeAdvance(mode, down));
		}
	}

	public TrackModeValue getVpotMode() {
		return this.trackChannelMode;
	}

	private HardwareButton createPressButton(final NoteOnAssignment assignment) {
		final HardwareButton button = surface.createHardwareButton(assignment.toString() + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		button.setBackgroundLight(surface.createOnOffHardwareLight(assignment.toString() + "_BUTTON_LED"));
		return button;
	}

	private HardwareButton createButton(final NoteOnAssignment assignment) {
		final HardwareButton button = surface.createHardwareButton(assignment + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment + "_BUTTON_LED");
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(assignment, led.isOn().currentValue() ? 127 : 0);
		});
		return button;
	}

	private void onMidi0(final ShortMidiMessage msg) {
//		RemoteConsole.out.println(" MIDI ch={} st={} d1={} d2={}", msg.getChannel(), msg.getStatusByte(),
//				msg.getData1(), msg.getData2());
	}

	private void setUpMidiSysExCommands() {
		midiIn.setSysexCallback(data -> {
			if (data.startsWith(SYSEX_DEVICE_RELOAD)) {
				updateAll();
			} else {
//				RemoteConsole.out.println(" MIDI SYS EX {}", data);
			}
		});
	}

	private void updateAll() {
		surface.updateHardware();
		sections.forEach(MixControl::fullHardwareUpdate);
		for (int i = 0; i < lightStatusMap.length; i++) {
			if (lightStatusMap[i] >= 0) {
				midiOut.sendMidi(Midi.NOTE_ON, i, lightStatusMap[i]);
			}
		}
	}

	protected void initTrackBank(final int nrOfScenes) {
		// initNaviagtion();
		final int numberOfHwChannels = 8 * (nrOfExtenders + 1);

		cursorTrack = getHost().createCursorTrack(8, nrOfScenes);
		cursorTrack.color().markInterested();

		cursorDeviceControl = new CursorDeviceControl(cursorTrack, 8, numberOfHwChannels);

		final HardwareButton soloButton = createButtonWState(NoteOnAssignment.SOLO, cursorTrack.solo(), false);
		mainLayer.bindPressed(soloButton, cursorTrack.solo().toggleAction());

		final TrackBank mainTrackBank = getHost().createMainTrackBank(numberOfHwChannels, 1, nrOfScenes);

		mainTrackBank.followCursorTrack(cursorTrack);

		mixerTrackBank = getHost().createMainTrackBank(numberOfHwChannels, 1, nrOfScenes);
		mixerTrackBank.setSkipDisabledItems(false);
		mixerTrackBank.canScrollChannelsDown().markInterested();
		mixerTrackBank.canScrollChannelsUp().markInterested();
		mixerTrackBank.scrollPosition().addValueObserver(offset -> {
			if (mixerMode.get() == MixerMode.MAIN) {
				ledDisplay.setAssignment(StringUtil.toTwoCharVal(offset + 1), false);
			}
		});

		globalTrackBank = host.createTrackBank(numberOfHwChannels, 1, nrOfScenes);
		globalTrackBank.setSkipDisabledItems(false);
		globalTrackBank.canScrollChannelsDown().markInterested();
		globalTrackBank.canScrollChannelsUp().markInterested();
		globalTrackBank.scrollPosition().addValueObserver(offset -> {
			if (mixerMode.get() == MixerMode.GLOBAL) {
				ledDisplay.setAssignment(StringUtil.toTwoCharVal(offset + 1), false);
			}
		});

		final DeviceTypeBank deviceTypeBank = new DeviceTypeBank(host, cursorDeviceControl);

		final DrumPadBank drumPadBank = cursorDeviceControl.getDrumPadBank();
		drumPadBank.setIndication(true);
		drumPadBank.scrollPosition().addValueObserver(offset -> {
			if (mixerMode.get() == MixerMode.DRUM) {
				ledDisplay.setAssignment(StringUtil.toTwoCharVal(offset), true);
			}
		});

		mixerMode.addValueObserver((oldMode, newMode) -> {
			switch (newMode) {
			case DRUM:
				ledDisplay.setAssignment(StringUtil.toTwoCharVal(drumPadBank.scrollPosition().get()), true);
				break;
			case GLOBAL:
				ledDisplay.setAssignment(StringUtil.toTwoCharVal(globalTrackBank.scrollPosition().get() + 1), false);
				break;
			case MAIN:
				ledDisplay.setAssignment(StringUtil.toTwoCharVal(mixerTrackBank.scrollPosition().get() + 1), false);
				break;
			default:
				break;
			}
		});

		for (final MixControl channelSection : sections) {
			channelSection.initMainControl(mixerTrackBank, globalTrackBank, drumPadBank);
		}
		mainSection.initTrackControl(cursorTrack, cursorDeviceControl.getDrumCursor(), deviceTypeBank);
		initMenuButtons();
	}

	public BrowserConfiguration getBrowserConfiguration() {
		return browserConfiguration;
	}

	public void setBrowserConfiguration(final BrowserConfiguration browserConfiguration) {
		this.browserConfiguration = browserConfiguration;
	}

	private void initMenuButtons() {
		final CueMarkerBank cueMarkerBank = arranger.createCueMarkerBank(8);
		cueMarkerBank.itemCount().addValueObserver(items -> {
			// RemoteConsole.out.println("Number Changed {}", items);
		});

		final BooleanValueObject marker = initCueMarkerSection(cueMarkerBank);

		createGlobalViewButton(NoteOnAssignment.GLOBAL_VIEW);
		createGroupModeButton(NoteOnAssignment.GROUP, buttonViewMode);
		// createNoteModeButton(NoteOnAssignment.GV_USER, buttonViewMode);

		initFButton(0, NoteOnAssignment.F1, marker, cueMarkerBank, () -> transport.returnToArrangement());
		initFButton(1, NoteOnAssignment.F2, marker, cueMarkerBank,
				() -> application.setPanelLayout(currentLayoutType.other().getName()));
		initFButton(2, NoteOnAssignment.F3, marker, cueMarkerBank, () -> application.navigateToParentTrackGroup());
		initFButton(3, NoteOnAssignment.F4, marker, cueMarkerBank, () -> {
		});
		initFButton(4, NoteOnAssignment.F5, marker, cueMarkerBank, () -> {
		});
		initFButton(5, NoteOnAssignment.F6, marker, cueMarkerBank, () -> {
		});
		initFButton(6, NoteOnAssignment.F7, marker, cueMarkerBank, () -> {
		});
		initFButton(7, NoteOnAssignment.F8, marker, cueMarkerBank, () -> {
		});

		application.panelLayout().addValueObserver(v -> {
			currentLayoutType = LayoutType.toType(v);
		});
	}

	private void initClickSection() {
		final HardwareButton metroButton = createButton(NoteOnAssignment.CLICK);
		final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("MARKER_MENU", mainSection);
		final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
		builder.bindBool("METRO", transport.isMetronomeEnabled());
		builder.bindBool("P.ROLL", transport.isMetronomeAudibleDuringPreRoll());
		builder.bindBool("M.TICK", transport.isMetronomeTickPlaybackEnabled());
		builder.bindValue("M.VOL", transport.metronomeVolume(), 0.05);
		builder.insertEmpty();
		builder.bindBool("T.CLCK", transportClick);

		transportClick.addValueObserver(value -> {
			midiOut.sendSysex(MAIN_UNIT_SYEX_HEADER + (value ? "0A 01" : "0A 00") + " F7");
		});
		builder.fillRest();
		bindHoldToggleButton(metroButton, menu, transport.isMetronomeEnabled());
	}

	private void initCycleSection() {
		final HardwareButton loopButton = createButton(NoteOnAssignment.CYCLE);
		final MenuModeLayerConfiguration cycleConfig = new MenuModeLayerConfiguration("MARKER_MENU", mainSection);
		final BeatTimeFormatter formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);

		final SettableBeatTimeValue cycleStart = transport.arrangerLoopStart();
		final SettableBeatTimeValue cycleLength = transport.arrangerLoopDuration();

		cycleStart.markInterested();
		cycleLength.markInterested();

		cycleConfig.addNameBinding(0, new BasicStringValue("Cycle"));
		cycleConfig.addNameBinding(1, new BasicStringValue("Start"));
		cycleConfig.addNameBinding(2, new BasicStringValue("Len"));
		cycleConfig.addNameBinding(4, new BasicStringValue("P.IN"));
		cycleConfig.addNameBinding(5, new BasicStringValue("P.OUT"));

		cycleConfig.addValueBinding(0, transport.isArrangerLoopEnabled(), "< ON >", "<OFF >");
		cycleConfig.addValueBinding(1, cycleStart, v -> {
			return cycleStart.getFormatted(formatter);
		});
		cycleConfig.addValueBinding(2, cycleLength, v -> {
			return cycleLength.getFormatted(formatter);
		});
		cycleConfig.addValueBinding(4, transport.isPunchInEnabled(), "< ON >", "<OFF >");
		cycleConfig.addValueBinding(5, transport.isPunchOutEnabled(), "< ON >", "<OFF >");

		cycleConfig.addRingBoolBinding(0, transport.isArrangerLoopEnabled());
		cycleConfig.addRingFixedBindingActive(1);
		cycleConfig.addRingFixedBindingActive(2);
		cycleConfig.addRingBoolBinding(0, transport.isPunchInEnabled());
		cycleConfig.addRingBoolBinding(0, transport.isPunchOutEnabled());

		cycleConfig.addPressEncoderBinding(0, index -> transport.isArrangerLoopEnabled().toggle());
		cycleConfig.addPressEncoderBinding(1, index -> {
			cycleStart.set(transport.getPosition().get());
		});
		cycleConfig.addPressEncoderBinding(4, index -> transport.isPunchInEnabled().toggle());
		cycleConfig.addPressEncoderBinding(5, index -> transport.isPunchOutEnabled().toggle());
		cycleConfig.addPressEncoderBinding(2, index -> {
			final double startTime = cycleStart.get();
			final double diff = transport.getPosition().get() - startTime;
			if (diff > 0) {
				cycleLength.set(diff);
			}
		});
		cycleConfig.addEncoderIncBinding(1, cycleStart, 1);
		cycleConfig.addEncoderIncBinding(2, cycleLength, 1);

		cycleConfig.addRingFixedBinding(3);
		cycleConfig.addRingFixedBinding(6);
		cycleConfig.addRingFixedBinding(7);
		// cycleConfig.addEncoderIncBinding(0, t, 1);
		bindHoldToggleButton(loopButton, cycleConfig, transport.isArrangerLoopEnabled());
	}

	private void bindHoldToggleButton(final HardwareButton button, final MenuModeLayerConfiguration menu,
			final SettableBooleanValue value) {
		mainLayer.bind(value, (OnOffHardwareLight) button.backgroundLight());
		mainLayer.bindPressed(button, () -> {
			holdState.enter(mainSection.getCurrentConfiguration(), button.getName());
			mainSection.setConfiguration(menu);
		});
		mainLayer.bindReleased(button, () -> {
			final LayerConfiguration layer = holdState.endHold();
			if (layer != null) {
				mainSection.setConfiguration(layer);
			}
			if (holdState.exit()) {
				value.toggle();
			}
		});
	}

	private BooleanValueObject initCueMarkerSection(final CueMarkerBank cueMarkerBank) {
		final HardwareButton markerButton = createButton(NoteOnAssignment.MARKER);
		final BooleanValueObject marker = new BooleanValueObject();

		final BeatTimeFormatter formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);

		final MenuModeLayerConfiguration markerMenuConfig = new MenuModeLayerConfiguration("MARKER_MENU", mainSection);
		for (int i = 0; i < 8; i++) {
			final CueMarker cueMarker = cueMarkerBank.getItemAt(i);
			cueMarker.exists().markInterested();
			markerMenuConfig.addValueBinding(i, cueMarker.position(), cueMarker, "---", v -> {
				return cueMarker.position().getFormatted(formatter);
			});
			markerMenuConfig.addNameBinding(i, cueMarker.getName(), cueMarker, "<Cue" + (i + 1) + ">");
			markerMenuConfig.addRingExistsBinding(i, cueMarker);
			markerMenuConfig.addPressEncoderBinding(i, index -> {
				if (cueMarker.exists().get()) {
					if (modifier.isShift()) {
						cueMarker.remove();
					} else {
						cueMarker.position().set(transport.getPosition().get());
					}
				} else {
					transport.addCueMarkerAtCurrentPosition();
				}
			});
			markerMenuConfig.addEncoderIncBinding(i, cueMarker.position(), 1);
		}

		bindHoldToggleButton(markerButton, markerMenuConfig, marker);
		return marker;
	}

	public void createGlobalViewButton(final NoteOnAssignment assignment) {
		final HardwareButton button = surface.createHardwareButton(assignment.toString() + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment.toString() + "_BUTTON_LED");
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(assignment, led.isOn().currentValue() ? 127 : 0);
		});
		mixerMode.addValueObserver((oldMode, newMode) -> {
			if (newMode != MixerMode.DRUM) {
				previousOverallMode = newMode;
			}
		});
		mainLayer.bind(() -> lightStateMixMode(mixerMode), led);
		mainLayer.bindPressed(button, () -> {
			if (mixerMode.get() == MixerMode.DRUM) {
				mixerMode.set(previousOverallMode);
			} else if (buttonViewMode.get() == ButtonViewState.GLOBAL_VIEW) {
				buttonViewMode.set(ButtonViewState.MIXER);
			} else {
				buttonViewMode.set(ButtonViewState.GLOBAL_VIEW);
			}
		});
	}

	private boolean lightStateMixMode(final ValueObject<MixerMode> valueState) {
		if (buttonViewMode.get() == ButtonViewState.GLOBAL_VIEW) {
			return true;
		} else if (valueState.get() == MixerMode.DRUM) {
			if (blinkTicks % 8 < 3) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	public MixerMode getPreviousOverallMode() {
		return previousOverallMode;
	}

	public void createNoteModeButton(final NoteOnAssignment assignment, final ValueObject<ButtonViewState> valueState) {
		final HardwareButton button = surface.createHardwareButton(assignment.toString() + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment.toString() + "_BUTTON_LED");
		button.setBackgroundLight(led);
		mainLayer.bind(() -> buttonViewMode.get() == ButtonViewState.GROUP_LAUNCH, led);
		mainLayer.bindPressed(button, () -> {
			final ButtonViewState current = valueState.get();
			switch (current) {
			case NOTE_PLAY:
				valueState.set(ButtonViewState.MIXER);
				break;
			default:
				valueState.set(ButtonViewState.NOTE_PLAY);
				break;
			}
		});

	}

	public void createGroupModeButton(final NoteOnAssignment assignment,
			final ValueObject<ButtonViewState> valueState) {
		final HardwareButton button = surface.createHardwareButton(assignment.toString() + "_BUTTON");
		assignment.holdActionAssign(midiIn, button);
		final OnOffHardwareLight led = surface.createOnOffHardwareLight(assignment.toString() + "_BUTTON_LED");
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedUpdate(assignment, led.isOn().currentValue() ? 127 : 0);
		});
		mainLayer.bind(() -> buttonViewMode.get() == ButtonViewState.GROUP_LAUNCH, led);
		mainLayer.bindPressed(button, () -> {
			final ButtonViewState current = valueState.get();
			switch (current) {
			case GROUP_LAUNCH:
				valueState.set(ButtonViewState.MIXER);
				break;
			default:
				valueState.set(ButtonViewState.GROUP_LAUNCH);
				break;
			}
		});
	}

	public void initFButton(final int index, final NoteOnAssignment assign, final BooleanValueObject marker,
			final CueMarkerBank cueMarkerBank, final Runnable nonMarkerFunction) {
		final HardwareButton fButton = createPressButton(assign);
		mainLayer.bindIsPressed(fButton, v -> {
			if (v) {
				if (marker.get()) {
					cueMarkerBank.getItemAt(index).launch(modifier.isShift());
				} else {
					nonMarkerFunction.run();
				}
			}
		});
	}

	public ValueObject<MixerMode> getMixerMode() {
		return mixerMode;
	}

	public ValueObject<ButtonViewState> getButtonView() {
		return buttonViewMode;
	}

	public CursorDeviceControl getCursorDeviceControl() {
		return cursorDeviceControl;
	}

	public CursorTrack getCursorTrack() {
		return cursorTrack;
	}

	public void sendLedUpdate(final NoteOnAssignment assingment, final int value) {
		final int noteNr = assingment.getNoteNo();
		lightStatusMap[noteNr] = value;
		midiOut.sendMidi(assingment.getType(), assingment.getNoteNo(), value);
	}

	public Layer getMainLayer() {
		return mainLayer;
	}

	private void shutDownController(final CompletableFuture<Boolean> shutdown) {
		ledDisplay.clearAll();
		midiOut.sendSysex(MAIN_UNIT_SYEX_HEADER + "0A 00 F7"); // turn off click
		sections.forEach(MixControl::resetLeds);
		sections.forEach(MixControl::resetFaders);
		masterFaderResponse.sendValue(0);
		sections.forEach(MixControl::exitMessage);
		try {
			Thread.sleep(300);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		shutdown.complete(true);
	}

	@Override
	public void exit() {
		final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
		Executors.newSingleThreadExecutor().execute(() -> shutDownController(shutdown));
		try {
			shutdown.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		getHost().showPopupNotification(" Exit Mackie MCU Pro");
	}

	public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
		return host.createRelativeHardwareControlStepTarget(//
				host.createAction(() -> consumer.accept(1), () -> "+"),
				host.createAction(() -> consumer.accept(-1), () -> "-"));
	}

	public Layers getLayers() {
		return layers;
	}

	public MidiIn getMidiIn() {
		return midiIn;
	}

	public HardwareSurface getSurface() {
		return surface;
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	public Project getProject() {
		return project;
	}

	public Application getApplication() {
		return application;
	}

	public ModifierValueObject getModifier() {
		return modifier;
	}

	public BooleanValueObject getFlipped() {
		return flipped;
	}

	public TrackModeValue getTrackChannelMode() {
		return trackChannelMode;
	}

	public DrumNoteHandler getNoteHandler() {
		return noteHandler;
	}
}
