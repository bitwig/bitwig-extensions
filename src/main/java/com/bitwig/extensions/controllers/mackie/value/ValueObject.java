package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

public class ValueObject<T> implements IncrementalValue {

	private final List<ChangeCallback<T>> callbacks = new ArrayList<>();

	private final IncrementHandler<T> incrementHandler;
	private final StringConverter<T> converter;

	private T value;

	@FunctionalInterface
	public interface ChangeCallback<T> {
		void valueChanged(T oldValue, T newValue);
	}

	@FunctionalInterface
	public interface StringConverter<T> {
		String convert(T value);
	}

	@FunctionalInterface
	public interface IncrementHandler<T> {
		T increment(T value, int increment);
	}

	public ValueObject(final T initValue) {
		this.value = initValue;
		this.incrementHandler = null;
		this.converter = null;
	}

	public ValueObject(final T initValue, final IncrementHandler<T> incrementHandler,
			final StringConverter<T> converter) {
		this.value = initValue;
		this.incrementHandler = incrementHandler;
		this.converter = converter;
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

	@Override
	public void increment(final int inc) {
		if (incrementHandler != null) {
			set(incrementHandler.increment(value, inc));
		}
	}

	@Override
	public String displayedValue() {
		if (converter != null) {
			return converter.convert(value);
		}
		return value.toString();
	}

	public T get() {
		return value;
	}

}
