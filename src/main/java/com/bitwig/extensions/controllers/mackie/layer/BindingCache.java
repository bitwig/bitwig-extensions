package com.bitwig.extensions.controllers.mackie.layer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BindingCache {

	private final Map<Class<?>, Map<Integer, ?>> storage = new HashMap<>();

	public <T> T get(final Class<T> clazz, final int index) {
		@SuppressWarnings("unchecked")
		final Map<Integer, T> list = (Map<Integer, T>) storage.get(clazz);
		if (list != null) {
			return list.get(index);
		}
		return null;
	}

	public <T> T getInit(final Class<T> clazz, final int index, final Supplier<T> producer) {
		@SuppressWarnings("unchecked")
		Map<Integer, T> list = (Map<Integer, T>) storage.get(clazz);
		if (list == null) {
			list = new HashMap<>();
			storage.put(clazz, list);
		}

		T ele = list.get(index);
		if (ele == null) {
			ele = producer.get();
			list.put(index, ele);
		}
		return ele;
	}
}
