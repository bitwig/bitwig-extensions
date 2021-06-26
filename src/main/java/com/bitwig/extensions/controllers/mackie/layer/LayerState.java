package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

public class LayerState {
	private Layer faderLayer;
	private Layer encoderLayer;
	private Layer buttonLayer;
	private DisplayLayer displayLayer;

	LayerState(final LayerConfiguration initalConfig) {
		faderLayer = initalConfig.getFaderLayer();
		encoderLayer = initalConfig.getEncoderLayer();
		buttonLayer = initalConfig.getButtonLayer();
		displayLayer = initalConfig.getDisplayLayer(0);
		faderLayer.setIsActive(true);
		encoderLayer.setIsActive(true);
		buttonLayer.setIsActive(true);
		displayLayer.setIsActive(true);
	}

	public void updateState(final LayerConfiguration config, final DisplayLayer displayLayer) {
		final Layer newFaderLayer = config.getFaderLayer();
		final Layer newEncoderLayer = config.getEncoderLayer();
		final Layer newButtonLayer = config.getButtonLayer();
		if (!newFaderLayer.equals(faderLayer)) {
			faderLayer.setIsActive(false);
			faderLayer = newFaderLayer;
			faderLayer.setIsActive(true);
		}
		if (!newEncoderLayer.equals(encoderLayer)) {
			RemoteConsole.out.println(" Encoder Config ", config.getName());
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