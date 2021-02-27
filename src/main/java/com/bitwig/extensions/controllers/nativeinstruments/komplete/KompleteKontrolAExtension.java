package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.AutoDetectionMidiPortNames;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

public class KompleteKontrolAExtension extends KompleteKontrolExtension {

	protected KompleteKontrolAExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		super.init();
		intoDawMode();
		final ControllerHost host = getHost();
		surface = host.createHardwareSurface();
		layers = new Layers(this);
		mainLayer = new KompleteLayer(this, "Main");
		arrangeFocusLayer = new KompleteLayer(this, "ArrangeFocus");
		sessionFocusLayer = new KompleteLayer(this, "SessionFocus");

		project = host.getProject();
		mTransport = host.createTransport();

		final AutoDetectionMidiPortNamesList defs = getExtensionDefinition()
				.getAutoDetectionMidiPortNamesList(host.getPlatformType());

		final AutoDetectionMidiPortNames inport = defs.getPortNames().get(0);
		RemoteConsole.out.println("NAMES = {}", inport.getInputNames()[1]);

		setUpSliders(midiIn);
		final MidiIn midiIn2 = host.getMidiInPort(1);
		final NoteInput noteInput = midiIn2.createNoteInput(inport.getInputNames()[1], "80????", "90????", "D0????",
				"E0????");
		noteInput.setShouldConsumeEvents(true);

		initTrackBank();
		setUpTransport();
		initJogWheel();

		final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
		bindMacroControl(cursorDevice, midiIn2);

		for (final CcAssignment cc : CcAssignment.values()) {
			sendLedUpdate(cc, 0);
		}
		mainLayer.activate();
		host.showPopupNotification("Komplete Kontrol A Initialized");
	}

	@Override
	protected void setUpSliders(final MidiIn midiIn) {
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

	@Override
	protected void initNaviagtion() {
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
		});

		final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
		leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));
		final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
		rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));
		final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
		upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));
		final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
		downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));
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
			RemoteConsole.out.println("EXEC QUANTIZE");
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

	@Override
	public void exit() {
		midiOutDaw.sendMidi(Midi.KK_DAW, Midi.GOODBYE, 0);
		getHost().showPopupNotification("Komplete Kontrol A Series Exited");
	}

	@Override
	public void flush() {
		if (dawModeConfirmed) {
			surface.updateHardware();
		}
	}

	@Override
	protected void setUpChannelControl(final int index, final Track channel) {
		final IndexButton selectButton = new IndexButton(this, index, "SELECT_BUTTON", 0x42);
		mainLayer.bindPressed(selectButton.getHwButton(), () -> {
			if (!channel.exists().get()) {
				application.createInstrumentTrack(-1);
			} else {
				channel.selectInMixer();
			}
		});

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

		channel.pan().displayedValue().addValueObserver(value -> {
			trackPanTextCommand.send(midiOutDaw, index, value);
		});

		channel.pan().value().addValueObserver(value -> {
			final int v = (int) (value * 127);
			midiOutDaw.sendMidi(0xBF, 0x58 + index, v);
		});

		channel.trackType().addValueObserver(v -> {
			final TrackType type = TrackType.toType(v);
			trackAvailableCommand.send(midiOutDaw, index, type.getId());
		});
		volumeKnobs[index].addBindingWithSensitivity(channel.volume(), 0.025);
		panKnobs[index].addBindingWithSensitivity(channel.pan(), 0.025);

		channel.isActivated().markInterested();
		channel.canHoldAudioData().markInterested();
		channel.canHoldNoteData().markInterested();
	}

	@Override
	protected void setUpChannelDisplayFeedback(final int index, final Track channel) {
	}
}
