package com.bitwig.extensions.controllers.mackie.layer;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingDisplayBinding;
import com.bitwig.extensions.controllers.mackie.bindings.TouchFaderBinding;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.targets.MotorFader;
import com.bitwig.extensions.controllers.mackie.targets.RingDisplay;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;

public class MixerSectionHardware {
	public static final int REC_INDEX = 0;
	public static final int SOLO_INDEX = 1;
	public static final int MUTE_INDEX = 2;
	public static final int SELECT_INDEX = 3;

	private final int[] lightStatusMap = new int[127];
	private final AbsoluteHardwareKnob[] volumeKnobs = new AbsoluteHardwareKnob[8];
	private final RelativeHardwareKnob[] encoders = new RelativeHardwareKnob[8];
	private final HardwareButton[] encoderPress = new HardwareButton[8];
	private final HardwareButton[] faderTouch = new HardwareButton[8];
	private final MotorFader[] motorFaderDest = new MotorFader[8];
	private final RingDisplay[] ringDisplays = new RingDisplay[8];
	private final HardwareButton buttonMatrix[][] = new HardwareButton[4][8];

	private final MidiIn midiIn;
	private final MidiOut midiOut;
	private final MackieMcuProExtension driver;
	private final int sectionIndex;
	private final LcdDisplay mainDisplay;

	public MixerSectionHardware(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
			final int sectionIndex, final SectionType type) {
		this.midiIn = midiIn;
		this.midiOut = midiOut;
		this.driver = driver;
		this.sectionIndex = sectionIndex;
		mainDisplay = new LcdDisplay(driver, midiOut, type);

		for (int i = 0; i < lightStatusMap.length; i++) {
			lightStatusMap[i] = -1;
		}
		initControlHardware(driver.getSurface());
		initButtonSection(driver.getSurface());
	}

	private void initButtonSection(final HardwareSurface surface) {
		for (int i = 0; i < 8; i++) {
			final HardwareButton armButton = createLightButton("ARM", i, NoteOnAssignment.REC_BASE.getNoteNo());
			final HardwareButton soloButton = createLightButton("SOLO", i, NoteOnAssignment.SOLO_BASE.getNoteNo());
			final HardwareButton muteButton = createLightButton("MUTE", i, NoteOnAssignment.MUTE_BASE.getNoteNo());
			final HardwareButton selectButton = createLightButton("SELECT", i,
					NoteOnAssignment.SELECT_BASE.getNoteNo());
			buttonMatrix[REC_INDEX][i] = armButton;
			buttonMatrix[SOLO_INDEX][i] = soloButton;
			buttonMatrix[MUTE_INDEX][i] = muteButton;
			buttonMatrix[SELECT_INDEX][i] = selectButton;
		}
	}

	private void initControlHardware(final HardwareSurface surface) {
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

//			valueTargets[i] = new DisplayValueTarget(mainDisplay, i);
//			nameTargets[i] = new DisplayNameTarget(mainDisplay, i);
		}
	}

	public void registerVuMeter(final int index, final Track channel) {
		channel.addVuMeterObserver(14, -1, true, v -> {
			midiOut.sendMidi(Midi.CHANNEL_AT, index << 4 | v, 0);
		});
	}

	public void bindButton(final Layer layer, final int index, final int buttonIndex, final BooleanSupplier param,
			final Runnable action) {
		final HardwareButton button = buttonMatrix[buttonIndex][index];
		final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();
		layer.bindPressed(button, action);
		layer.bind(param, light);
	}

	private HardwareButton createEncoderButon(final int index) {
		final HardwareButton button = driver.getSurface()
				.createHardwareButton("ENCODER_PRESS_" + sectionIndex + "_" + index);
		button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 32 + index));
		button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, 32 + index));
		return button;
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

	public LcdDisplay getMainDisplay() {
		return mainDisplay;
	}

	public MotorFader getMotorFader(final int index) {
		return motorFaderDest[index];
	}

	public RelativeHardwareKnob getEncoder(final int index) {
		return encoders[index];
	}

	public HardwareButton getEncoderPress(final int index) {
		return encoderPress[index];
	}

	public AbsoluteHardwareKnob getVolumeFader(final int index) {
		return volumeKnobs[index];
	}

	public RingDisplay getRingDisplay(final int index) {
		return ringDisplays[index];
	}

	public int getSectionIndex() {
		return sectionIndex;
	}

	public AbsoluteHardwareControlBinding createFaderParamBinding(final int index, final Parameter parameter) {
		return new AbsoluteHardwareControlBinding(volumeKnobs[index], parameter);
	}

	public TouchFaderBinding createFaderTouchBinding(final int index, final Runnable execAction) {
		return new TouchFaderBinding(faderTouch[index], createAction(execAction));
	}

	public FaderBinding createMotorFaderBinding(final int index, final Parameter param) {
		return new FaderBinding(param, motorFaderDest[index]);
	}

	public ButtonBinding createEncoderPressBinding(final int index, final Parameter param) {
		return new ButtonBinding(encoderPress[index], createAction(() -> param.reset()));
	}

	public RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final int index,
			final Parameter param) {
		return new RelativeHardwareControlToRangedValueBinding(encoders[index], param);
	}

	public RingDisplayBinding createRingDisplayBinding(final int index, final Parameter param,
			final RingDisplayType type) {
		return new RingDisplayBinding(param, ringDisplays[index], type);
	}

	HardwareActionBindable createAction(final Runnable action) {
		return driver.getHost().createAction(action, null);
	}

	public void assignFaderTouchAction(final int index, final BooleanValueChangedCallback action) {
		faderTouch[index].isPressed().addValueObserver(action);
	}

}
