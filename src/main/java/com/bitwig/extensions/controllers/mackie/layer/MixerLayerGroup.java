package com.bitwig.extensions.controllers.mackie.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MixerLayerGroup {

	private final List<Bank<? extends Parameter>> bankList = new ArrayList<>();
	private final Layer volumeFaderLayer;
	private final Layer volumeEncoderLayer;
	private final Layer panFaderLayer;
	private final Layer panEncoderLayer;
	private final Layer mixerButtonLayer;
	private final Layer sendFaderLayer;
	private final Layer sendEncoderLayer;
	private final MixControl control;
	private final DisplayLayer volumeDisplayConfiguration;
	private final DisplayLayer panDisplayConfiguration;
	private final DisplayLayer sendDisplayConfiguration;

	public MixerLayerGroup(final String name, final MixControl control) {
		final int sectionIndex = control.getHwControls().getSectionIndex();
		this.control = control;
		final Layers layers = this.control.getDriver().getLayers();
		mixerButtonLayer = new Layer(layers, name + "_MIXER_BUTTON_LAYER_" + sectionIndex);

		volumeFaderLayer = new Layer(layers, name + "_VOLUME_FADER_LAYER_" + sectionIndex);
		volumeEncoderLayer = new Layer(layers, name + "_VOLUME_ENCODER_LAYER_" + sectionIndex);

		panFaderLayer = new Layer(layers, name + "_PAN_FADER_LAYER_" + sectionIndex);
		panEncoderLayer = new Layer(layers, name + "_PAN_ENCODER_LAYER_" + sectionIndex);

		sendFaderLayer = new Layer(layers, name + "_SEND_FADER_LAYER_" + sectionIndex);
		sendEncoderLayer = new Layer(layers, name + "_SEN_ENCODER_LAYER_" + sectionIndex);

		volumeDisplayConfiguration = new DisplayLayer("MixVolume", control);
		panDisplayConfiguration = new DisplayLayer("MixPan", control);
		sendDisplayConfiguration = new DisplayLayer("MixSend", control);
	}

	public DisplayLayer getPanDisplayConfiguration() {
		return panDisplayConfiguration;
	}

	public DisplayLayer getVolumeDisplayConfiguration() {
		return volumeDisplayConfiguration;
	}

	public DisplayLayer getSendDisplayConfiguration() {
		return sendDisplayConfiguration;
	}

	public Layer getFaderLayer(final ParamElement type) {
		switch (type) {
		case VOLUME:
			return volumeFaderLayer;
		case PAN:
			return panFaderLayer;
		case SENDMIXER:
			return sendFaderLayer;
		default:
			return volumeFaderLayer;
		}
	}

	public DisplayLayer getDisplayConfiguration(final ParamElement type) {
		switch (type) {
		case VOLUME:
			return volumeDisplayConfiguration;
		case PAN:
			return panDisplayConfiguration;
		case SENDMIXER:
			return sendDisplayConfiguration;
		default:
			return volumeDisplayConfiguration;
		}
	}

	public Layer getEncoderLayer(final ParamElement type) {
		switch (type) {
		case VOLUME:
			return volumeEncoderLayer;
		case PAN:
			return panEncoderLayer;
		case SENDMIXER:
			return sendEncoderLayer;
		default:
			return volumeEncoderLayer;
		}
	}

	public Layer getMixerButtonLayer() {
		return mixerButtonLayer;
	}

	public void navigateHorizontally(final int direction) {
		for (final Bank<?> bank : bankList) {
			if (direction > 0 && bank.canScrollForwards().get()) {
				bank.scrollForwards();
			} else if (direction < 0 && bank.canScrollBackwards().get()) {
				bank.scrollBackwards();
			}
		}
	}

	public void init(final TrackBank trackBank) {
		final int sectionIndex = control.getHwControls().getSectionIndex();
		for (int i = 0; i < 8; i++) {
			final int trackIndex = i + sectionIndex * 8;
			setUpChannelControl(i, trackBank.getItemAt(trackIndex));
		}
	}

	protected void setUpChannelControl(final int index, final Track channel) {
		final MixerSectionHardware hwControls = control.getHwControls();
		channel.exists().markInterested();
		hwControls.registerVuMeter(index, channel);

		final BooleanValueObject selectedInMixer = new BooleanValueObject();
		channel.addIsSelectedInEditorObserver(v -> selectedInMixer.set(v));

		setControlLayer(index, channel.volume(), volumeFaderLayer, volumeEncoderLayer, RingDisplayType.FILL_LR_0);
		setControlLayer(index, channel.pan(), panFaderLayer, panEncoderLayer, RingDisplayType.PAN_FILL);
		final SendBank bank = channel.sendBank();
		setControlLayer(index, bank.getItemAt(0), sendFaderLayer, sendEncoderLayer, RingDisplayType.FILL_LR);
		bankList.add(bank);
		bank.canScrollForwards().markInterested();
		bank.canScrollBackwards().markInterested();

		volumeDisplayConfiguration.bindDisplayParameterValue(index, channel.volume(),
				s -> StringUtil.condenseVolumenValue(s, 7));
		panDisplayConfiguration.bindParameterValue(index, channel.pan(), StringUtil::panToString);
		sendDisplayConfiguration.bindDisplayParameterValue(index, bank.getItemAt(0),
				s -> StringUtil.condenseVolumenValue(s, 7));

		final TrackNameValueHandler trackNameHandler = new TrackNameValueHandler(channel.name());

		volumeDisplayConfiguration.bindName(index, trackNameHandler);
		panDisplayConfiguration.bindName(index, trackNameHandler);
		sendDisplayConfiguration.bindName(index, trackNameHandler);

		hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.REC_INDEX, channel.arm(), () -> {
			channel.arm().toggle();
		});
		hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SOLO_INDEX, channel.solo(),
				() -> control.handleSoloAction(channel));
		hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.MUTE_INDEX, channel.mute(), () -> {
			channel.mute().toggle();
		});
		hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer,
				() -> control.handleTrackSelection(channel));
	}

	private void setControlLayer(final int index, final Parameter parameter, final Layer faderLayer,
			final Layer encoderLayer, final RingDisplayType type) {
		final MixerSectionHardware hwControls = control.getHwControls();
		faderLayer.addBinding(hwControls.createMotorFaderBinding(index, parameter));
		faderLayer.addBinding(hwControls.createFaderParamBinding(index, parameter));
		faderLayer.addBinding(hwControls.createFaderTouchBinding(index, () -> {
			if (control.getModifier().isShift()) {
				parameter.reset();
			}
		}));
		encoderLayer.addBinding(hwControls.createEncoderPressBinding(index, parameter));
		encoderLayer.addBinding(hwControls.createEncoderToParamBinding(index, parameter));
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, parameter, type));
	}

}
