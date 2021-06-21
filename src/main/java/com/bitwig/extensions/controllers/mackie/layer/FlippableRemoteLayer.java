package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.bindings.FullDisplayBinding;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTracker;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.value.DisplayTextValue;
import com.bitwig.extensions.framework.Layer;

/**
 * Flippable Layer that is attached to a device with remote control pages.
 *
 */
public class FlippableRemoteLayer extends FlippableLayer {

	private DeviceTracker device;
	protected final Layer staticTextLayer;
	private final DisplayTextValue displayText = new DisplayTextValue();
	private boolean deviceExists;

	public FlippableRemoteLayer(final ChannelSection section, final String name) {
		super(section, name);
		staticTextLayer = new Layer(section.getLayers(), name + "_STATIC_TEXT");
	}

	public void addDisplayBinding(final LcdDisplay display) {
		// for some reason this does not work if execute with doAdditionalBindings
		staticTextLayer.addBinding(new FullDisplayBinding(displayText, display, true));
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
			device.selectPreviousParameterPage();
		} else {
			device.selectNextParameterPage();
		}
	}

	@Override
	public void navigateUpDown(final int direction) {
		if (!isActive()) {
			return;
		}
		if (direction < 0) {
			device.selectPreviousDevice();
		} else {
			device.selectNextDevice();
		}
	}

	public void setDevice(final DeviceTracker device) {
		this.device = device;
		this.device.getDevice().exists().addValueObserver(exist -> {
			deviceExists = exist;
			updateDisplayLayers();
		});
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
