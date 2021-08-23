package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.mackie.StringUtil;

public class TrackNameValueHandler implements StringValue {

	private final List<StringValueChangedCallback> listeners = new ArrayList<>();
	private String currentValue = "";
	private final int maxLen = 7;

	public TrackNameValueHandler(final Channel channel) {
		final SettableStringValue nameValue = channel.name();

		if (channel instanceof Track) {
			final Track track = (Track) channel;
			final BooleanValue isGroupValue = track.isGroup();
			final BooleanValue isExpandedValue = track.isGroupExpanded();
			nameValue.addValueObserver(newString -> {
				currentValue = toDisplay(newString, isGroupValue.get(), isExpandedValue.get(), maxLen);
				listeners.forEach(callback -> callback.valueChanged(currentValue));
			});
			isGroupValue.addValueObserver(newGroup -> {
				currentValue = toDisplay(nameValue.get(), newGroup, isExpandedValue.get(), maxLen);
				listeners.forEach(callback -> callback.valueChanged(currentValue));
			});
			isExpandedValue.addValueObserver(newGroupExpand -> {
				currentValue = toDisplay(nameValue.get(), isGroupValue.get(), newGroupExpand, maxLen);
				listeners.forEach(callback -> callback.valueChanged(currentValue));
			});
			currentValue = toDisplay(nameValue.get(), isGroupValue.get(), isExpandedValue.get(), maxLen);
		} else {
			nameValue.addValueObserver(newString -> {
				currentValue = StringUtil.toAsciiDisplay(newString, maxLen);
				listeners.forEach(callback -> callback.valueChanged(currentValue));
			});
			currentValue = StringUtil.toAsciiDisplay(nameValue.get(), maxLen);
		}
	}

	public static String toDisplay(final String name, final boolean isGroupTrack, final boolean isExpanded,
			final int maxLen) {
		if (!isGroupTrack) {
			return StringUtil.toAsciiDisplay(name, maxLen);
		} else if (isExpanded) {
			return StringUtil.toAsciiDisplay(">" + name, maxLen);
		}
		return StringUtil.toAsciiDisplay("*" + name, maxLen);
	}

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
