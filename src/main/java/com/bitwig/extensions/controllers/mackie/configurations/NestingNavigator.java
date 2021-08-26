package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;

public class NestingNavigator {
	final CursorDeviceControl deviceControl;
	final BasicStringValue[] sections;
	final Runnable[] actions;
	private final int maxSlots;

	public NestingNavigator(final CursorDeviceControl cursorDeviceControl, final int maxSlots) {
		super();
		this.deviceControl = cursorDeviceControl;
		final PinnableCursorDevice device = cursorDeviceControl.getCursorDevice();
		this.maxSlots = maxSlots;
		sections = new BasicStringValue[maxSlots];
		actions = new Runnable[maxSlots];
		device.isNested().addValueObserver(nested -> evaluateSlots(nested, device.slotNames().get(),
				device.hasLayers().get(), device.hasDrumPads().get()));
		device.slotNames().addValueObserver(slotname -> evaluateSlots(device.isNested().get(), slotname,
				device.hasLayers().get(), device.hasDrumPads().get()));
		device.hasLayers().addValueObserver(hasLayers -> evaluateSlots(device.isNested().get(),
				device.slotNames().get(), hasLayers, device.hasDrumPads().get()));
		device.hasDrumPads().addValueObserver(hasPads -> evaluateSlots(device.isNested().get(),
				device.slotNames().get(), device.hasLayers().get(), hasPads));
		for (int i = 0; i < sections.length; i++) {
			sections[i] = new BasicStringValue("");
		}
		evaluateSlots(device.isNested().get(), device.slotNames().get(), device.hasLayers().get(),
				device.hasDrumPads().get());
	}

	private void handleSlotSelection(final String slotName) {
		deviceControl.getCursorDevice().selectFirstInSlot(slotName);
	}

	private void handleLayerSelection() {
		deviceControl.getCursorDevice().selectFirstInLayer(0);
	}

	private void selectParent() {
		deviceControl.getCursorDevice().selectParent();
	}

	private void handleDrumSelection() {
		deviceControl.focusOnDrumDevice();
	}

	private void selectPrevious() {
		deviceControl.getCursorDevice().selectPrevious();
	}

	private void selectNext() {
		deviceControl.getCursorDevice().selectNext();
	}

	public void evaluateSlots(final boolean isNested, final String[] slotNames, final boolean hasLayers,
			final boolean drumPads) {
		int slot = 0;
		sections[slot].set("<PREV");
		actions[slot] = this::selectPrevious;
		slot++;
		sections[slot].set("NEXT>");
		actions[slot] = this::selectNext;
		slot++;
		if (isNested) {
			sections[slot].set("PARENT");
			actions[slot] = this::selectParent;
			slot++;
		}
		for (final String slotName : slotNames) {
			sections[slot].set(slotName);
			actions[slot] = () -> this.handleSlotSelection(slotName);
			slot++;
		}
		if (hasLayers) {
			sections[slot].set("LAYERS");
			actions[slot] = this::handleLayerSelection;
			slot++;
		}
		if (drumPads) {
			sections[slot].set("PADS");
			actions[slot] = this::handleDrumSelection;
			slot++;
		}
		while (slot < maxSlots) {
			sections[slot].set("");
			actions[slot] = null;
			slot++;
		}

	}

	public StringValue getSection(final int index) {
		return sections[index];
	}

	public void doAction(final int index) {
		if (actions[index] != null) {
			actions[index].run();
		}
	}

}
