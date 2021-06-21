package com.bitwig.extensions.controllers.mackie.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.VPotMode;
import com.bitwig.extensions.controllers.mackie.VPotMode.Assign;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayNameBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayStringValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingDisplayBinding;
import com.bitwig.extensions.controllers.mackie.bindings.TouchFaderBinding;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTracker;
import com.bitwig.extensions.controllers.mackie.devices.Devices;
import com.bitwig.extensions.controllers.mackie.devices.EqDevice;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.targets.DisplayNameTarget;
import com.bitwig.extensions.controllers.mackie.targets.DisplayValueTarget;
import com.bitwig.extensions.controllers.mackie.targets.MotorFader;
import com.bitwig.extensions.controllers.mackie.targets.RingDisplay;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;

public class ChannelSection {

	private final MidiIn midiIn;
	private final MidiOut midiOut;
	private final int[] lightStatusMap = new int[127];

	final BindingCache volumeBindings = new BindingCache();

	private final AbsoluteHardwareKnob[] volumeKnobs = new AbsoluteHardwareKnob[8];
	private final RelativeHardwareKnob[] encoders = new RelativeHardwareKnob[8];
	private final HardwareButton[] encoderPress = new HardwareButton[8];
	private final HardwareButton[] faderTouch = new HardwareButton[8];
	private final MotorFader[] motorFaderDest = new MotorFader[8];
	private final RingDisplay[] ringDisplays = new RingDisplay[8];

	final DisplayValueTarget[] valueTargets = new DisplayValueTarget[8];
	final DisplayNameTarget[] nameTargets = new DisplayNameTarget[8];

	private final List<FlippableLayer> layers = new ArrayList<FlippableLayer>();
	private final Track[] channels = new Track[8];

	private final MackieMcuProExtension driver;
	private final int sectionIndex;
	private final LcdDisplay mainDisplay;

	private final FlippableLayer panLayer;
	private final FlippableBankLayer sendLayer;
	private final FlippableParameterBankLayer eqTrackLayer;
	private final FlippableLayer sendTrackLayer;
	private final FlippableRemoteLayer instrumentTrackLayer;
	private final FlippableRemoteLayer pluginTrackLayer;
	private FlippableLayer currentLayer;

	private final BooleanValueObject fadersTouched = new BooleanValueObject();
	private int touchCount = 0;
	private final SectionType type;
	private CursorTrack cursorTrack;

	public enum SectionType {
		MAIN, XTENDER;
	}

	public ChannelSection(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
			final int sectionIndex, final SectionType type) {
		this.midiIn = midiIn;
		this.midiOut = midiOut;
		this.driver = driver;
		this.sectionIndex = sectionIndex;
		this.type = type;
		panLayer = new FlippableLayer(this, "PANE_" + sectionIndex + "_LAYER");
		sendLayer = new FlippableBankLayer(this, "SEND_" + sectionIndex + "_LAYER");

		sendTrackLayer = new FlippableLayer(this, "SEND_TRACK_" + sectionIndex + "_LAYER");
		eqTrackLayer = new FlippableParameterBankLayer(this, "EQ_TRACK_" + sectionIndex + "_LAYER");
		instrumentTrackLayer = new FlippableRemoteLayer(this, "INSTRUMENT_" + sectionIndex + "_LAYER");
		pluginTrackLayer = new FlippableRemoteLayer(this, "PLUGIN_" + sectionIndex + "_LAYER");
		eqTrackLayer.setMessages("no EQ+ device on track", "<< press EQ button to insert EQ+ device >>");

		layers.add(panLayer);
		layers.add(sendTrackLayer);
		layers.add(sendLayer);
		layers.add(eqTrackLayer);
		layers.add(instrumentTrackLayer);
		layers.add(pluginTrackLayer);

		mainDisplay = new LcdDisplay(driver, midiOut, type);
		final HardwareSurface surface = driver.getSurface();
		initControlHardware(surface);

		panLayer.activate();
		currentLayer = panLayer;
		driver.getFlipped().addValueObserver(flipped -> {
			layers.forEach(layer -> layer.setFlipped(flipped));
		});

		// TODO this needs to work with Xtender
		fadersTouched.addValueObserver(touched -> {
			if (touched) {
				layers.forEach(layer -> layer.setTouched(touched));
				driver.cancelAction("TOUCH");
			} else {
				driver.scheduleAction("TOUCH", 1500, () -> {
					layers.forEach(layer -> layer.setTouched(touched));
				});
			}
		});
		for (int i = 0; i < lightStatusMap.length; i++) {
			lightStatusMap[i] = -1;
		}
		driver.getTrackChannelMode().addValueObserver(v -> changeTrackMode(v, driver.getTrackChannelMode().getMode()));
	}

