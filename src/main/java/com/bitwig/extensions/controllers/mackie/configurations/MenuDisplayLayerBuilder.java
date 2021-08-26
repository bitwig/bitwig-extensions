package com.bitwig.extensions.controllers.mackie.configurations;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;

public class MenuDisplayLayerBuilder {
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
		if (currentSlot > 7) {
			return;
		}
		menuDisplayLayer.bindBool(currentSlot, value, trueString, falseString, existSource, emptyString);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run());
		menuControl.addRingBoolBinding(currentSlot, value);
		currentSlot++;
	}

	public void bindEncAction(final StringValue displayName, final IntConsumer pressAction) {
		if (currentSlot > 7) {
			return;
		}
		menuDisplayLayer.bindName(1, currentSlot, displayName);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> pressAction.accept(encIndex));
		menuControl.addRingFixedBinding(currentSlot);
		currentSlot++;
	}

	public void bindFixed(final String displayName, final Runnable pressAction) {
		if (currentSlot > 7) {
			return;
		}
		menuDisplayLayer.bindFixed(currentSlot, displayName);
		menuControl.addPressEncoderBinding(currentSlot, encIndex -> pressAction.run());
		menuControl.addRingFixedBinding(currentSlot);
		currentSlot++;
	}

	public void insertEmpty() {
		if (currentSlot > 7) {
			return;
		}
		menuControl.addRingFixedBinding(currentSlot);
		currentSlot++;
	}

}
