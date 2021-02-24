package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SpecificPluginDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolSMk2Extension extends ControllerExtension {
	private static final int KOMPLETE_KONTROL_DEVICE_ID = 1315523403;
	private static byte[] levelDbLookup = new byte[201]; // maps level values to align with KK display
	final NhiaSysexValueCommand trackAvailableCommand = new NhiaSysexValueCommand(0x40);
	final NhiaSysexTextCommand selectTrackCommand = new NhiaSysexTextCommand(0x41);
	final NhiaSysexValueCommand trackSelectedCommand = new NhiaSysexValueCommand(0x42);
	final NhiaSysexValueCommand trackMutedCommand = new NhiaSysexValueCommand(0x43);
	final NhiaSysexValueCommand trackSoloCommand = new NhiaSysexValueCommand(0x44);
	final NhiaSysexValueCommand trackArmedCommand = new NhiaSysexValueCommand(0x45);
	final NhiaSysexTextCommand trackVolumeTextCommand = new NhiaSysexTextCommand(0x46);
	final NhiaSysexTextCommand trackPanTextCommand = new NhiaSysexTextCommand(0x47);
	final NhiaSysexTextCommand trackNameCommand = new NhiaSysexTextCommand(0x48);
	final NhiaSyexLevelsCommand trackLevelMeterComand = new NhiaSyexLevelsCommand(0x49);
	final NhiaSysexValueCommand trackMutedBySoloCommand = new NhiaSysexValueCommand(0x4A);

	private SpecificPluginDevice kompleteKontrolPlugin;
	private Parameter kompleteKontrolInstId;
	private HardwareSurface surface;
	private MidiOut midiOutDaw;
	private TrackBank mixerTrackBank;
	private Transport mTransport;
	final RelativeHardwareKnob[] volumeKnobs = new RelativeHardwareKnob[8];
	final RelativeHardwareKnob[] panKnobs = new RelativeHardwareKnob[8];

	final ModeButton[] selectButtons = new ModeButton[8];
	private MidiIn midiIn;
	private Layers layers;
	private KompleteLayer mainLayer;
	private Application application;
	// private Clip cursorClip;
	private CursorTrack cursorTrack;

	private boolean sceneNavMode = false;

	private LayoutType currentLayoutType = LayoutType.LAUNCHER;
	private Project project;

	boolean dawModeConfirmed = false;
	private KompleteLayer arrangeFocusLayer;
	private KompleteLayer sessionFocusLayer;

	protected KompleteKontrolSMk2Extension(final KompleteKontrolSMk2ExtensionDefinition definition,
			final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		final ControllerHost host = getHost();

		application = host.createApplication();
		midiOutDaw = host.getMidiOutPort(0);
		midiIn = host.getMidiInPort(0);
		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
		// RemoteConsole.out.println("Switchting To DAW MODE {}",
		// Thread.currentThread().getName());
		intoDawMode();
		initSliderLookup();
		surface = host.createHardwareSurface();
		layers = new Layers(this);
		mainLayer = new KompleteLayer(this, "Main");
		arrangeFocusLayer = new KompleteLayer(this, "ArrangeFocus");
		sessionFocusLayer = new KompleteLayer(this, "SeesionFocus");

		project = host.getProject();
		mTransport = host.createTransport();

		setUpSliders(midiIn);
		final MidiIn midiIn2 = host.getMidiInPort(1);
		final NoteInput noteInput = midiIn2.createNoteInput("KOMPLETE KONTROL-1", "80????", "90????", "D0????",
				"E0????");
		noteInput.setShouldConsumeEvents(true);

		initTrackBank();
		setUpTransport();
		initJogWheel();

		final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
		bindMacroControl(cursorDevice, midiIn2);

		mainLayer.activate();
		host.showPopupNotification("Komplete Kontrol S Mk2 Initialized");
	}

	private void initJogWheel() {
		final RelativeHardwareKnob fourDKnob = surface.createRelativeHardwareKnob("4D_WHEEL_PLUGIN_MODE");
		fourDKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x34, 128));
		fourDKnob.setStepSize(1 / 128.0);

		final HardwareActionBindable incAction = getHost().createAction(() -> {
		}, () -> "+");
		final HardwareActionBindable decAction = getHost().createAction(() -> {
		}, () -> "-");
		fourDKnob.addBinding(getHost().createRelativeHardwareControlStepTarget(incAction, decAction));

		final RelativeHardwareKnob fourDKnobMixer = surface.createRelativeHardwareKnob("4D_WHEEL_MIX_MODE");
		fourDKnobMixer.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x64, 4096));
		fourDKnobMixer.setStepSize(1 / 128.0);

		final HardwareActionBindable incMixAction = getHost().createAction(() -> {
		}, () -> "+");
		final HardwareActionBindable decMixAction = getHost().createAction(() -> {
		}, () -> "-");
		fourDKnobMixer.addBinding(getHost().createRelativeHardwareControlStepTarget(incMixAction, decMixAction));
	}

	private void bindMacroControl(final PinnableCursorDevice device, final MidiIn midiIn) {
		final CursorRemoteControlsPage remote = device.createCursorRemoteControlsPage(8);
		final AbsoluteHardwareKnob[] macroKnobs = new AbsoluteHardwareKnob[8];
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("MACRO_" + i);
			macroKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 14 + i));
			final RemoteControl parameter = remote.getParameter(index);
			parameter.setIndication(true);
			mainLayer.bind(knob, parameter);
		}
	}

	public void setUpTransport() {
		final Preferences preferences = getHost().getPreferences(); // THIS
		final SettableEnumValue focusMode = preferences.getEnumSetting("Focus", //
				"Recording/Automation",
				new String[] { FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor() },
				FocusMode.ARRANGER.getDescriptor());
		final ModeButton recButton = new ModeButton(this, "REC_BUTTON", CcAssignment.REC);
		final ModeButton autoButton = new ModeButton(this, "AUTO_BUTTON", CcAssignment.AUTO);
		final ModeButton countInButton = new ModeButton(this, "COUNTIN_BUTTON", CcAssignment.COUNTIN);
		focusMode.markInterested();

		arrangeFocusLayer.bindToggle(recButton.getHwButton(), mTransport.isArrangerRecordEnabled());
		arrangeFocusLayer.bindToggle(autoButton.getHwButton(), mTransport.isArrangerAutomationWriteEnabled());
		arrangeFocusLayer.bindToggle(countInButton.getHwButton(), mTransport.isArrangerOverdubEnabled());

		sessionFocusLayer.bindToggle(recButton.getHwButton(), mTransport.isClipLauncherOverdubEnabled());
		sessionFocusLayer.bindToggle(autoButton.getHwButton(), mTransport.isClipLauncherAutomationWriteEnabled());
		sessionFocusLayer.bindToggle(countInButton.getHwButton(), mTransport.isClipLauncherOverdubEnabled());

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

		final ModeButton playButton = new ModeButton(this, "PLAY_BUTTON", CcAssignment.PLAY);
		mainLayer.bindToggle(playButton.getHwButton(), mTransport.isPlaying());
		final ModeButton restartButton = new ModeButton(this, "RESTART_BUTTON", CcAssignment.RESTART);
		mainLayer.bindPressed(restartButton.getHwButton(), () -> {
			mTransport.stop();
			getHost().scheduleTask(() -> {
				mTransport.stop();
			}, 10);
			getHost().scheduleTask(() -> {
				mTransport.play();
			}, 20);
		});
		final ModeButton stopButton = new ModeButton(this, "STOP_BUTTON", CcAssignment.STOP);
		mainLayer.bindPressed(stopButton.getHwButton(), mTransport.stopAction());

		final ModeButton loopButton = new ModeButton(this, "LOOP_BUTTON", CcAssignment.LOOP);
		mainLayer.bindToggle(loopButton.getHwButton(), mTransport.isArrangerLoopEnabled());
		final ModeButton metroButton = new ModeButton(this, "METRO_BUTTON", CcAssignment.METRO);
		mainLayer.bindToggle(metroButton.getHwButton(), mTransport.isMetronomeEnabled());
		final ModeButton tapTempoButton = new ModeButton(this, "TAP_BUTTON", CcAssignment.TAPTEMPO);
		mainLayer.bindPressed(tapTempoButton.getHwButton(), () -> {
			mTransport.tapTempo();
		});
		tapTempoButton.bindLightToPressed();

		final ModeButton undoButton = new ModeButton(this, "UNDO_BUTTON", CcAssignment.UNDO);
		mainLayer.bindPressed(undoButton.getHwButton(), () -> {
			application.undo();
		});
		undoButton.getLed().isOn().setValue(true); // As long as there is no canUndo

		final ModeButton redoButton = new ModeButton(this, "REDO_BUTTON", CcAssignment.REDO);
		mainLayer.bindPressed(redoButton.getHwButton(), () -> {
			application.redo();
		});
		redoButton.getLed().isOn().setValue(true);

	}

	public void setUpSliders(final MidiIn midiIn) {
		for (int i = 0; i < 8; i++) {
			final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("VOLUME_KNOB" + i);
			volumeKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x50 + i, 128));
			knob.setStepSize(1 / 1024.0);

			final RelativeHardwareKnob panKnob = surface.createRelativeHardwareKnob("PAN_KNOB" + i);
			panKnobs[i] = panKnob;
			panKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x58 + i, 128));
			panKnob.setStepSize(1 / 1024.0);
		}
	}

	public Layers getLayers() {
		return layers;
	}

	public HardwareSurface getSurface() {
		return surface;
	}

	public MidiIn getMidiIn() {
		return midiIn;
	}

	private static void initSliderLookup() {
		final int[] intervalls = { 0, 25, 63, 100, 158, 200 };
		final int[] pollValues = { 0, 14, 38, 67, 108, 127 };
		int curentIv = 1;
		int ivLb = 0;
		int plLb = 0;
		int ivUb = intervalls[curentIv];
		int plUb = pollValues[curentIv];
		double ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
		for (int i = 0; i < levelDbLookup.length; i++) {
			if (i > intervalls[curentIv]) {
				curentIv++;
				ivLb = ivUb;
				plLb = plUb;
				ivUb = intervalls[curentIv];
				plUb = pollValues[curentIv];
				ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
			}
			levelDbLookup[i] = (byte) Math.round(plLb + (i - ivLb) * ratio);
		}
	}

	@Override
	public void exit() {
		midiOutDaw.sendMidi(Midi.KK_DAW, Midi.GOODBYE, 0);
		getHost().showPopupNotification("Komplete Kontrol S Mk2 Exited");
	}

	@Override
	public void flush() {
		if (dawModeConfirmed) {
			surface.updateHardware();
		}
	}

	public void intoDawMode() {
		midiOutDaw.sendMidi(0xBF, 0x1, 0x3);
	}

	public void sendLedUpdate(final CcAssignment assignement, final int value) {
		midiOutDaw.sendMidi(0xBF, assignement.getStateId(), value);
	}

	public void sendLedUpdate(final int code, final int value) {
		midiOutDaw.sendMidi(0xBF, code, value);
	}

	/** Called when we receive short MIDI message on port 0. */
	private void onMidi0(final ShortMidiMessage msg) {
		if (msg.getStatusByte() == 0xBF) {
			if (msg.getData1() == 1) {
				dawModeConfirmed = true;
			}
		}
	}

	private void createKompleteKontrolDeviceKompleteKontrol(final PinnableCursorDevice cursorDevice) {
		kompleteKontrolPlugin = cursorDevice.createSpecificVst2Device(KOMPLETE_KONTROL_DEVICE_ID);
		kompleteKontrolInstId = kompleteKontrolPlugin.createParameter(0);
		kompleteKontrolInstId.markInterested();
		kompleteKontrolInstId.name().markInterested();
		kompleteKontrolInstId.exists().markInterested();
		kompleteKontrolInstId.name().addValueObserver(name -> {
			selectTrackCommand.send(midiOutDaw, name);
		});
	}

	private void initNaviagtion() {
		final Clip cursorClip = getHost().createLauncherCursorClip(8, 128);
		final Clip arangerClip = getHost().createArrangerCursorClip(8, 128);

		arangerClip.exists().markInterested();
		final Track rootTrack = project.getRootTrackGroup();
		cursorClip.clipLauncherSlot().setIndication(true);

		final TrackBank singleTrackBank = getHost().createTrackBank(1, 0, 1);
		singleTrackBank.scrollPosition().markInterested();

		cursorTrack = getHost().createCursorTrack(1, 1);
		singleTrackBank.followCursorTrack(cursorTrack);

		final Track theTrack = singleTrackBank.getItemAt(0);
		final ClipLauncherSlotBank slotBank = theTrack.clipLauncherSlotBank();
		slotBank.setIndication(true);
		final ClipLauncherSlot theClip = slotBank.getItemAt(0);

		final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
		createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
		final SceneBank sceneBank = singleTrackBank.sceneBank();

		sceneBank.cursorIndex().markInterested();
		sceneBank.canScrollBackwards().markInterested();
		sceneBank.canScrollForwards().markInterested();

		sceneBank.getScene(0).setIndication(false);

		application.panelLayout().addValueObserver(v -> {
			currentLayoutType = LayoutType.toType(v);
			updateLedChannelDown(singleTrackBank, singleTrackBank.canScrollChannelsDown().get());
			udpateLedChannelUp(singleTrackBank, singleTrackBank.canScrollChannelsUp().get());
			updateLedSceneForwards(sceneBank, sceneBank.canScrollForwards().get());
			updateLedSceneBackwards(sceneBank, sceneBank.canScrollBackwards().get());
		});

		singleTrackBank.canScrollChannelsUp().addValueObserver(v -> {
			udpateLedChannelUp(singleTrackBank, v);
		});
		singleTrackBank.canScrollChannelsDown().addValueObserver(v -> {
			updateLedChannelDown(singleTrackBank, v);
		});

		sceneBank.canScrollBackwards().addValueObserver(v -> {
			updateLedSceneBackwards(sceneBank, v);
		});
		sceneBank.canScrollForwards().addValueObserver(v -> {
			updateLedSceneForwards(sceneBank, v);
		});

		final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
		leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));
		final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
		rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));
		final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
		upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));
		final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
		downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));
		mainLayer.bindPressed(leftNavButton, () -> {
			switch (currentLayoutType) {
			case LAUNCHER:
				singleTrackBank.scrollBy(1);
				theClip.select();
				theTrack.selectInMixer();
				if (sceneNavMode) {
					sceneNavMode = false;
					sceneBank.getScene(0).setIndication(false);
					theClip.setIndication(true);
				}
				break;
			case ARRANGER:
				sceneBank.scrollForwards();
				theClip.select();
				break;
			default:
				break;
			}
		});
		mainLayer.bindPressed(rightNavButton, () -> {
			switch (currentLayoutType) {
			case LAUNCHER:
				if (singleTrackBank.scrollPosition().get() == 0) {
					sceneNavMode = true;
					sceneBank.getScene(0).setIndication(true);
					theClip.setIndication(false);
				} else {
					singleTrackBank.scrollBy(-1);
					theClip.select();
					theTrack.selectInMixer();
				}
				break;
			case ARRANGER:
				sceneBank.scrollBackwards();
				theClip.select();
				break;
			default:
				break;
			}
		});
		mainLayer.bindPressed(upNavButton, () -> {
			switch (currentLayoutType) {
			case LAUNCHER:
				sceneBank.scrollBackwards();
				theClip.select();
				break;
			case ARRANGER:
				if (singleTrackBank.scrollPosition().get() == 0) {
					sceneNavMode = true;
					sceneBank.getScene(0).setIndication(true);
					theClip.setIndication(false);
				} else {
					singleTrackBank.scrollBy(-1);
					theClip.select();
					theTrack.selectInMixer();
				}
				break;
			default:
				break;
			}
		});
		mainLayer.bindPressed(downNavButton, () -> {
			switch (currentLayoutType) {
			case LAUNCHER:
				sceneBank.scrollForwards();
				theClip.select();
				break;
			case ARRANGER:
				singleTrackBank.scrollBy(1);
				theClip.select();
				theTrack.selectInMixer();
				if (sceneNavMode) {
					sceneNavMode = false;
					sceneBank.getScene(0).setIndication(false);
					theClip.setIndication(true);
				}
				break;
			default:
				break;
			}
		});

		cursorClip.exists().markInterested();
		final ModeButton quantizeButton = new ModeButton(this, "QUANTIZE_BUTTON", CcAssignment.QUANTIZE);
		sessionFocusLayer.bindPressed(quantizeButton, () -> {
			cursorClip.quantize(1.0);
		});
		sessionFocusLayer.bind(() -> {
			return cursorTrack.canHoldNoteData().get() && cursorClip.exists().get();
		}, quantizeButton.getLed());

		arrangeFocusLayer.bindPressed(quantizeButton, () -> {
			arangerClip.quantize(1.0);
		});
		arrangeFocusLayer.bind(() -> {
			return cursorTrack.canHoldNoteData().get() && arangerClip.exists().get();
		}, quantizeButton.getLed());

		cursorTrack.canHoldNoteData().markInterested();
		cursorClip.exists().markInterested();

		final ModeButton clearButton = new ModeButton(this, "CLEAR_BUTTON", CcAssignment.CLEAR);
		sessionFocusLayer.bindPressed(clearButton, () -> {
			cursorClip.clearSteps();
		});
		sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
				clearButton.getLed());

		arrangeFocusLayer.bindPressed(clearButton, () -> {
			arangerClip.clearSteps();
		});
		arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arangerClip.exists().get(),
				clearButton.getLed());

		clearButton.getLed().isOn().setValueSupplier(() -> {
			return cursorTrack.canHoldNoteData().get() && cursorClip.exists().get();
		});

		final ModeButton knobPressed = new ModeButton(this, "KNOB4D_PRESSED", CcAssignment.PRESS_4D_KNOB);
		mainLayer.bindPressed(knobPressed, () -> {
			if (sceneNavMode) {
				sceneBank.getScene(0).launch();
			} else {
				theClip.launch();
			}
		});
		final ModeButton knobShiftPressed = new ModeButton(this, "KNOB4D_PRESSED_SHIFT",
				CcAssignment.PRESS_4D_KNOB_SHIFT);
		mainLayer.bindPressed(knobShiftPressed, () -> {
			if (sceneNavMode) {
				rootTrack.stop();
			} else {
				cursorTrack.stop();
			}
		});
	}

	private void updateLedSceneForwards(final SceneBank sceneBank, final boolean v) {
		final int sv = (v ? 0x2 : 0x0) | (sceneBank.canScrollBackwards().get() ? 0x1 : 0x0);
		switch (currentLayoutType) {
		case LAUNCHER:
			sendLedUpdate(0x32, sv);
			break;
		case ARRANGER:
			sendLedUpdate(0x30, sv);
			break;
		default:
			break;
		}
	}

	private void updateLedSceneBackwards(final SceneBank sceneBank, final boolean v) {
		final int sv = sceneBank.canScrollForwards().get() ? 0x2 : 0x0;
		switch (currentLayoutType) {
		case LAUNCHER:
			sendLedUpdate(0x32, (v ? 0x1 : 0x0) | sv);
			break;
		case ARRANGER:
			sendLedUpdate(0x30, (v ? 0x1 : 0x0) | sv);
			break;
		default:
			break;
		}
	}

	private void updateLedChannelDown(final TrackBank singleTrackBank, final boolean v) {
		final int sv = (v ? 0x2 : 0x0) | (singleTrackBank.canScrollChannelsUp().get() ? 0x1 : 0x0);
		switch (currentLayoutType) {
		case LAUNCHER:
			sendLedUpdate(0x30, sv);
			break;
		case ARRANGER:
			sendLedUpdate(0x32, sv);
			break;
		default:
			break;
		}
	}

	private void udpateLedChannelUp(final TrackBank singleTrackBank, final boolean v) {
		final int sv = (v ? 0x1 : 0x0) | (singleTrackBank.canScrollChannelsDown().get() ? 0x2 : 0x0);
		switch (currentLayoutType) {
		case LAUNCHER:
			sendLedUpdate(0x30, sv);
			break;
		case ARRANGER:
			sendLedUpdate(0x32, sv);
			break;
		default:
			break;
		}
	}

	private void initTrackBank() {
		initNaviagtion();

		final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
		createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
		mixerTrackBank = getHost().createTrackBank(8, 0, 1);
		mixerTrackBank.setSkipDisabledItems(true);
		mixerTrackBank.canScrollChannelsDown().markInterested();
		mixerTrackBank.canScrollChannelsUp().markInterested();

		final HardwareButton trackNavLeftButton = surface.createHardwareButton("TRACK_LEFT_NAV_BUTTON");
		trackNavLeftButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x31, 127));
		final HardwareButton trackRightNavButton = surface.createHardwareButton("TRACK_RIGHT_NAV_BUTTON");
		trackRightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x31, 1));
		mixerTrackBank.setChannelScrollStepSize(8);

		mainLayer.bindPressed(trackNavLeftButton, () -> {
			mixerTrackBank.scrollBy(-8);
		});

		mainLayer.bindPressed(trackRightNavButton, () -> {
			mixerTrackBank.scrollBy(8);
		});

		mixerTrackBank.canScrollChannelsUp().addValueObserver(v -> {
			final int bw = mixerTrackBank.canScrollChannelsDown().get() ? 0x2 : 0x0;
			sendLedUpdate(0x31, (v ? 0x1 : 0x0) | bw);
		});
		mixerTrackBank.canScrollChannelsDown().addValueObserver(v -> {
			final int bw = mixerTrackBank.canScrollChannelsUp().get() ? 0x1 : 0x0;
			sendLedUpdate(0x31, (v ? 0x2 : 0x0) | bw);
		});

		for (int i = 0; i < 8; i++) {
			setUpChannelControl(i, mixerTrackBank.getItemAt(i));
		}
	}

	private void setUpChannelControl(final int index, final Track channel) {
		final IndexButton selectButton = new IndexButton(this, index, "SELECT_BUTTON", 0x42);
		mainLayer.bindPressed(selectButton.getHwButton(), () -> {
			if (!channel.exists().get()) {
				application.createInstrumentTrack(-1);
			} else {
				channel.selectInMixer();
			}
		});
		final IndexButton muteButton = new IndexButton(this, index, "MUTE_BUTTON", 0x43);
		mainLayer.bindPressed(muteButton.getHwButton(), () -> channel.mute().toggle());
		final IndexButton soloButton = new IndexButton(this, index, "SOLO_BUTTON", 0x44);
		mainLayer.bindPressed(soloButton.getHwButton(), () -> channel.solo().toggle());
		final IndexButton armButton = new IndexButton(this, index, "ARM_BUTTON", 0x45);
		mainLayer.bindPressed(armButton.getHwButton(), () -> channel.arm().toggle());

		channel.exists().markInterested();
		channel.addIsSelectedInMixerObserver(v -> {
			trackSelectedCommand.send(midiOutDaw, index, v);
		});
		channel.mute().addValueObserver(v -> {
			trackMutedCommand.send(midiOutDaw, index, v);
		});
		channel.solo().addValueObserver(v -> {
			trackSoloCommand.send(midiOutDaw, index, v);
		});
		channel.arm().addValueObserver(v -> {
			trackArmedCommand.send(midiOutDaw, index, v);
		});
		channel.isMutedBySolo().addValueObserver(v -> {
			trackMutedBySoloCommand.send(midiOutDaw, index, v);
		});

		channel.name().addValueObserver(name -> {
			trackNameCommand.send(midiOutDaw, index, name);
		});

		channel.volume().displayedValue().addValueObserver(valueText -> {
			trackVolumeTextCommand.send(midiOutDaw, index, valueText);
		});

		channel.volume().value().addValueObserver(value -> {
			final byte v = toSliderVal(value);
			midiOutDaw.sendMidi(0xBF, 0x50 + index, v);
		});
		channel.pan().value().addValueObserver(value -> {
			final int v = (int) (value * 127);
			midiOutDaw.sendMidi(0xBF, 0x58 + index, v);
		});
		channel.addVuMeterObserver(201, 0, true, leftValue -> {
			trackLevelMeterComand.updateLeft(index, levelDbLookup[leftValue]);
		});
		channel.trackType().addValueObserver(v -> {
			final TrackType type = TrackType.toType(v);
			trackAvailableCommand.send(midiOutDaw, index, type.getId());
		});
		channel.addVuMeterObserver(201, 1, true, rightValue -> {
			trackLevelMeterComand.updateRight(index, levelDbLookup[rightValue]);
			trackLevelMeterComand.update(midiOutDaw);
		});
		volumeKnobs[index].addBindingWithSensitivity(channel.volume(), 0.025);
		panKnobs[index].addBindingWithSensitivity(channel.pan(), 0.025);

		channel.isActivated().markInterested();
		channel.canHoldAudioData().markInterested();
		channel.canHoldNoteData().markInterested();
	}

	byte toSliderVal(final double value) {
		return levelDbLookup[(int) (value * 200)];
	}

}
