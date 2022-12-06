package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.SettableStringValue;

public class CombinedStringValueObject implements SettableStringValue {

	private String value = "";
	private final List<StringValueChangedCallback> callbacks = new ArrayList<>();

	public CombinedStringValueObject(final String intialValue) {
		this.value = intialValue;
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

	@Override
	public void set(final String value) {
		this.value = value;
		for (final StringValueChangedCallback changedCallbacks : callbacks) {
			changedCallbacks.valueChanged(value);
		}
	}

	@Override
	public String get() {
		return value;
	}

	@Override
	public String getLimited(final int maxLength) {
		return value.substring(maxLength);
	}

}
