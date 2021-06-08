package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayNameBinding;
import com.bitwig.extensions.controllers.mackie.bindings.DisplayValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingDisplayBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class FlippableLayer extends Layer {

	private final Layer mainLayer;
	private final Layer flipLayer;
	private boolean flipped = false;
	private boolean touched = false;
	private final Layer displayNonTouchLayer;
	private final Layer displayTouchLayer;
	private final ChannelSection section;

	public FlippableLayer(final ChannelSection section, final String name) {
		super(section.getLayers(), name);
		final Layers layers = section.getLayers();
		this.section = section;
		mainLayer = new Layer(layers, name + "_FLIP_NORMAL");
		flipLayer = new Layer(layers, name + "_FLIP_FLIP");
		displayNonTouchLayer = new Layer(layers, name + "_NON_TOUCH");
		displayTouchLayer = new Layer(layers, name + "_TOUCH");
	}

	public void addBinding(final int index, final Parameter other, final RingDisplayType type, final boolean isMixer) {
		mainLayer.addBinding(section.getVolumeToFaderBinding(index));
		mainLayer.bind(section.getEncoder(index), other);
		mainLayer.addBinding(section.getVolumeFaderBinding(index));
		mainLayer.addBinding(new RingDisplayBinding(other, section.getRingDisplay(index), type));

		flipLayer.bind(section.getVolumeFader(index), other);
		flipLayer.addBinding(section.getVolumeToEncoderBindings(index));
		flipLayer.addBinding(new FaderBinding(other, section.getMotorFader(index)));
		flipLayer.addBinding(section.getVolumeRingDisplayBinding(index));

		displayTouchLayer.addBinding(section.getVolumeDisplayBinding(index));
		displayNonTouchLayer.addBinding(new DisplayValueBinding(other, section.getValueTarget(index)));
		if (isMixer) {
			displayTouchLayer.addBinding(section.getChannelNameBinding(index));
			displayNonTouchLayer.addBinding(section.getChannelNameBinding(index));
		} else {
			displayTouchLayer.addBinding(section.getChannelNameBinding(index));
			displayNonTouchLayer.addBinding(new DisplayNameBinding(other.name(), section.getNameTarget(index)));
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

	public void setTouched(final boolean touched) {
		if (touched == this.touched) {
			return;
		}
		this.touched = touched;
		updateDisplayLayers();
	}

	private void updateDisplayLayers() {
		if (!isActive()) {
			return;
		}
		if (touched && !flipped || !touched && flipped) {
			displayNonTouchLayer.deactivate();
			displayTouchLayer.activate();
		} else {
			displayTouchLayer.deactivate();
			displayNonTouchLayer.activate();
		}
	}

	public void updateActivation() {
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

}
