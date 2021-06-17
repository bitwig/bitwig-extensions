package com.bitwig.extensions.controllers.mackie.bindings;

import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.value.DisplayTextValue;
import com.bitwig.extensions.framework.Binding;

public class FullDisplayBinding extends Binding<DisplayTextValue, LcdDisplay> {

	private final boolean centered;

	public FullDisplayBinding(final DisplayTextValue source, final LcdDisplay target, final boolean isCentered) {
		super(source, target);
		source.addListener(this::handleValueChange);
		this.centered = isCentered;
	}

	private void handleValueChange(final int index, final String value) {
		if (isActive()) {
			if (centered) {
				getTarget().centerText(index, value);
			} else {
				getTarget().sendToDisplay(index, value);
			}
		}
	}

	@Override
	protected void deactivate() {
		getTarget().clearText();
	}

	@Override
	protected void activate() {
		if (centered) {
			getTarget().centerText(0, getSource().getLine(0));
			getTarget().centerText(1, getSource().getLine(1));
		} else {
			getTarget().sendToDisplay(0, getSource().getLine(0));
			getTarget().sendToDisplay(1, getSource().getLine(1));
		}
	}

}
