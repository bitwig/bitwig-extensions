package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

public class ValueObject<T> {

	private final List<ChangeCallback<T>> callbacks = new ArrayList<>();
	T value;

	public interface ChangeCallback<T> {
		void valueChanged(T oldValue, T newValue);
	}

	public ValueObject(final T initValue) {
		this.value = initValue;
	}

	public void addValueObserver(final ChangeCallback<T> callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	public void set(final T value) {
		if (this.value == value) {
			return;
		}
		final T oldValue = this.value;
		this.value = value;
		for (final ChangeCallback<T> listener : callbacks) {
			listener.valueChanged(oldValue, value);
		}
	}

	public T get() {
		return value;
	}

}
