package com.bitwig.extensions.controllers.mackie.configurations;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ResetableRelativeValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingDisplayRangedValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ValueConverter;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderMode;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.value.IncrementalValue;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;
import com.bitwig.extensions.framework.Layer;

public class MenuModeLayerConfiguration extends LayerConfiguration {
	private final EncoderLayer encoderLayer;
	private final DisplayLayer displayLayer;
	private Consumer<String> displayEvaluation;

	public MenuModeLayerConfiguration(final String name, final MixControl mixControl) {
		super(name, mixControl);
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		encoderLayer = new EncoderLayer(mixControl, name + "_ENCODER_LAYER_" + sectionIndex);
		encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
		displayLayer = new DisplayLayer(name, this.mixControl);
	}

	public void setTextEvaluation(final Consumer<String> action) {
		this.displayEvaluation = action;
	}

	public void evaluateTextDisplay(final String text) {
		if (displayEvaluation != null) {
			displayEvaluation.accept(text);
		}
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

	public void addDisplayValueBinding(final int i, final StringValue value) {
		displayLayer.bindName(1, i, value);
	}

	public void addDisplayValueBinding(final int i, final IntValueObject value) {
		displayLayer.bindParameterValue(i, value);
	}

	public <T> void addDisplayValueBinding(final int i, final ValueObject<T> value) {
		displayLayer.bindParameterValue(i, value);
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

	public <T> void addEncoderIncBinding(final int i, final IncrementalValue value) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
		final RelativeHardwarControlBindable incBinder = getDriver().createIncrementBinder(inc -> {
			value.increment(inc);
		});
		encoderLayer.bind(encoder, incBinder);
	}

	public void addEncoderBinding(final int i, final SettableRangedValue value, final double sensitivity) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
		final ResetableRelativeValueBinding absoluteEncoderBinding = new ResetableRelativeValueBinding(encoder, value,
				sensitivity);
		encoderLayer.addBinding(absoluteEncoderBinding);
		final RingDisplay ringDisplay = hwControls.getRingDisplay(i);
		final RingDisplayRangedValueBinding ringBinding = new RingDisplayRangedValueBinding(value, ringDisplay,
				RingDisplayType.FILL_LR);
		encoderLayer.addBinding(ringBinding);
	}

}
