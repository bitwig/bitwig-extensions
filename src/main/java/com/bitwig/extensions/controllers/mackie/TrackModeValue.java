package com.bitwig.extensions.controllers.mackie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extensions.controllers.mackie.VPotMode.Assign;

public class TrackModeValue extends BooleanValueObject {

	private final List<BooleanValueChangedCallback> callbacks = new ArrayList<BooleanValueChangedCallback>();
	private final Map<VPotMode, Boolean> modes = new HashMap<VPotMode, Boolean>();
	private VPotMode mode = VPotMode.PAN;

	public TrackModeValue() {
		super();
		for (final VPotMode vPotMode : VPotMode.values()) {
			modes.put(vPotMode, vPotMode.getAssign() == Assign.CHANNEL);
		}
	}

	@Override
	public void toggle() {
		if (this.mode.getAssign() == Assign.BOTH) {
			final boolean current = !modes.get(this.mode);
			modes.put(this.mode, current);
			update(current);
		}
	}

	public VPotMode getMode() {
		return mode;
	}

	public void setMode(final VPotMode mode) {
		if (this.mode != mode) {
			final boolean previous = modes.get(this.mode);
			this.mode = mode;
			final boolean newstate = modes.get(this.mode);
			if (previous != newstate) {
				update(newstate);
			}
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
	public void set(final boolean value) {
		if (this.mode.getAssign() == Assign.BOTH) {
			final boolean current = modes.get(this.mode);
			if (value != current) {
				modes.put(this.mode, value);
				update(current);
			}
		}
	}

	public void update(final boolean current) {
		for (final BooleanValueChangedCallback booleanValueChangedCallback : callbacks) {
			booleanValueChangedCallback.valueChanged(current);
		}
	}

	@Override
	public boolean get() {
		return modes.get(this.mode);
	}

}