	public void initControlHardware(final HardwareSurface surface) {
		for (int i = 0; i < 8; i++) {
			final AbsoluteHardwareKnob knob = surface
					.createAbsoluteHardwareKnob("VOLUME_FADER_" + sectionIndex + "_" + i);
			volumeKnobs[i] = knob;
			faderTouch[i] = createTouchButton("FADER_TOUCH", i);
			knob.setAdjustValueMatcher(this.midiIn.createAbsolutePitchBendValueMatcher(i));

			motorFaderDest[i] = new MotorFader(this.midiOut, i);
			ringDisplays[i] = new RingDisplay(this.midiOut, i);

			final RelativeHardwareKnob encoder = surface
					.createRelativeHardwareKnob("PAN_KNOB" + sectionIndex + "_" + i);
			encoders[i] = encoder;
			encoderPress[i] = createEncoderButon(i);
			encoder.setAdjustValueMatcher(this.midiIn.createRelativeSignedBitCCValueMatcher(0x0, 0x10 + i, 200));
			encoder.setStepSize(1 / 128.0);
			encoder.isUpdatingTargetValue().addValueObserver(v -> {
				if (v) {
					driver.doActionImmediate("TOUCH");
				}
			});

			valueTargets[i] = new DisplayValueTarget(mainDisplay, i);
			nameTargets[i] = new DisplayNameTarget(mainDisplay, i);
		}
	}

	public void initChannelControl(final TrackBank mixerTrackBank, final CursorTrack cursorTrack) {
		this.cursorTrack = cursorTrack;
		for (int i = 0; i < 8; i++) {
			final int trackIndex = i + sectionIndex * 8;
			setUpChannelControl(i, mixerTrackBank.getItemAt(trackIndex));
		}
		if (type == SectionType.MAIN) {
			final SendBank sendBank = cursorTrack.sendBank();
			for (int i = 0; i < 8; i++) {
				sendTrackLayer.addBinding(i, sendBank.getItemAt(i), RingDisplayType.FILL_LR, false);
			}
			initEqDevice();
			initInstrumentDevice();
			driver.getCursorDevice().deviceType().markInterested();
//			driver.getCursorDevice().deviceType().addValueObserver(d -> handleDeviceTypeChanged(d));
			driver.getCursorDevice().name().addValueObserver(name -> handleDeviceNameChanged(name));
		}

	}

	private void handleDeviceNameChanged(final String name) {
//		RemoteConsole.out.println(" dn > {} => {} :: {}", name, driver.getCursorDevice().deviceType().get(),
//				driver.getVpotMode());
		final String type = driver.getCursorDevice().deviceType().get();
		switch (driver.getVpotMode()) {
		case EQ:
			if (!name.equals("EQ+")) {
				forceSwitch(type);
			}
			break;
		case INSTRUMENT:
			if (!type.equals("instrument")) {
				if (name.equals("EQ+")) {
					forceSwitch("eq+");
				} else {
					forceSwitch(type);
				}
			}
			break;
		case PLUGIN:
			if (!type.equals("audio-effect")) {
				if (name.equals("EQ+")) {
					forceSwitch("eq+");
				} else {
					forceSwitch(type);
				}
			}
			break;
		default:
			break;
		}
	}

	private void forceSwitch(final String type) {
		switch (type) {
		case "audio-effect":
			driver.setVPotMode(VPotMode.PLUGIN);
			break;
		case "instrument":
			driver.setVPotMode(VPotMode.INSTRUMENT);
			break;
		case "eq+":
			break;
		}
	}

	private void initInstrumentDevice() {
		final DeviceTracker instrumentDevice = driver.getInstrumentDevice();

		instrumentTrackLayer.setDevice(instrumentDevice);
		instrumentTrackLayer.setMessages("no Instrument on Track", "<< select >>");
		instrumentTrackLayer.addDisplayBinding(mainDisplay);

		instrumentTrackLayer
				.setResetAction((index, param) -> instrumentDevice.handleReset(index, param, driver.getModifier()));
		for (int i = 0; i < 8; i++) {
			instrumentTrackLayer.addBinding(i, instrumentDevice.getParameter(i), RingDisplayType.FILL_LR, false);
		}
		final DeviceTracker fxDevice = driver.getPluginDevice();
		pluginTrackLayer.setResetAction((index, param) -> fxDevice.handleReset(index, param, driver.getModifier()));
		pluginTrackLayer.setDevice(fxDevice);
		for (int i = 0; i < 8; i++) {
			pluginTrackLayer.addBinding(i, fxDevice.getParameter(i), RingDisplayType.FILL_LR, false);
		}
	}

