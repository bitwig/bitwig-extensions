package com.bitwig.extensions.controllers.mackie.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extensions.controllers.mackie.bindings.FullDisplayBinding;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.value.DisplayTextValue;
import com.bitwig.extensions.framework.Layer;

public class FlippableParameterBankLayer extends FlippableLayer {

	private final List<ParameterPage> bands = new ArrayList<>();
	protected final Layer staticTextLayer;

	private Device device;
	private boolean deviceExists;
	private final DisplayTextValue displayText = new DisplayTextValue();

	public FlippableParameterBankLayer(final ChannelSection section, final String name) {
		super(section, name);
		staticTextLayer = new Layer(section.getLayers(), name + "_STATIC_TEXT");
	}

	public void attachDevice(final Device device) {
		this.device = device;
		this.device.exists().addValueObserver(exist -> {
			deviceExists = exist;
			updateDisplayLayers();
		});
	}

	public void setMessages(final String... lines) {
		for (int i = 0; i < lines.length; i++) {
			displayText.setLine(i, lines[i]);
		}
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

	public void addDisplayBinding(final LcdDisplay display) {
		staticTextLayer.addBinding(new FullDisplayBinding(displayText, display, true));
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

	@Override
	void activateNonTouchDisplay() {
		displayTouchLayer.deactivate();
		if (deviceExists) {
			staticTextLayer.deactivate();
			displayNonTouchLayer.activate();
		} else {
			displayNonTouchLayer.deactivate();
			staticTextLayer.activate();
		}
	}

	@Override
	void activateTouchDisplay() {
		staticTextLayer.deactivate();
		displayNonTouchLayer.deactivate();
		displayTouchLayer.activate();
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
		staticTextLayer.deactivate();
	}

	@Override
	protected void onActivate() {
		super.onActivate();
	}

}
