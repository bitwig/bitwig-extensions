package com.bitwig.extensions.controllers.mackie.layer;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ValueConverter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

public class MenuModeLayerConfiguration extends LayerConfiguration {
	private final Layer encoderLayer;
	private final DisplayLayer displayLayer;

	public MenuModeLayerConfiguration(final String name, final MixControl mixControl) {
		super(name, mixControl);
		final Layers layers = this.mixControl.getDriver().getLayers();
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		encoderLayer = new Layer(layers, name + "_ENCODER_LAYER_" + sectionIndex);
		displayLayer = new DisplayLayer(name, this.mixControl);
	}

	public boolean isActive() {
		return encoderLayer.isActive();
	}

	@Override
	public Layer getFaderLayer() {
		final boolean isMixerGlobal = this.mixControl.driver.getGlobalViewActive().get();
		if (isMixerGlobal) {
			return this.mixControl.globalGroup.getFaderLayer(ParamElement.VOLUME);
		}
		return this.mixControl.mainGroup.getFaderLayer(ParamElement.VOLUME);
	}

	@Override
	public Layer getEncoderLayer() {
		return encoderLayer;
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		return displayLayer;
	}

	public void addNameBinding(final int index, final StringValue nameSource, final ObjectProxy source,
			final String emptyValue) {
		displayLayer.bindName(index, nameSource, source, emptyValue);
	}

	public void addPressEncoderBinding(final int index, final IntConsumer pressaction) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();

		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(index),
				hwControls.createAction(() -> pressaction.accept(index))));
	}

	public void addRingBoolBinding(final int index, final BooleanValue value) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, value, RingDisplayType.FILL_LR_0));
	}

	public void addRingFixedBinding(final int index) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 0, RingDisplayType.FILL_LR_0));
	}

	public void addRingExistsBinding(final int index, final ObjectProxy existSource) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, existSource, RingDisplayType.FILL_LR_0));
	}

	public void addValueBinding(final int i, final DoubleValue value, final ObjectProxy existSource,
			final String nonExistText, final ValueConverter converter) {
		displayLayer.bindParameterValue(i, value, existSource, nonExistText, converter);
	}

	public void addEncoderIncBinding(final int i, final SettableBeatTimeValue position, final double increment) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
		final RelativeHardwareControlBinding bind = new RelativeHardwareControlBinding(encoder, position);
		encoderLayer.addBinding(bind);
	}

}
