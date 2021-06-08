package com.bitwig.extensions.controllers.mackie;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayNameBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingDisplayBinding;
import com.bitwig.extensions.controllers.mackie.target.DisplayNameTarget;
import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;
import com.bitwig.extensions.controllers.mackie.target.MotorFader;
import com.bitwig.extensions.controllers.mackie.target.RingDisplay;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

public class ChannelSection {

	private final MidiIn midiIn;
	private final MidiOut midiOut;

	final AbsoluteHardwareKnob[] volumeKnobs = new AbsoluteHardwareKnob[8];
	final RelativeHardwareKnob[] encoders = new RelativeHardwareKnob[8];
	final MotorFader[] motorFaderDest = new MotorFader[8];
	final RingDisplay[] ringDisplays = new RingDisplay[8];
	final DisplayValueTarget[] valueTargets = new DisplayValueTarget[8];
	final DisplayNameTarget[] nameTargets = new DisplayNameTarget[8];
	private final DisplayNameBinding[] channelNameBindings = new DisplayNameBinding[8];

	final DisplayValueBinding[] volumeDisplayBindings = new DisplayValueBinding[8];
	final FaderBinding[] volumeFaderBindings = new FaderBinding[8];
	final RingDisplayBinding[] volumeRingDisplayBindings = new RingDisplayBinding[8];
	final AbsoluteHardwareControlBinding[] volumeToFaderBindings = new AbsoluteHardwareControlBinding[8];
	final RelativeHardwareControlToRangedValueBinding[] volumeToEncoderBindings = new RelativeHardwareControlToRangedValueBinding[8];
	final private HardwareButton[] encoderPress = new HardwareButton[8];

	private final List<FlippableLayer> layers = new ArrayList<FlippableLayer>();
	private final Track[] channel = new Track[8];

	private final MackieMcuProExtension driver;
	private final int sectionIndex;
	private final LcdDisplay mainDisplay;

	private final FlippableLayer panLayer;
	private final FlippableLayer sendLayer;
	private FlippableLayer currentLayer;

	private final BooleanValueObject fadersTouched = new BooleanValueObject();
	private int touchCount = 0;
	private final SectionType type;
	private final FlippableLayer sendPtLayer;

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
		sendLayer = new FlippableLayer(this, "SEND_" + sectionIndex + "_LAYER");
		sendPtLayer = new FlippableLayer(this, "SEND_PT_" + sectionIndex + "_LAYER");
		layers.add(panLayer);
		layers.add(sendLayer);
		layers.add(sendPtLayer);

		final HardwareSurface surface = driver.getSurface();
		for (int i = 0; i < 8; i++) {
			final AbsoluteHardwareKnob knob = surface
					.createAbsoluteHardwareKnob("VOLUME_FADER_" + sectionIndex + "_" + i);
			volumeKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(i));

			motorFaderDest[i] = new MotorFader(midiOut, i);
			ringDisplays[i] = new RingDisplay(midiOut, i);

