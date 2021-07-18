package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.framework.Layer;

public class EncoderLayer extends Layer {

	private final MixControl control;
	private EncoderMode encoderMode;
	private int stepDivisor;

	public EncoderLayer(final MixControl mixControl, final String name) {
		super(mixControl.getDriver().getLayers(), name);
		this.control = mixControl;
		this.encoderMode = EncoderMode.ACCELERATED;
		this.stepDivisor = 64;
	}

	public void setEncoderMode(final EncoderMode encoderMode) {
		this.encoderMode = encoderMode;
	}

	public void setStepDivisor(final int stepDivisor) {
		this.stepDivisor = stepDivisor;
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		control.getHwControls().setEncoderBehavior(encoderMode, stepDivisor);
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
	}
}
