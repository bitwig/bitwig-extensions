package com.bitwig.extensions.controllers.mackie.configurations;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ValueConverter;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderMode;
import com.bitwig.extensions.controllers.mackie.layer.MixControl;
import com.bitwig.extensions.controllers.mackie.layer.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.layer.ParamElement;
import com.bitwig.extensions.framework.Layer;

public class MenuModeLayerConfiguration extends LayerConfiguration {
	private final EncoderLayer encoderLayer;
	private final DisplayLayer displayLayer;

	public MenuModeLayerConfiguration(final String name, final MixControl mixControl) {
		super(name, mixControl);
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		encoderLayer = new EncoderLayer(mixControl, name + "_ENCODER_LAYER_" + sectionIndex);
		encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
		displayLayer = new DisplayLayer(name, this.mixControl);
	}

	@Override
	public boolean isActive() {
		return encoderLayer.isActive();
	}

	@Override
	public Layer getFaderLayer() {
		return this.mixControl.getActiveMixGroup().getFaderLayer(ParamElement.VOLUME);
	}

	@Override
	public EncoderLayer getEncoderLayer() {
		return encoderLayer;
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		return displayLayer;
	}

	public void addNameBinding(final int index, final StringValue name) {
		displayLayer.bindName(index, name);
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

	public void addRingFixedBindingActive(final int index) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 11, RingDisplayType.FILL_LR_0));
	}

	public void addRingExistsBinding(final int index, final ObjectProxy existSource) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, existSource, RingDisplayType.FILL_LR_0));
	}

	public void addValueBinding(final int i, final DoubleValue value, final ObjectProxy existSource,
			final String nonExistText, final ValueConverter converter) {
		displayLayer.bindParameterValue(i, value, existSource, nonExistText, converter);
	}

	public void addValueBinding(final int i, final DoubleValue value, final ValueConverter converter) {
		displayLayer.bindParameterValue(i, value, converter);
	}

	public void addValueBinding(final int i, final BooleanValue value, final String trueString,
			final String falseString) {
		displayLayer.bindBool(i, value, trueString, falseString);
	}

	public void addEncoderIncBinding(final int i, final SettableBeatTimeValue position, final double increment) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
		final RelativeHardwarControlBindable incBinder = getDriver().createIncrementBinder(inc -> {
			position.set(position.get() + inc * increment);
		});
		encoderLayer.bind(encoder, incBinder);
	}

}
