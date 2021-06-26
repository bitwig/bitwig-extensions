package com.bitwig.extensions.controllers.mackie.old;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FullDisplayBinding;
import com.bitwig.extensions.controllers.mackie.devices.ControlDevice;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.value.DisplayTextValue;
import com.bitwig.extensions.framework.Layer;

public class FlippableParameterBankLayer extends FlippableLayer {

	private final List<ParameterPage> bands = new ArrayList<>();
	protected final Layer staticTextLayer;

	private ControlDevice device;
	private boolean deviceExists;
	private final DisplayTextValue displayText = new DisplayTextValue();

	public FlippableParameterBankLayer(final ChannelSection section, final String name) {
		super(section, name);
		staticTextLayer = new Layer(section.getLayers(), name + "_STATIC_TEXT");
	}

	public void attachDevice(final ControlDevice device) {
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
		if (direction < 0) {
			device.navigatePrevious();

		} else if (direction > 0) {
			device.navigateNext();
		}
	}

	public void addDisplayBinding(final LcdDisplay display) {
		staticTextLayer.addBinding(new FullDisplayBinding(displayText, display, true));
	}

	public void addBinding(final int index, final ParameterPage parameterSlot, final boolean isMixer,
			final BiConsumer<Integer, ParameterPage> resetAction) {
		bands.add(parameterSlot);
		bindVolumeParameter(index, isMixer);

		mainLayer.addBinding(parameterSlot.getRelativeEncoderBinding(section.getEncoder(index)));

		mainLayer.addBinding(parameterSlot.createRingBinding(section.getRingDisplay(index)));

		final HardwareButton buttonPress = section.getEncoderPress(index);
		final HardwareActionBindable action = section.createAction(() -> resetAction.accept(index, parameterSlot));
		mainLayer.addBinding(new ButtonBinding(buttonPress, action));

		flipLayer.addBinding(parameterSlot.getFaderBinding(section.getVolumeFader(index)));
		flipLayer.addBinding(parameterSlot.createFaderBinding(section.getMotorFader(index)));
//		flipLayer.addBinding(section.createTouchFaderBinding(index, encParameter));

		displayNonTouchLayer.addBinding(parameterSlot.createValueDisplayBinding(section.getValueTarget(index)));
		if (!isMixer) {
			displayNonTouchLayer.addBinding(parameterSlot.createNameDisplayBinding(section.getNameTarget(index)));
		}
		parameterSlot.resetBindings();
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
