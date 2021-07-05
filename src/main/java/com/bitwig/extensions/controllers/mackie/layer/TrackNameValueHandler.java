package com.bitwig.extensions.controllers.mackie.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.StringUtil;

public class TrackNameValueHandler implements StringValue {

	private final List<StringValueChangedCallback> listeners = new ArrayList<>();
	private String currentValue = "";
	private final int maxLen = 7;

	public TrackNameValueHandler(final StringValue value) {
		value.addValueObserver(newString -> {
			currentValue = StringUtil.toAsciiDisplay(newString, maxLen);
			listeners.forEach(callback -> callback.valueChanged(currentValue));
		});
		currentValue = StringUtil.toAsciiDisplay(value.get(), maxLen);
	}

	@Override
	public void markInterested() {
	}

	@Override
	public void addValueObserver(final StringValueChangedCallback callback) {
		listeners.add(callback);
	}

	@Override
	public boolean isSubscribed() {
		return true;
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
	public String get() {
		return currentValue;
	}

	@Override
	public String getLimited(final int maxLength) {
		return currentValue;
	}

}