			final RelativeHardwareKnob encoder = surface
					.createRelativeHardwareKnob("PAN_KNOB" + sectionIndex + "_" + i);
			encoders[i] = encoder;
			encoderPress[i] = createEncoderButon(i);
			encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBitCCValueMatcher(0x0, 0x10 + i, 200));
			encoder.setStepSize(1 / 128.0);
		}
		mainDisplay = new LcdDisplay(driver, midiOut, type);

		panLayer.activate();
		currentLayer = panLayer;
		driver.getFlipped().addValueObserver(flipped -> {
			layers.forEach(layer -> layer.setFlipped(flipped));
		});

		fadersTouched.addValueObserver(touched -> {
			layers.forEach(layer -> layer.setTouched(touched));
		});
	}

	private HardwareButton createEncoderButon(final int index) {
		final HardwareButton button = driver.getSurface()
				.createHardwareButton("ENCODER_PRESS_" + sectionIndex + "_" + index);
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 32 + index));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, 32 + index));
		return button;
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

	AbsoluteHardwareKnob getVolumeFader(final int index) {
		return volumeKnobs[index];
	}

	public HardwareButton getEncoderPress(final int index) {
		return encoderPress[index];
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

	DisplayValueBinding getVolumeDisplayBinding(final int index) {
		return volumeDisplayBindings[index];
	}

	FaderBinding getVolumeFaderBinding(final int index) {
		return volumeFaderBindings[index];
	}

	RingDisplayBinding getVolumeRingDisplayBinding(final int index) {
		return volumeRingDisplayBindings[index];
	}

	AbsoluteHardwareControlBinding getVolumeToFaderBinding(final int index) {
		return volumeToFaderBindings[index];
	}

	RelativeHardwareControlToRangedValueBinding getVolumeToEncoderBindings(final int index) {
		return volumeToEncoderBindings[index];
	}

	DisplayNameBinding getChannelNameBinding(final int index) {
		return channelNameBindings[index];
	}

	void getVolumeResetBinding(final int index) {
		// channel[index];

	}

	protected void setUpChannelControl(final int index, final Track channel) {
		final HardwareButton muteButton = createLightButton("MUTE", index, NoteOnAssignment.MUTE_BASE.getNoteNo(),
				channel.mute());
		final Layer mainLayer = driver.getMainLayer();
		final Project project = driver.getProject();
		final Application application = driver.getApplication();

		this.channel[index] = channel;
		mainLayer.bindPressed(muteButton, () -> channel.mute().toggle());

		final HardwareButton armButton = createLightButton("ARM", index, NoteOnAssignment.REC_BASE.getNoteNo(),
				channel.arm());
		mainLayer.bindPressed(armButton, () -> channel.arm().toggle());

		final HardwareButton soloButton = createLightButton("SOLO", index, NoteOnAssignment.SOLO_BASE.getNoteNo(),
				channel.solo());
		mainLayer.bindPressed(soloButton, () -> {
			if (!channel.exists().get()) {
				return;
			}
			if (channel.solo().get()) {
				channel.solo().set(false);
			} else {
				project.unsoloAll();
				channel.solo().set(true);
			}
		});

		final HardwareButton selectButton = createLightButton("SELECT", index,
				NoteOnAssignment.SELECT_BASE.getNoteNo());
		channel.addIsSelectedInMixerObserver(v -> {
			final OnOffHardwareLight led = (OnOffHardwareLight) selectButton.backgroundLight();
			led.isOn().setValue(v);
		});
		mainLayer.bindPressed(selectButton, () -> {
			if (channel.exists().get()) {
				channel.selectInMixer();
			} else {
				if (driver.getModifier().isShiftSet()) {
					application.createAudioTrack(-1);
				} else {
					application.createInstrumentTrack(-1);
				}
			}
		});

		channel.exists().markInterested();
//		channel.trackType().addValueObserver(v -> {
//			final TrackType type = TrackType.toType(v);
//		});
		channel.addVuMeterObserver(14, -1, true, v -> {
			midiOut.sendMidi(Midi.CHANNEL_AT, index << 4 | v, 0);
		});

		volumeToFaderBindings[index] = new AbsoluteHardwareControlBinding(getVolumeFader(index), channel.volume());
		volumeToEncoderBindings[index] = new RelativeHardwareControlToRangedValueBinding(getEncoder(index),
				channel.volume());

		valueTargets[index] = new DisplayValueTarget(mainDisplay, index);
		nameTargets[index] = new DisplayNameTarget(mainDisplay, index);
		channelNameBindings[index] = new DisplayNameBinding(channel.name(), nameTargets[index]);
		volumeDisplayBindings[index] = new DisplayValueBinding(channel.volume(), valueTargets[index]);
		volumeFaderBindings[index] = new FaderBinding(channel.volume(), motorFaderDest[index]);
		volumeRingDisplayBindings[index] = new RingDisplayBinding(channel.volume(), ringDisplays[index],
				RingDisplayType.FILL_LR);

		panLayer.addBinding(index, channel.pan(), RingDisplayType.PAN_FILL, true);
		sendPtLayer.addBinding(index, channel.sendBank().getItemAt(0), RingDisplayType.FILL_LR, true);

		final HardwareButton touchButton = createTouchButton("FADER_TOUCH", index);
		touchButton.isPressed().addValueObserver(v -> {
			if (v) {
				touchCount++;
			} else if (touchCount > 0) {
				touchCount--;
			}
			if (touchCount > 0 && !fadersTouched.get()) {
				fadersTouched.setValue(true);
			} else if (touchCount == 0 && fadersTouched.get()) {
				fadersTouched.setValue(false);
			}
		});

		channel.isActivated().markInterested();
		channel.canHoldAudioData().markInterested();
		channel.canHoldNoteData().markInterested();
	}

	public void clearAll() {
		mainDisplay.clearAll();
	}

	public void notifyModeAdvance() {
		FlippableLayer newLayer = null;
		if (type == SectionType.MAIN) {
			if (driver.getVpotMode() == VPotMode.SEND) {
				if (this.currentLayer == sendLayer) {
					newLayer = sendPtLayer;
				} else if (this.currentLayer == sendPtLayer) {
					newLayer = sendLayer;
				}
			}
		}
		switchToLayer(newLayer);
	}

	public void notifyModeChange(final VPotMode mode) {
		FlippableLayer newLayer = null;
		switch (mode) {
		case EQ:
			break;
		case INSTRUMENT:
			break;
		case PAN:
			newLayer = panLayer;
			break;
		case PLUGIN:
			break;
		case SEND:
			newLayer = sendPtLayer;
			break;
		case TRACK:
			break;
		default:
			break;
		}
		switchToLayer(newLayer);
	}

	public void switchToLayer(final FlippableLayer newLayer) {
		if (newLayer != null && newLayer != currentLayer) {
			currentLayer.deactivate();
			currentLayer = newLayer;
			currentLayer.activate();
		}
	}

	public void initChannelControl(final TrackBank mixerTrackBank, final CursorTrack cursorTrack) {
		for (int i = 0; i < 8; i++) {
			final int trackIndex = i + sectionIndex * 8;
			setUpChannelControl(i, mixerTrackBank.getItemAt(trackIndex));
		}
		if (type == SectionType.MAIN) {
			final SendBank sendBank = cursorTrack.sendBank();
			for (int i = 0; i < 8; i++) {
				setUpSendControl(i, sendBank.getItemAt(i));
			}
		}
		final ControllerHost host = driver.getHost();
		final DeviceMatcher eq5Matcher = host
				.createBitwigDeviceMatcher(UUID.fromString("e4815188-ba6f-4d14-bcfc-2dcb8f778ccb"));
		final DeviceBank deviceBank = cursorTrack.createDeviceBank(1);
		deviceBank.setDeviceMatcher(eq5Matcher);
		deviceBank.getDevice(0).exists().addValueObserver(v -> {
			RemoteConsole.out.println(" W {}", v);
		});
		deviceBank.getDevice(0).name().addValueObserver(v -> {
			RemoteConsole.out.println(" NAME {}", v);
		});

	}

	private void setUpSendControl(final int index, final Send senditem) {
		sendLayer.addBinding(index, senditem, RingDisplayType.FILL_LR, false);
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
			midiOut.sendMidi(Midi.NOTE_ON, notNr + index, led.isOn().currentValue() ? 127 : 0);
		});
		if (value != null) {
			value.addValueObserver(v -> led.isOn().setValue(v));
		}
		return button;
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
		RemoteConsole.out.println(" EX P {} c=[{}]", cx, (char) cx);
		mainDisplay.sendChar(0, (char) cx);
	}

	public void applyVuMode(final VuMode mode) {
		mainDisplay.appyVuMode(mode);
	}

}
