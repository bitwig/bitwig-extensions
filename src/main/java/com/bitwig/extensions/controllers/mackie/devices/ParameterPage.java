package com.bitwig.extensions.controllers.mackie.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.RelativeHardwareControlToRangedValueBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.bindings.AbstractDisplayNameBinding;
import com.bitwig.extensions.controllers.mackie.bindings.AbstractDisplayValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderParameterBankBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ResetableAbsoluteValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ResetableRelativeValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.RingParameterBankDisplayBinding;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.target.DisplayNameTarget;
import com.bitwig.extensions.controllers.mackie.target.DisplayValueTarget;
import com.bitwig.extensions.controllers.mackie.target.MotorFader;
import com.bitwig.extensions.controllers.mackie.target.RingDisplay;

public class ParameterPage implements SettableRangedValue {

	private static final int RING_RANGE = 10;
	private DeviceParameter currentParameter;
	private int pageIndex = 0;

	private ResetableRelativeValueBinding relativeEncoderBinding;
	private ResetableAbsoluteValueBinding absoluteEncoderBinding;
	private RingParameterBankDisplayBinding ringBinding;
	private FaderParameterBankBinding faderBinding;

	// listeners for the parameter Name
	private final List<Consumer<String>> nameChangeCallbacks = new ArrayList<>();
	// listeners for the display values
	private final List<Consumer<String>> valueChangeCallbacks = new ArrayList<>();
	// listeners for the ring display values
	private final List<IntConsumer> intValueCallbacks = new ArrayList<>();
	// listeners for existence of value // TODO ring value
	private final List<Consumer<Boolean>> exitsCallbacks = new ArrayList<>();
	private final List<DoubleConsumer> doulbeValueCallbacks = new ArrayList<>();

	private final List<DeviceParameter> pages = new ArrayList<>();

	private final boolean exists = false;

	public ParameterPage(final int index, final SpecificBitwigDevice device, final ParameterGenerator generator) {

		for (int page = 0; page < generator.getPages(); page++) {
			final int pIndex = pages.size();
			final String pname = generator.getParamName(page, index);
			final Parameter param = device.createParameter(pname);
			final DeviceParameter deviceParameter = generator.createDeviceParameter(pname, param, page, index);

			param.value().markInterested();
			param.value().addValueObserver(v -> {
				if (pIndex == pageIndex) {
					final int intv = (int) (v * RING_RANGE);
					notifyIntValueChanged(intv);
					notifyValueChanged(v);
				}
			});
			param.value().displayedValue().addValueObserver(v -> {
				if (pIndex == pageIndex) {
					notifyValueChanged(v);
				}
			});

			pages.add(deviceParameter);
		}
		currentParameter = pages.get(pageIndex);
	}

	public void triggerUpdate() {
		if (ringBinding != null) {
			ringBinding.update();
		}
		if (faderBinding != null) {
			faderBinding.update();
		}
	}

	public RingParameterBankDisplayBinding createRingBinding(final RingDisplay display) {
		ringBinding = new RingParameterBankDisplayBinding(this, display);
		return ringBinding;
	}

	public FaderParameterBankBinding createFaderBinding(final MotorFader fader) {
		faderBinding = new FaderParameterBankBinding(this, fader);
		return faderBinding;
	}

	public AbstractDisplayNameBinding<ParameterPage> createNameDisplayBinding(final DisplayNameTarget target) {
		return new AbstractDisplayNameBinding<ParameterPage>(this, target) {
			@Override
			protected void initListening() {
				getSource().addNameObserver(this::valueChanged);
			}
		};
	}

	public AbstractDisplayValueBinding<ParameterPage> createValueDisplayBinding(final DisplayValueTarget target) {
		return new AbstractDisplayValueBinding<ParameterPage>(this, target) {
			@Override
			protected void initListening() {
				getSource().addStringValueObserver(this::valueChanged);
			}
		};
	}

	public ResetableRelativeValueBinding getRelativeEncoderBinding(final RelativeHardwareKnob encoder) {
		this.relativeEncoderBinding = new ResetableRelativeValueBinding(encoder, this);
		return this.relativeEncoderBinding;
	}

	public ResetableAbsoluteValueBinding getFaderBinding(final AbsoluteHardwareKnob fader) {
		this.absoluteEncoderBinding = new ResetableAbsoluteValueBinding(fader, this);
		return this.absoluteEncoderBinding;
	}

	public void navigateNext() {
		pageIndex = (pageIndex + 1) % pages.size();
		currentParameter = pages.get(pageIndex);
		resetBindings();
	}

	public void navigatePrevious() {
		final int nextIndex = pageIndex - 1;
		pageIndex = nextIndex < 0 ? pages.size() - 1 : nextIndex < pages.size() ? nextIndex : 0;
		currentParameter = pages.get(pageIndex);
		resetBindings();
	}

