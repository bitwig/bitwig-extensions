package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.framework.Layer;

class MixerLayerConfiguration extends LayerConfiguration {

	ParamElement encoderAssign;

	public MixerLayerConfiguration(final String name, final MixControl mixControl, final ParamElement encoderAssign) {
		super(name, mixControl);
		this.encoderAssign = encoderAssign;
	}

	@Override
	public Layer getFaderLayer() {
		final boolean flipped = this.mixControl.driver.getFlipped().get();
		final MixerLayerGroup activeGroup = this.mixControl.getActiveMixGroup();
		if (flipped) {
			return activeGroup.getFaderLayer(encoderAssign);
		} else {
			return activeGroup.getFaderLayer(ParamElement.VOLUME);
		}
	}

	@Override
	public EncoderLayer getEncoderLayer() {
		final boolean flipped = this.mixControl.driver.getFlipped().get();
		final MixerLayerGroup activeGroup = this.mixControl.getActiveMixGroup();

		if (flipped) {
			return activeGroup.getEncoderLayer(ParamElement.VOLUME);
		} else {
			return activeGroup.getEncoderLayer(encoderAssign);
		}
	}

	@Override
	public Layer getButtonLayer() {
		return this.mixControl.getActiveMixGroup().getMixerButtonLayer();
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		final MixerLayerGroup activeGroup = this.mixControl.getActiveMixGroup();
		if (which == 0) {
			return activeGroup.getDisplayConfiguration(encoderAssign);
		}
		return activeGroup.getDisplayConfiguration(ParamElement.VOLUME);
	}

}