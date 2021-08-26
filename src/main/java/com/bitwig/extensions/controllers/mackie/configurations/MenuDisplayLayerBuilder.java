package com.bitwig.extensions.controllers.mackie.configurations;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;

public class MenuDisplayLayerBuilder {
	private static final int MAX_SLOT_INDEX = 7;
	private final MenuModeLayerConfiguration menuControl;
	private final DisplayLayer menuDisplayLayer;
	int currentSlot = 0;

	public MenuDisplayLayerBuilder(final MenuModeLayerConfiguration menuControl) {
		super();
		this.menuControl = menuControl;
		this.menuDisplayLayer = menuControl.getDisplayLayer(0);
	}

	public void bindBool(final SettableBooleanValue value, final String trueString, final String falseString,
			final ObjectProxy existSource, final String emptyString, final Runnable pressAction) {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuDisplayLayer.bindBool(currentSlot, value, trueString, falseString, existSource, emptyString);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run());
		menuControl.addRingBoolBinding(currentSlot, value);
		currentSlot++;
	}

	public void bindEncAction(final StringValue displayName, final IntConsumer pressAction) {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuDisplayLayer.bindName(1, currentSlot, displayName);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> pressAction.accept(encIndex));
		menuControl.addRingFixedBinding(currentSlot);
		currentSlot++;
	}

	public void bindFixed(final String displayName, final Runnable pressAction) {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuDisplayLayer.bindFixed(currentSlot, displayName);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run());
		menuControl.addRingFixedBinding(currentSlot);
		currentSlot++;
	}

	public void bindBool(final String title, final SettableBooleanValue value) {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuControl.addNameBinding(currentSlot, new BasicStringValue(title));
		menuControl.addValueBinding(currentSlot, value, "< ON >", "<OFF >");
		menuControl.addRingBoolBinding(currentSlot, value);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> value.toggle());
		currentSlot++;
	}

	public void bindValue(final String title, final SettableRangedValue value, final double sensitivity) {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuControl.addNameBinding(currentSlot, new BasicStringValue(title));
		menuControl.addDisplayValueBinding(currentSlot, value.displayedValue());
		menuControl.addEncoderBinding(currentSlot, value, sensitivity);
		currentSlot++;
	}

	public void bindAction(final String title, final Runnable action) {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuControl.addNameBinding(currentSlot, new BasicStringValue(title));
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> action.run());
		currentSlot++;
	}

	public void fillRest() {
		while (currentSlot < 8) {
			menuControl.addRingFixedBinding(currentSlot);
			currentSlot++;
		}
	}

	public void insertEmpty() {
		if (currentSlot > MAX_SLOT_INDEX) {
			return;
		}
		menuControl.addRingFixedBinding(currentSlot);
		currentSlot++;
	}

}