	public void resetBindings() {
		if (relativeEncoderBinding != null) {
			this.relativeEncoderBinding.reset();
		}
		if (absoluteEncoderBinding != null) {
			this.absoluteEncoderBinding.reset();
		}
		notifyValueChanged(getCurrentValue());
		notifyNameChanged(getCurrentName());
	}

	public Parameter getCurrentParameter() {
		return currentParameter.parameter;
	}

	@Override
	public double get() {
		return currentParameter.parameter.get();
	}

	@Override
	public void markInterested() {
		currentParameter.parameter.markInterested();
	}

	public void addStringValueObserver(final Consumer<String> callback) {
		valueChangeCallbacks.add(callback);
	}

	@Override
	public void addValueObserver(final DoubleValueChangedCallback callback) {
	}

	@Override
	public boolean isSubscribed() {
		return currentParameter.parameter.isSubscribed();
	}

	@Override
	public void setIsSubscribed(final boolean value) {
	}

	@Override
	public void subscribe() {
		currentParameter.parameter.subscribe();
	}

	@Override
	public void unsubscribe() {
		currentParameter.parameter.unsubscribe();
	}

	@Override
	public void set(final double value) {
		currentParameter.parameter.set(value);
	}

	@Override
	public void inc(final double amount) {
		currentParameter.parameter.inc(amount);
	}

	@Override
	public double getRaw() {
		return currentParameter.parameter.getRaw();
	}

	@Override
	public StringValue displayedValue() {
		return currentParameter.parameter.displayedValue();
	}

	@Override
	public void addValueObserver(final int range, final IntegerValueChangedCallback callback) {
		// Not needed
	}

	@Override
	public void addRawValueObserver(final DoubleValueChangedCallback callback) {
		// Not needed
	}

	@Override
	public void setImmediately(final double value) {
		currentParameter.parameter.setImmediately(value);
	}

	@Override
	public void set(final Number value, final Number resolution) {
		currentParameter.parameter.set(value, resolution);
	}

	@Override
	public void inc(final Number increment, final Number resolution) {
		currentParameter.parameter.inc(increment, resolution);
	}

	@Override
	public void setRaw(final double value) {
		currentParameter.parameter.setRaw(value);
	}

	@Override
	public void incRaw(final double delta) {
		currentParameter.parameter.incRaw(delta);
	}

	@Override
	public AbsoluteHardwareControlBinding addBindingWithRange(final AbsoluteHardwareControl hardwareControl,
			final double minNormalizedValue, final double maxNormalizedValue) {
		return currentParameter.parameter.addBindingWithRange(hardwareControl, minNormalizedValue, maxNormalizedValue);
	}

	@Override
	public RelativeHardwareControlToRangedValueBinding addBindingWithRangeAndSensitivity(
			final RelativeHardwareControl hardwareControl, final double minNormalizedValue,
			final double maxNormalizedValue, final double sensitivity) {
		return currentParameter.parameter.addBindingWithRangeAndSensitivity(hardwareControl, minNormalizedValue,
				maxNormalizedValue, currentParameter.sensitivity);
	}

	private void notifyValueChanged(final String value) {
		valueChangeCallbacks.forEach(callback -> callback.accept(value));
	}

	public String getCurrentValue() {
		return currentParameter.parameter.displayedValue().get();
	}

	public void addNameObserver(final Consumer<String> callback) {
		nameChangeCallbacks.add(callback);
	}

	private void notifyNameChanged(final String name) {
		nameChangeCallbacks.forEach(callback -> callback.accept(currentParameter.name));
	}

	public String getCurrentName() {
		return currentParameter.name;
	}

	public void addIntValueObserver(final IntConsumer listener) {
		intValueCallbacks.add(listener);
	}

	private void notifyIntValueChanged(final int value) {
		intValueCallbacks.forEach(callback -> callback.accept(value));
	}

	private void notifyValueChanged(final double value) {
		doulbeValueCallbacks.forEach(callback -> callback.accept(value));
	}

	public void addExistsValueObserver(final Consumer<Boolean> listener) {
		exitsCallbacks.add(listener);
	}

	public void notifyExists(final boolean value) {
		exitsCallbacks.forEach(callback -> callback.accept(value));
	}

	public int getIntValue() {
		return (int) (currentParameter.parameter.value().get() * RING_RANGE);
	}

	public RingDisplayType getRingDisplayType() {
		return currentParameter.ringDisplayType;
	}

	public boolean exists() {
		return exists;
	}

	public double getParamValue() {
		return currentParameter.parameter.value().get();
	}

	public void addDoubleValueObserver(final DoubleConsumer listener) {
		doulbeValueCallbacks.add(listener);
	}

	public void doReset() {
		currentParameter.parameter.reset();
	}

}