	private void initEqDevice() {
		final EqDevice eqDevice = driver.getEqDevice();
		eqTrackLayer.attachDevice(eqDevice);
		eqTrackLayer.addDisplayBinding(mainDisplay);
		final List<ParameterPage> bands = eqDevice.getEqBands();
		for (int i = 0; i < 8; i++) {
			eqTrackLayer.addBinding(i, bands.get(i), false,
					(pindex, pslot) -> eqDevice.handleResetInvoked(pindex, driver.getModifier()));
		}
	}

	public void fullHardwareUpdate() {
		mainDisplay.refreshDisplay();
		for (final MotorFader fader : motorFaderDest) {
			fader.refresh();
		}

		for (final RingDisplay ringDisplay : ringDisplays) {
			ringDisplay.refresh();
		}

		for (int i = 0; i < lightStatusMap.length; i++) {
			if (lightStatusMap[i] >= 0) {
				midiOut.sendMidi(Midi.NOTE_ON, i, lightStatusMap[i]);
			}
		}
	}

	private HardwareButton createEncoderButon(final int index) {
		final HardwareButton button = driver.getSurface()
				.createHardwareButton("ENCODER_PRESS_" + sectionIndex + "_" + index);
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 32 + index));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, 32 + index));
		return button;
	}

	public LcdDisplay getMainDisplay() {
		return mainDisplay;
	}

	Layers getLayers() {
		return driver.getLayers();
	}

	MotorFader getMotorFader(final int index) {
		return motorFaderDest[index];
	}

	RelativeHardwareKnob getEncoder(final int index) {
		return encoders[index];
	}

	HardwareButton getEncoderPress(final int index) {
		return encoderPress[index];
	}

	AbsoluteHardwareKnob getVolumeFader(final int index) {
		return volumeKnobs[index];
	}

	RingDisplay getRingDisplay(final int index) {
		return ringDisplays[index];
	}

	DisplayValueTarget getValueTarget(final int index) {
		return valueTargets[index];
	}

	DisplayNameTarget getNameTarget(final int index) {
		return nameTargets[index];
	}

	DisplayStringValueBinding getVolumeDisplayBinding(final int index) {
		final StringValue displayValue = channels[index].volume().displayedValue();
		return volumeBindings.getInit(DisplayStringValueBinding.class, index,
				() -> new DisplayStringValueBinding(displayValue, valueTargets[index]));
	}

	FaderBinding getVolumeFaderBinding(final int index) {
		return volumeBindings.getInit(FaderBinding.class, index,
				() -> new FaderBinding(channels[index].volume(), motorFaderDest[index]));
	}

	RingDisplayBinding getVolumeRingDisplayBinding(final int index) {
		return volumeBindings.getInit(RingDisplayBinding.class, index,
				() -> new RingDisplayBinding(channels[index].volume(), ringDisplays[index], RingDisplayType.FILL_LR));
	}

	AbsoluteHardwareControlBinding getVolumeToFaderBinding(final int index) {
		return volumeBindings.getInit(AbsoluteHardwareControlBinding.class, index,
				() -> new AbsoluteHardwareControlBinding(volumeKnobs[index], channels[index].volume()));
	}

	RelativeHardwareControlToRangedValueBinding getVolumeToEncoderBindings(final int index) {
		return volumeBindings.getInit(RelativeHardwareControlToRangedValueBinding.class, index,
				() -> new RelativeHardwareControlToRangedValueBinding(encoders[index], channels[index].volume()));
	}

	DisplayNameBinding getChannelNameBinding(final int index) {
		return volumeBindings.getInit(DisplayNameBinding.class, index,
				() -> new DisplayNameBinding(channels[index].name(), nameTargets[index]));
	}

	TouchFaderBinding getVolumeTouchBinding(final int index) {
		return volumeBindings.getInit(TouchFaderBinding.class, index, () -> new TouchFaderBinding(faderTouch[index],
				createAction(() -> handleTouchPressed(channels[index].volume()))));
	}

	TouchFaderBinding createTouchFaderBinding(final int index, final Parameter param) {
		return new TouchFaderBinding(faderTouch[index], createAction(() -> handleTouchPressed(param)));
	}

	private void handleTouchPressed(final Parameter param) {
		if (driver.getModifier().isShiftSet()) {
			param.reset();
		}
	}

	ButtonBinding getVolumeResetBinding(final int index) {
		return volumeBindings.getInit(ButtonBinding.class, index,
				() -> new ButtonBinding(encoderPress[index], createAction(() -> channels[index].volume().reset())));
	}

	HardwareActionBindable createAction(final Runnable action) {
		return driver.getHost().createAction(action, null);
	}

	protected void setUpChannelControl(final int index, final Track channel) {
		final HardwareButton muteButton = createLightButton("MUTE", index, NoteOnAssignment.MUTE_BASE.getNoteNo(),
				channel.mute());
		final Layer mainLayer = driver.getMainLayer();
		final Project project = driver.getProject();
		final Application application = driver.getApplication();

		this.channels[index] = channel;

		mainLayer.bindPressed(muteButton, () -> channel.mute().toggle());

		final HardwareButton armButton = createLightButton("ARM", index, NoteOnAssignment.REC_BASE.getNoteNo(),
				channel.arm());
		mainLayer.bindPressed(armButton, () -> channel.arm().toggle());

		final HardwareButton soloButton = createLightButton("SOLO", index, NoteOnAssignment.SOLO_BASE.getNoteNo(),
				channel.solo());
		mainLayer.bindPressed(soloButton, () -> handleSoloAction(project, channel));

		final HardwareButton selectButton = createLightButton("SELECT", index,
				NoteOnAssignment.SELECT_BASE.getNoteNo());
		channel.addIsSelectedInMixerObserver(v -> {
			final OnOffHardwareLight led = (OnOffHardwareLight) selectButton.backgroundLight();
			led.isOn().setValue(v);
		});
		mainLayer.bindPressed(selectButton, () -> handleTrackSelection(channel, application));

		channel.exists().markInterested();
//		channel.trackType().addValueObserver(v -> {
//			final TrackType type = TrackType.toType(v);
//		});
		channel.addVuMeterObserver(14, -1, true, v -> {
			midiOut.sendMidi(Midi.CHANNEL_AT, index << 4 | v, 0);
		});

		// TODO this binding doesn't go away if the param is gone
		panLayer.addBinding(index, channel.pan(), RingDisplayType.PAN_FILL, true, ChannelSection::panToString);

		final SendBank bank = channel.sendBank();
		sendLayer.addBinding(index, bank.getItemAt(0), RingDisplayType.FILL_LR, true);
		sendLayer.addBank(bank);

		// mainLayer.bindIsPressed poses a problem because it also want a background
		// light, which the fader doesn't have
		faderTouch[index].isPressed().addValueObserver(touched -> handleTouch(touched));

		channel.isActivated().markInterested();
		channel.canHoldAudioData().markInterested();
		channel.canHoldNoteData().markInterested();
	}

	private void handleTrackSelection(final Track channel, final Application application) {
		if (channel.exists().get()) {
			if (driver.getModifier().isControl()) {
				channel.deleteObject();
			} else if (driver.getModifier().isAlt()) {
				channel.stop();
			} else if (driver.getModifier().isOption()) {
				application.navigateIntoTrackGroup(channel);
			} else {
				channel.selectInMixer();
			}
		} else {
			if (driver.getModifier().isShift()) {
				application.createAudioTrack(-1);
			} else if (driver.getModifier().isSet(ModifierValueObject.ALT)) {
				application.createEffectTrack(-1);
			} else {
				application.createInstrumentTrack(-1);
			}
		}
	}

	public static String panToString(final double v) {
		final int intv = (int) (v * 100);
		if (intv == 50) {
			return "  C";
		} else if (intv < 50) {
			return " " + (50 - intv) + "L";
		}
		return " " + (intv - 50) + "R";
	}

	public void handleSoloAction(final Project project, final Track channel) {
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

	public void handleTouch(final boolean touched) {
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

	public void clearAll() {
		mainDisplay.clearAll();
	}

	public void exitMessage() {
		mainDisplay.exitMessage();
	}

	public void notifyModeAdvance() {
		switch (driver.getVpotMode()) {
		case EQ:
			final boolean hasEq = driver.getEqDevice().exists().get();
			if (!hasEq) {
				final InsertionPoint ip = cursorTrack.endOfDeviceChainInsertionPoint();
				ip.insertBitwigDevice(Devices.EQ_PLUS.getUuid());
			} else {
				eqTrackLayer.navigateLeftRight(1);
			}
			break;
		case PLUGIN:
			pluginTrackLayer.navigateLeftRight(1);
			break;
		case INSTRUMENT:
			instrumentTrackLayer.navigateLeftRight(1);
			break;
		default:
		}
	}

	private void changeTrackMode(final boolean v, final VPotMode vPotMode) {
		if (vPotMode.getAssign() == Assign.BOTH) {
			notifyModeChange(vPotMode);
		}
	}

	public void notifyModeChange(final VPotMode mode) {
		FlippableLayer newLayer = null;
		switch (mode) {
		case EQ:
			if (type == SectionType.MAIN) {
				newLayer = eqTrackLayer;
				toEqMode();
			}
			break;
		case INSTRUMENT:
			if (type == SectionType.MAIN) {
				newLayer = instrumentTrackLayer;
				toModeDevice(driver.getInstrumentDevice());
			}
			break;
		case PAN:
			newLayer = panLayer;
			break;
		case PLUGIN:
			if (type == SectionType.MAIN) {
				newLayer = pluginTrackLayer;
				toModeDevice(driver.getPluginDevice());
			}
			break;
		case SEND:
			if (type != SectionType.MAIN) {
				newLayer = sendLayer;
			} else if (driver.getTrackChannelMode().get()) {
				newLayer = sendTrackLayer;
			} else {
				newLayer = sendLayer;
			}
			break;
		default:
			break;
		}
		switchToLayer(newLayer);
	}

	private void toModeDevice(final DeviceTracker deviceTracker) {
		if (deviceTracker.exists()) {
			deviceTracker.getDevice().selectInEditor();
			driver.getCursorDevice().selectDevice(deviceTracker.getDevice());
		}
	}

	private void toEqMode() {
		final EqDevice eqDevice = driver.getEqDevice();
		final boolean hasEq = eqDevice.exists().get();
		if (hasEq) {
			driver.getCursorDevice().selectDevice(eqDevice.getDevice());
			eqDevice.getDevice().selectInEditor();
			eqDevice.triggerUpdate();
		}
	}

	private void switchToLayer(final FlippableLayer newLayer) {
		if (newLayer != null && newLayer != currentLayer) {
			currentLayer.deactivate();
			currentLayer = newLayer;
			currentLayer.activate();
		}
	}

	private HardwareButton createTouchButton(final String name, final int index) {
		final HardwareSurface surface = driver.getSurface();
		final int notNr = NoteOnAssignment.TOUCH_VOLUME.getNoteNo() + index;
		final HardwareButton button = surface.createHardwareButton(name + "_" + sectionIndex + "_" + index);
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, notNr));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, notNr));
		return button;
	}

	private HardwareButton createLightButton(final String name, final int index, final int notNr) {
		return createLightButton(name, index, notNr, null);
	}

	private HardwareButton createLightButton(final String name, final int index, final int notNr,
			final BooleanValue value) {
		final HardwareSurface surface = driver.getSurface();
		final HardwareButton button = surface.createHardwareButton(name + "_" + sectionIndex + "_" + index);
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, notNr + index));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, notNr + index));
		final OnOffHardwareLight led = surface
				.createOnOffHardwareLight(name + "BUTTON_LED" + "_" + sectionIndex + "_" + index);
		button.setBackgroundLight(led);
		led.onUpdateHardware(() -> {
			sendLedLightStatus(notNr + index, led.isOn().currentValue() ? 127 : 0);
		});
		if (value != null) {
			value.addValueObserver(v -> led.isOn().setValue(v));
		}
		return button;
	}

	public void resetLeds() {
		final NoteOnAssignment[] nv = NoteOnAssignment.values();
		for (final NoteOnAssignment noteOnAssignment : nv) {
			sendLedLightStatus(noteOnAssignment.getNoteNo(), 0);
		}
	}

	private void sendLedLightStatus(final int noteNr, final int value) {
		lightStatusMap[noteNr] = value;
		midiOut.sendMidi(Midi.NOTE_ON, noteNr, value);
	}

	public void resetFaders() {
		for (final MotorFader fader : motorFaderDest) {
			fader.sendValue(0);
		}
	}

	int cx = 0;

	public void exprimental(final int inc) {
		final int nx = cx + inc;
		if (nx < 0) {
			cx = 127;
		} else if (nx > 127) {
			cx = 0;
		} else {
			cx = nx;
		}
		// RemoteConsole.out.println(" EX P {} c=[{}]", cx, (char) cx);
		mainDisplay.sendChar(0, (char) cx);
	}

	public void applyVuMode(final VuMode mode) {
		mainDisplay.appyVuMode(mode);
	}

	public void navigateLeftRight(final int direction) {
		currentLayer.navigateLeftRight(direction);
	}

	public void navigateUpDown(final int direction) {
		currentLayer.navigateUpDown(direction);
	}

}
