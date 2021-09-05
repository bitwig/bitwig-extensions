package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;

public class NestingNavigator {
	private static final int MAX_SLOTS = 8;
	final CursorDeviceControl deviceControl;
	final BasicStringValue[] sections;
	final Runnable[] actions;

	class Builder {
		int currentSlot = 0;

		public void addAction(final String title, final Runnable action) {
			if (currentSlot < MAX_SLOTS) {
				sections[currentSlot].set(title);
				actions[currentSlot] = action;
				currentSlot++;
			}
		}

		public void complete() {
			while (currentSlot < MAX_SLOTS) {
				sections[currentSlot].set("");
				actions[currentSlot] = null;
				currentSlot++;
			}
		}
	}

	public NestingNavigator(final CursorDeviceControl cursorDeviceControl) {
		super();
		this.deviceControl = cursorDeviceControl;
		final PinnableCursorDevice device = cursorDeviceControl.getCursorDevice();
		sections = new BasicStringValue[MAX_SLOTS];
		actions = new Runnable[MAX_SLOTS];
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
		deviceControl.handleLayerSelection();
	}

	private void selectParent() {
		deviceControl.getCursorDevice().selectParent();
	}

	private void selectPreviousInLayer() {
		deviceControl.navigatePreviousInLayer();
	}

	private void selectNextInLayer() {
		deviceControl.navigateNextInLayer();
	}

	private void selectPrevious() {
		deviceControl.getCursorDevice().selectPrevious();
	}

	private void selectNext() {
		deviceControl.getCursorDevice().selectNext();
	}

	public void evaluateSlots(final boolean isNested, final String[] slotNames, final boolean hasLayers,
			final boolean drumPads) {
		final Builder menuBuilder = new Builder();
		menuBuilder.addAction("<C.PRE", this::selectPrevious);
		menuBuilder.addAction("C.NXT>", this::selectNext);
		if (isNested) {
			menuBuilder.addAction("|PRNT|", this::selectParent);
		}
		for (final String slotName : slotNames) {
			menuBuilder.addAction(StringUtil.limit(slotName, 4) + ">", () -> this.handleSlotSelection(slotName));
		}
		if (hasLayers) {
			menuBuilder.addAction("LAYER>", this::handleLayerSelection);
			menuBuilder.addAction("<L.PR", this::selectPreviousInLayer);
			menuBuilder.addAction("L.NX>", this::selectNextInLayer);
		}
		menuBuilder.complete();
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
