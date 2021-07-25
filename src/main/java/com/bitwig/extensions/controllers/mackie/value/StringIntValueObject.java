package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

public class StringIntValueObject implements StringValue {
	private final List<StringValueChangedCallback> callbacks = new ArrayList<>();
	private final String valuePrefix;
	private final StringValue stringValue;
	private final ObjectProxy sourceOfExistence;
	private final IntegerValue intValue;

	public StringIntValueObject(final StringValue stringValue, final IntegerValue intValue,
			final ObjectProxy sourceOfExistance, final String intValueExpression) {
		this.stringValue = stringValue;
		this.sourceOfExistence = sourceOfExistance;
		this.intValue = intValue;
		this.valuePrefix = intValueExpression;
		sourceOfExistance.exists().addValueObserver(exists -> {
			callbacks.forEach(callback -> callback.valueChanged(calcValue(stringValue.get(), exists, intValue.get())));
		});
		intValue.addValueObserver(v -> {
			callbacks.forEach(callback -> callback
					.valueChanged(calcValue(stringValue.get(), sourceOfExistence.exists().get(), v)));
		});
		stringValue.addValueObserver(v -> {
			RemoteConsole.out.println("CHANGOR {}", v);
			callbacks.forEach(
					callback -> callback.valueChanged(calcValue(v, sourceOfExistence.exists().get(), intValue.get())));
		});
	}

	private String calcValue(final String baseValue, final boolean exists, final int value) {
		if (!exists) {
			return String.format(valuePrefix, intValue.get());
		}
		return baseValue;
	}

	@Override
	public String get() {
		return calcValue(stringValue.get(), sourceOfExistence.exists().get(), intValue.get());
	}

	@Override
	public String getLimited(final int maxLength) {
		return get();
	}

	@Override
	public void markInterested() {
	}

	@Override
	public void addValueObserver(final StringValueChangedCallback callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	@Override
	public boolean isSubscribed() {
		return !callbacks.isEmpty();
	}

	@Override
	public void setIsSubscribed(final boolean value) {
	}

	@Override
	public void subscribe() {
	}

	@Override
	public void unsubscribe() {
	}

}
