package com.bitwig.extensions.controllers.mackie.value;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.IntegerValue;

public class ModifierValueObject implements IntegerValue {

	public static final int SHIFT = 0x1;
	public static final int OPTION = 0x2;
	public static final int CONTROL = 0x4;
	public static final int ALT = 0x8;

	private int value = 0;
	private boolean isSubscribed = false;
	private final List<IntegerValueChangedCallback> callbacks = new ArrayList<IntegerValueChangedCallback>();

	@Override
	public void markInterested() {
	}

	@Override
	public void addValueObserver(final IntegerValueChangedCallback callback) {
		callbacks.add(callback);
	}

	@Override
	public void addValueObserver(final IntegerValueChangedCallback callback, final int valueWhenUnassigned) {
		addValueObserver(callback);
	}

	@Override
	public boolean isSubscribed() {
		return isSubscribed;
	}

	@Override
	public void setIsSubscribed(final boolean value) {
		isSubscribed = value;
	}

	@Override
	public void subscribe() {
	}

	@Override
	public void unsubscribe() {
	}

	@Override
	public int get() {
		return value;
	}

	public boolean notSet() {
		return value == 0;
	}

	private void notifyValueChanged() {
		callbacks.forEach(callback -> callback.valueChanged(value));
	}

	public boolean isSet(final int value) {
		return this.value == value;
	}

	public boolean isSet(final int... values) {
		for (final int v : values) {
			if ((v & value) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return if the shift button is held down
	 */
	public boolean isShiftSet() {
		return (value & SHIFT) != 0;
	}

	/**
	 * @return if exactly the shift button is held down
	 */
	public boolean isShift() {
		return value == SHIFT;
	}

	public void setShift(final boolean shift) {
		if (shift && !isShiftSet()) {
			value |= SHIFT;
			notifyValueChanged();
		} else if (!shift && isShiftSet()) {
			value &= ~SHIFT;
			notifyValueChanged();
		}
	}

	/**
	 * @return if the option button is held down
	 */
	public boolean isOptionSet() {
		return (value & OPTION) != 0;
	}

	/**
	 * @return if exactly the option button is held down
	 */
	public boolean isOption() {
		return value == OPTION;
	}

	public void setOption(final boolean option) {
		if (option && !isOptionSet()) {
			value |= OPTION;
			notifyValueChanged();
		} else if (!option && isOptionSet()) {
			value &= ~OPTION;
			notifyValueChanged();
		}
	}

	/**
	 * @return if the control button is held down
	 */
	public boolean isControlSet() {
		return (value & CONTROL) != 0;
	}

	/**
	 * @return if exactly only the control button is held down
	 */
	public boolean isControl() {
		return value == CONTROL;
	}

	public void setControl(final boolean control) {
		if (control && !isControlSet()) {
			value |= CONTROL;
			notifyValueChanged();
		} else if (!control && isControlSet()) {
			value &= ~CONTROL;
			notifyValueChanged();
		}
	}

	public boolean isAltSet() {
		return (value & ALT) != 0;
	}

	public boolean isAlt() {
		return value == ALT;
	}

	public void setAlt(final boolean alt) {
		if (alt && !isAltSet()) {
			value |= ALT;
			notifyValueChanged();
		} else if (!alt && isAltSet()) {
			value &= ~ALT;
			notifyValueChanged();
		}
	}

}
