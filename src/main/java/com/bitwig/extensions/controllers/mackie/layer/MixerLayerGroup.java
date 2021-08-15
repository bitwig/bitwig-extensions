package com.bitwig.extensions.controllers.mackie.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MixerLayerGroup {

	private final List<Bank<? extends Parameter>> sendBankList = new ArrayList<>();
	private final Layer volumeFaderLayer;
	private final EncoderLayer volumeEncoderLayer;
	private final Layer panFaderLayer;
	private final EncoderLayer panEncoderLayer;
	private final Layer mixerButtonLayer;
	private final Layer sendFaderLayer;
	private final EncoderLayer sendEncoderLayer;
	private final MixControl control;
	private final DisplayLayer volumeDisplayConfiguration;
	private final DisplayLayer panDisplayConfiguration;
	private final DisplayLayer sendDisplayConfiguration;
	private final DisplayLayer sendDisplayAltConfiguration;
	private DisplayLayer activeSendDisplayConfig;

	public MixerLayerGroup(final String name, final MixControl control) {
		final int sectionIndex = control.getHwControls().getSectionIndex();
		this.control = control;
		final Layers layers = this.control.getDriver().getLayers();
		mixerButtonLayer = new Layer(layers, name + "_MIXER_BUTTON_LAYER_" + sectionIndex);

		volumeFaderLayer = new Layer(layers, name + "_VOLUME_FADER_LAYER_" + sectionIndex);
		volumeEncoderLayer = new EncoderLayer(control, name + "_VOLUME_ENCODER_LAYER_" + sectionIndex);

		panFaderLayer = new Layer(layers, name + "_PAN_FADER_LAYER_" + sectionIndex);
		panEncoderLayer = new EncoderLayer(control, name + "_PAN_ENCODER_LAYER_" + sectionIndex);

		sendFaderLayer = new Layer(layers, name + "_SEND_FADER_LAYER_" + sectionIndex);
		sendEncoderLayer = new EncoderLayer(control, name + "_SEN_ENCODER_LAYER_" + sectionIndex);

		volumeDisplayConfiguration = new DisplayLayer("MixVolume", control);
		panDisplayConfiguration = new DisplayLayer("MixPan", control);
		sendDisplayConfiguration = new DisplayLayer("MixSend", control);
		sendDisplayAltConfiguration = new DisplayLayer("MixSendAlt", control);
		activeSendDisplayConfig = sendDisplayConfiguration;
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
			return activeSendDisplayConfig;
		default:
			return volumeDisplayConfiguration;
		}
	}

	public EncoderLayer getEncoderLayer(final ParamElement type) {
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

	public boolean notifyDisplayName(final boolean pressed) {
		if (!pressed) {
			return false;
		}
		if (sendDisplayConfiguration.isActive()) {
			activeSendDisplayConfig = sendDisplayAltConfiguration;
			return true;
		} else if (sendDisplayAltConfiguration.isActive()) {
			activeSendDisplayConfig = sendDisplayConfiguration;
			return true;
		}
		return false;
	}

	public Layer getMixerButtonLayer() {
		return mixerButtonLayer;
	}

	public void navigateHorizontally(final int direction) {
		for (final Bank<?> bank : sendBankList) {
			bank.scrollBy(direction);
		}
	}

	public void init(final DrumPadBank drumPadBank) {
		final int sectionIndex = control.getHwControls().getSectionIndex();
		for (int i = 0; i < 8; i++) {
			final int trackIndex = i + sectionIndex * 8;
			setUpChannelControl(i, drumPadBank.getItemAt(trackIndex));
		}
	}

	public void init(final TrackBank trackBank) {
		final int sectionIndex = control.getHwControls().getSectionIndex();
		for (int i = 0; i < 8; i++) {
			final int trackIndex = i + sectionIndex * 8;
			setUpChannelControl(i, trackBank.getItemAt(trackIndex));
		}
	}

	protected void setUpChannelControl(final int index, final Channel channel) {
		final MixerSectionHardware hwControls = control.getHwControls();
		channel.exists().markInterested();

		channel.addVuMeterObserver(14, -1, true, value -> {
			if (volumeEncoderLayer.isActive() || volumeFaderLayer.isActive()) {
				hwControls.sendVuUpdate(index, value);
			}
		});

		setControlLayer(index, channel.volume(), volumeFaderLayer, volumeEncoderLayer, RingDisplayType.FILL_LR_0);
		setControlLayer(index, channel.pan(), panFaderLayer, panEncoderLayer, RingDisplayType.PAN_FILL);
		final SendBank sendBank = channel.sendBank();
		final Send focusSendItem = sendBank.getItemAt(0);

		// focusSendItem.name().addValueObserver(itemName ->
		// RemoteConsole.out.println("ITEM NAME {}", itemName));

		setControlLayer(index, focusSendItem, sendFaderLayer, sendEncoderLayer, RingDisplayType.FILL_LR);
		sendBankList.add(sendBank);

		volumeDisplayConfiguration.bindDisplayParameterValue(index, channel.volume(),
				s -> StringUtil.condenseVolumenValue(s, 7));
		panDisplayConfiguration.bindParameterValue(index, channel.pan(), StringUtil::panToString);
		sendDisplayConfiguration.bindDisplayParameterValue(index, focusSendItem,
				s -> StringUtil.condenseVolumenValue(s, 7));
		sendDisplayAltConfiguration.bindDisplayParameterValue(index, focusSendItem,
				s -> StringUtil.condenseVolumenValue(s, 7));

		final TrackNameValueHandler trackNameHandler = new TrackNameValueHandler(channel.name());
		final TrackNameValueHandler sendNameHandler = new TrackNameValueHandler(focusSendItem.name());

		volumeDisplayConfiguration.bindName(index, trackNameHandler);
		panDisplayConfiguration.bindName(index, trackNameHandler);
		sendDisplayConfiguration.bindName(index, trackNameHandler);
		sendDisplayAltConfiguration.bindName(index, sendNameHandler);

		hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SOLO_INDEX, channel.solo(),
				() -> control.handleSoloAction(channel));
		hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.MUTE_INDEX, channel.mute(), () -> {
			channel.mute().toggle();
		});
		if (channel instanceof Track) {
			final Track track = (Track) channel;
			hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.REC_INDEX, track.arm(), () -> {
				track.arm().toggle();
			});
			final BooleanValueObject selectedInMixer = new BooleanValueObject();
			channel.addIsSelectedInEditorObserver(v -> selectedInMixer.set(v));
			hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer,
					() -> control.handleTrackSelection(track));
		} else if (channel instanceof DrumPad) {
			final DrumPad pad = (DrumPad) channel;
			final BooleanValueObject selectedInMixer = new BooleanValueObject();
			pad.addIsSelectedInMixerObserver(v -> selectedInMixer.set(v));
			hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer,
					() -> pad.selectInMixer());
		}
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
