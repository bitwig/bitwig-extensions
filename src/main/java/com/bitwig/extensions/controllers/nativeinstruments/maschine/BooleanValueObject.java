package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.BooleanValue;

public class BooleanValueObject implements BooleanValue {

	private boolean value = false;
	private final List<BooleanValueChangedCallback> callbacks = new ArrayList<BooleanValueChangedCallback>();

	@Override
	public void markInterested() {
	}

	public void toggle() {
		this.value = !value;
		for (final BooleanValueChangedCallback booleanValueChangedCallback : callbacks) {
			booleanValueChangedCallback.valueChanged(value);
		}
	}

	@Override
	public void addValueObserver(final BooleanValueChangedCallback callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	@Override
	public boolean isSubscribed() {
		return false;
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

	public void setValue(final boolean value) {
		this.value = value;
		for (final BooleanValueChangedCallback booleanValueChangedCallback : callbacks) {
			booleanValueChangedCallback.valueChanged(value);
		}
	}

	@Override
	public boolean get() {
		return value;
	}

}
