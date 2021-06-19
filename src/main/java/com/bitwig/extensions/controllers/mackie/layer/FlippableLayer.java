package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.bindings.AbstractDisplayValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayDoubleValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayNameBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayStringValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingDisplayBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ValueConverter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class FlippableLayer extends Layer {

	private boolean flipped = false;
	private boolean touched = false;

	protected final ChannelSection section;
	protected final Layer mainLayer;
	protected final Layer flipLayer;
	protected final Layer displayNonTouchLayer;
	protected final Layer displayTouchLayer;

	public FlippableLayer(final ChannelSection section, final String name) {
		super(section.getLayers(), name);
		final Layers layers = section.getLayers();
		this.section = section;
		mainLayer = new Layer(layers, name + "_FLIP_NORMAL");
		flipLayer = new Layer(layers, name + "_FLIP_FLIP");
		displayNonTouchLayer = new Layer(layers, name + "_NON_TOUCH");
		displayTouchLayer = new Layer(layers, name + "_TOUCH");
	}

	public void addBinding(final int index, final Parameter encParameter, final RingDisplayType type,
			final boolean isMixer, final ValueConverter converter) {
		bindVolumeParameter(index, isMixer);
		final DisplayDoubleValueBinding displayValueBinding = new DisplayDoubleValueBinding(encParameter.value(),
				section.getValueTarget(index), converter);
		addBinding(index, encParameter, type, isMixer, displayValueBinding);
	}

	public void addBinding(final int index, final Parameter encParameter, final RingDisplayType type,
			final boolean isMixer) {
		bindVolumeParameter(index, isMixer);
		final DisplayStringValueBinding displayValueBinding = new DisplayStringValueBinding(
				encParameter.displayedValue(), section.getValueTarget(index));
		addBinding(index, encParameter, type, isMixer, displayValueBinding);
	}

	public void addBinding(final int index, final Parameter encParameter, final RingDisplayType type,
			final boolean isMixer, final AbstractDisplayValueBinding<?> displayValueBinding) {
		mainLayer.bind(section.getEncoder(index), encParameter.value());
		mainLayer.addBinding(new RingDisplayBinding(encParameter, section.getRingDisplay(index), type));
		mainLayer.addBinding(section.createResetBinding(index, encParameter));

		flipLayer.bind(section.getVolumeFader(index), encParameter);
		flipLayer.addBinding(new FaderBinding(encParameter, section.getMotorFader(index)));
		flipLayer.addBinding(section.createTouchFaderBinding(index, encParameter));

		displayNonTouchLayer.addBinding(displayValueBinding);
		if (!isMixer) {
			displayNonTouchLayer.addBinding(new DisplayNameBinding(encParameter.name(), section.getNameTarget(index)));
		}
	}

	protected void bindVolumeParameter(final int index, final boolean isMixer) {
		mainLayer.addBinding(section.getVolumeToFaderBinding(index));
		mainLayer.addBinding(section.getVolumeFaderBinding(index));
		mainLayer.addBinding(section.getVolumeTouchBinding(index));
		flipLayer.addBinding(section.getVolumeResetBinding(index));
		flipLayer.addBinding(section.getVolumeToEncoderBindings(index));
		flipLayer.addBinding(section.getVolumeRingDisplayBinding(index));
		displayTouchLayer.addBinding(section.getVolumeDisplayBinding(index));
		if (isMixer) {
			displayTouchLayer.addBinding(section.getChannelNameBinding(index));
			displayNonTouchLayer.addBinding(section.getChannelNameBinding(index));
		} else {
			displayTouchLayer.addBinding(section.getChannelNameBinding(index));
		}
	}

	public void setFlipped(final boolean flipped) {
		if (flipped == this.flipped) {
			return;
		}
		this.flipped = flipped;
		updateActivation();
		updateDisplayLayers();
	}

	public boolean isFlipped() {
		return flipped;
	}

	public void setTouched(final boolean touched) {
		if (touched == this.touched) {
			return;
		}
		this.touched = touched;
		updateDisplayLayers();
	}

	void updateDisplayLayers() {
		if (!isActive()) {
			return;
		}
		if (touched && !flipped || !touched && flipped) {
			activateTouchDisplay();
		} else {
			activateNonTouchDisplay();
		}
	}

	void activateNonTouchDisplay() {
		displayTouchLayer.deactivate();
		displayNonTouchLayer.activate();
	}

	void activateTouchDisplay() {
		displayNonTouchLayer.deactivate();
		displayTouchLayer.activate();
	}

	private void updateActivation() {
		if (!isActive()) {
			return;
		}
		if (this.flipped) {
			mainLayer.deactivate();
			flipLayer.activate();
		} else {
			flipLayer.deactivate();
			mainLayer.activate();
		}
	}

	@Override
	protected void onActivate() {
		updateActivation();
		updateDisplayLayers();
	}

	@Override
	protected void onDeactivate() {
		flipLayer.deactivate();
		mainLayer.deactivate();
		displayNonTouchLayer.deactivate();
		displayTouchLayer.deactivate();
	}

	public void navigateLeftRight(final int direction) {
	}

	public void navigateUpDown(final int direction) {
	}
}
