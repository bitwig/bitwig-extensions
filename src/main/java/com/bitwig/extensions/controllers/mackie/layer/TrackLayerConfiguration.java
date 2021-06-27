package com.bitwig.extensions.controllers.mackie.layer;

import java.util.function.BiConsumer;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

class TrackLayerConfiguration extends LayerConfiguration {

	private final Layer faderLayer;
	private final Layer encoderLayer;
	private final DisplayLayer displayLayer;
	private DeviceManager deviceManager;

	public TrackLayerConfiguration(final String name, final MixControl mixControl) {
		super(name, mixControl);
		final Layers layers = this.mixControl.getDriver().getLayers();
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		faderLayer = new Layer(layers, name + "_FADER_LAYER_" + sectionIndex);
		encoderLayer = new Layer(layers, name + "_ENCODER_LAYER_" + sectionIndex);
		displayLayer = new DisplayLayer(name, this.mixControl);
	}

	@Override
	public DeviceManager getDeviceManager() {
		return deviceManager;
	}

	public void setDeviceManager(final DeviceManager deviceManager) {
		this.deviceManager = deviceManager;
		this.deviceManager.getCursorOnDevice().markInterested();
	}

	@Override
	public Layer getFaderLayer() {
		final boolean flipped = this.mixControl.driver.getFlipped().get();
		final boolean isMixerGlobal = this.mixControl.driver.getGlobalViewActive().get();
		if (flipped) {
			return faderLayer;
		}
		if (isMixerGlobal) {
			return this.mixControl.globalGroup.getFaderLayer(ParamElement.VOLUME);
		}
		return this.mixControl.mainGroup.getFaderLayer(ParamElement.VOLUME);
	}

	@Override
	public Layer getEncoderLayer() {
		final boolean flipped = this.mixControl.driver.getFlipped().get();
		final boolean isMixerGlobal = this.mixControl.driver.getGlobalViewActive().get();
		if (flipped) {
			if (isMixerGlobal) {
				return this.mixControl.globalGroup.getEncoderLayer(ParamElement.VOLUME);
			}
			return this.mixControl.mainGroup.getEncoderLayer(ParamElement.VOLUME);
		}
		return encoderLayer;
	}

	@Override
	public Layer getButtonLayer() {
		if (mixControl.driver.getGlobalViewActive().get()) {
			return mixControl.globalGroup.getMixerButtonLayer();
		}
		return mixControl.mainGroup.getMixerButtonLayer();
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		if (which == 0) {
			return displayLayer;
		}
		if (mixControl.driver.getGlobalViewActive().get()) {
			return mixControl.globalGroup.getDisplayConfiguration(ParamElement.VOLUME);
		}
		return mixControl.mainGroup.getDisplayConfiguration(ParamElement.VOLUME);
	}

	public void addBinding(final int index, final ParameterPage parameter,
			final BiConsumer<Integer, ParameterPage> resetAction) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		encoderLayer.addBinding(parameter.getRelativeEncoderBinding(hwControls.getEncoder(index)));
		encoderLayer.addBinding(parameter.createRingBinding(hwControls.getRingDisplay(index)));
		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(index),
				hwControls.createAction(() -> resetAction.accept(index, parameter))));

		faderLayer.addBinding(parameter.getFaderBinding(hwControls.getVolumeFader(index)));
		faderLayer.addBinding(parameter.createFaderBinding(hwControls.getMotorFader(index)));

		displayLayer.bind(index, parameter);
		parameter.resetBindings();
	}

	public void addBinding(final int index, final Parameter parameter, final RingDisplayType type) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();

		faderLayer.addBinding(hwControls.createMotorFaderBinding(index, parameter));
		faderLayer.addBinding(hwControls.createFaderParamBinding(index, parameter));
		faderLayer.addBinding(hwControls.createFaderTouchBinding(index, () -> {
			if (mixControl.getModifier().isShift()) {
				parameter.reset();
			}
		}));
		encoderLayer.addBinding(hwControls.createEncoderPressBinding(index, parameter));
		encoderLayer.addBinding(hwControls.createEncoderToParamBinding(index, parameter));
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, parameter, type));
		displayLayer.bindName(index, parameter.name());
		displayLayer.bindParameterValue(index, parameter);
	}

}