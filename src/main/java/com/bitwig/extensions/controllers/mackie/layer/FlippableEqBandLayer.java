package com.bitwig.extensions.controllers.mackie.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;

public class FlippableEqBandLayer extends FlippableLayer {

	private final List<ParameterPage> bands = new ArrayList<>();

	public FlippableEqBandLayer(final ChannelSection section, final String name) {
		super(section, name);
	}

	@Override
	public void navigateLeftRight(final int direction) {
		if (!isActive()) {
			return;
		}

		for (final ParameterPage eqFreqencyBand : bands) {
			if (direction < 0) {
				eqFreqencyBand.navigatePrevious();
				eqFreqencyBand.triggerUpdate();
			} else if (direction > 0) {
				eqFreqencyBand.navigateNext();
				eqFreqencyBand.triggerUpdate();
			}
		}
	}

	public void addBinding(final int index, final ParameterPage freqBand, final boolean isMixer) {
		bands.add(freqBand);
		bindVolumeParameter(index, isMixer);

		mainLayer.addBinding(freqBand.getRelativeEncoderBinding(section.getEncoder(index)));

		mainLayer.addBinding(freqBand.createRingBinding(section.getRingDisplay(index)));
		mainLayer.addBinding(section.createResetBinding(index, freqBand));

		flipLayer.addBinding(freqBand.getFaderBinding(section.getVolumeFader(index)));
		flipLayer.addBinding(freqBand.createFaderBinding(section.getMotorFader(index)));
//		flipLayer.addBinding(section.createTouchFaderBinding(index, encParameter));

		displayNonTouchLayer.addBinding(freqBand.createValueDisplayBinding(section.getValueTarget(index)));
		if (!isMixer) {
			displayNonTouchLayer.addBinding(freqBand.createNameDisplayBinding(section.getNameTarget(index)));
		}
		freqBand.resetBindings();
	}

}
