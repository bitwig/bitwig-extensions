package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.framework.Layer;

public class LayerState {
	private Layer faderLayer;
	private Layer encoderLayer;
	private Layer buttonLayer;
	private DisplayLayer displayLayer;

	LayerState(final LayerStateHandler statHandler) {
		final LayerConfiguration initalConfig = statHandler.getCurrentConfig();

		faderLayer = initalConfig.getFaderLayer();
		encoderLayer = initalConfig.getEncoderLayer();

		buttonLayer = statHandler.getButtonLayer();

		displayLayer = statHandler.getActiveDisplayLayer();
		faderLayer.setIsActive(true);
		encoderLayer.setIsActive(true);
		buttonLayer.setIsActive(true);
		displayLayer.setIsActive(true);
	}

	public void updateState(final LayerStateHandler statHandler) {
		final LayerConfiguration config = statHandler.getCurrentConfig();
		final DisplayLayer displayLayer = statHandler.getActiveDisplayLayer();

		final Layer newFaderLayer = config.getFaderLayer();
		final Layer newEncoderLayer = config.getEncoderLayer();

		final Layer newButtonLayer = statHandler.getButtonLayer();

		if (!newFaderLayer.equals(faderLayer)) {
			faderLayer.setIsActive(false);
			faderLayer = newFaderLayer;
			faderLayer.setIsActive(true);
		}
		if (!newEncoderLayer.equals(encoderLayer)) {
			encoderLayer.setIsActive(false);
			encoderLayer = newEncoderLayer;
			encoderLayer.setIsActive(true);
		}
		if (!newButtonLayer.equals(buttonLayer)) {
			buttonLayer.setIsActive(false);
			buttonLayer = newButtonLayer;
			buttonLayer.setIsActive(true);
		}
		updateDisplayState(displayLayer);
	}

	public void updateDisplayState(final DisplayLayer newDisplayLayer) {
		if (!newDisplayLayer.equals(displayLayer)) {
			displayLayer.setIsActive(false);
			displayLayer = newDisplayLayer;
			displayLayer.setIsActive(true);
		}
	}
}