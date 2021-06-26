package com.bitwig.extensions.controllers.mackie.layer;

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
		final boolean isGlobalView = this.mixControl.driver.getGlobalViewActive().get();
		if (flipped) {
			if (isGlobalView) {
				return this.mixControl.globalGroup.getFaderLayer(encoderAssign);
			} else {
				return this.mixControl.mainGroup.getFaderLayer(encoderAssign);
			}
		} else {
			if (isGlobalView) {
				return this.mixControl.globalGroup.getFaderLayer(ParamElement.VOLUME);
			} else {
				return this.mixControl.mainGroup.getFaderLayer(ParamElement.VOLUME);
			}
		}
	}

	@Override
	public Layer getEncoderLayer() {
		final boolean flipped = this.mixControl.driver.getFlipped().get();
		final boolean isGlobalView = this.mixControl.driver.getGlobalViewActive().get();

		if (flipped) {
			if (isGlobalView) {
				return this.mixControl.globalGroup.getEncoderLayer(ParamElement.VOLUME);
			} else {
				return this.mixControl.mainGroup.getEncoderLayer(ParamElement.VOLUME);
			}
		} else {
			if (isGlobalView) {
				return this.mixControl.globalGroup.getEncoderLayer(encoderAssign);
			} else {
				return this.mixControl.mainGroup.getEncoderLayer(encoderAssign);
			}
		}
	}

	@Override
	public Layer getButtonLayer() {
		if (this.mixControl.driver.getGlobalViewActive().get()) {
			return this.mixControl.globalGroup.getMixerButtonLayer();
		}
		return this.mixControl.mainGroup.getMixerButtonLayer();
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		final boolean isGlobalView = mixControl.driver.getGlobalViewActive().get();
		if (which == 0) {
			if (isGlobalView) {
				return mixControl.globalGroup.getDisplayConfiguration(encoderAssign);
			}
			return mixControl.mainGroup.getDisplayConfiguration(encoderAssign);
		}

		if (isGlobalView) {
			return mixControl.globalGroup.getDisplayConfiguration(ParamElement.VOLUME);
		}
		return mixControl.mainGroup.getDisplayConfiguration(ParamElement.VOLUME);
	}
}