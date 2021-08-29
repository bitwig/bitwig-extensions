package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.ButtonLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.framework.Layer;

public class FullDeviceLayerConfiguration extends TrackLayerConfiguration {

	private final ButtonLayer buttonLayer;

	public FullDeviceLayerConfiguration(final String name, final MixControl mixControl) {
		super(name, mixControl);
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();

		// Create an own type of Layer for Parameters
		this.buttonLayer = new ButtonLayer("ARP_BUTTONLAYER_" + sectionIndex, mixControl, null, 0);
	}

	@Override
	public Layer getFaderLayer() {
		return faderLayer;
	}

	@Override
	public EncoderLayer getEncoderLayer() {
		if (mixControl.getIsMenuHoldActive().get()) {
			return menuConfig.getEncoderLayer();
		}
		return encoderLayer;
	}

	@Override
	public Layer getButtonLayer() {
		return buttonLayer;
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		if (mixControl.getIsMenuHoldActive().get()) {
			return menuConfig.getDisplayLayer();
		}
		if (deviceManager != null && deviceManager.getInfoSource() != null) {
			return infoLayer;
		}
		return displayLayer;
	}

}
