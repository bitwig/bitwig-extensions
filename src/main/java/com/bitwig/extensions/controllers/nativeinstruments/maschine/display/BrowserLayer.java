package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.JogWheelDestination;

public class BrowserLayer extends DisplayLayer implements JogWheelDestination {

	private final PopupBrowser browser;
	private String selectedContentType = "";
	private String selectedElement = "";
	private String categoryElement = "";
	private String creatorElement = "";
	private final CursorBrowserFilterItem categoryItem;
	private final CursorBrowserFilterItem creatorItem;

	public BrowserLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
		browser = driver.getBrowser();
		final ControllerHost host = driver.getHost();
		final ModeButton[] buttons = driver.getDisplayButtons();
		browser.selectedContentTypeIndex().markInterested();
		browser.selectedContentTypeName().addValueObserver(content -> {
			selectedContentType = content;
			updateDisplay();
		});

		categoryItem = (CursorBrowserFilterItem) browser.categoryColumn().createCursorItem();
		categoryItem.name().addValueObserver(v -> {
			categoryElement = v.trim();
			updateDisplay();
		});
		categoryItem.hasNext().markInterested();
		categoryItem.hasPrevious().markInterested();

		creatorItem = (CursorBrowserFilterItem) browser.creatorColumn().createCursorItem();
		creatorItem.name().addValueObserver(v -> {
			creatorElement = v.trim();
			updateDisplay();
		});
		creatorItem.hasNext().markInterested();
		creatorItem.hasPrevious().markInterested();

		final BrowserResultsItem resultCursorItem = browser.resultsColumn().createCursorItem();
		resultCursorItem.name().addValueObserver(v -> {
			selectedElement = v;
			updateDisplay();
		});
		bindPressed(buttons[3], browser.shouldAudition());
		bindLightState(buttons[3], browser.shouldAudition());
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
		bind(knobs[0], createIncrementBinder(host, this::scrollContentType));
		bind(knobs[2], createIncrementBinder(host, this::scrollCategory));
		bind(knobs[4], createIncrementBinder(host, this::scrollCreator));
		bind(knobs[6], createIncrementBinder(host, this::scrollFile));
	}

	private void scrollContentType(final int increment) {
		final int v = browser.selectedContentTypeIndex().get();
		browser.selectedContentTypeIndex().set(v + increment);
	}

	private void scrollFile(final int increment) {
		if (increment > 0) {
			browser.selectNextFile();
		} else {
			browser.selectPreviousFile();
		}
	}

	private void scrollCreator(final int increment) {
		if (increment > 0) {
			creatorItem.selectNext();
		} else {
			creatorItem.selectPrevious();
		}
	}

	private void scrollCategory(final int increment) {
		if (increment > 0) {
			categoryItem.selectNext();
		} else {
			categoryItem.selectPrevious();
		}
	}

	private void updateDisplay() {
		sendToDisplay(0, DisplayUtil.padString("BROWSE ", 20) + "|<AUDIT>");
		sendToDisplay(2,
				DisplayUtil.padString(selectedContentType, 14) + "|" + DisplayUtil.padString(categoryElement, 14));
		sendToDisplay(1, "SEL:" + selectedElement);
		sendToDisplay(3, DisplayUtil.padString(creatorElement, 14));
	}

	@Override
	protected void notifyEncoderTouched(final int index, final boolean v) {
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
	}

	@Override
	protected void doNotifyMacroDown(final boolean active) {
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		setKnobSensitivity(1.0);
		updateDisplay();
	}

	@Override
	public void jogWheelAction(final int increment) {
		if (increment > 0) {
			browser.selectNextFile();
		} else {
			browser.selectPreviousFile();
		}
	}

	@Override
	public void jogWheelPush(final boolean push) {
		if (push) {
			browser.commit();
		}
	}
}
