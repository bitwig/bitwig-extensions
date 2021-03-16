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
	private String tagElement = "";
	private String fileTypeElement = "";
	private String deviceElement = "";
	private final CursorBrowserFilterItem categoryItem;
	private final CursorBrowserFilterItem creatorItem;
	private final CursorBrowserFilterItem tagItem;
	private final CursorBrowserFilterItem fileTypeItem;
	private final CursorBrowserFilterItem deviceItem;

	private int touchedIndex = -1;

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

		deviceItem = (CursorBrowserFilterItem) browser.deviceColumn().createCursorItem();
		deviceItem.name().addValueObserver(v -> {
			deviceElement = v.trim();
			updateDisplay();
		});

		fileTypeItem = (CursorBrowserFilterItem) browser.fileTypeColumn().createCursorItem();
		fileTypeItem.name().addValueObserver(v -> {
			fileTypeElement = v.trim();
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

		tagItem = (CursorBrowserFilterItem) browser.tagColumn().createCursorItem();
		tagItem.name().addValueObserver(v -> {
			tagElement = v.trim();
			updateDisplay();
		});

		final BrowserResultsItem resultCursorItem = browser.resultsColumn().createCursorItem();
		resultCursorItem.name().addValueObserver(v -> {
			selectedElement = v;
			updateDisplay();
		});

		bindPressed(buttons[3], browser.shouldAudition());
		bindLightState(buttons[3], browser.shouldAudition());
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
		bind(knobs[0], createIncrementBinder(host, this::scrollContentType));
		bind(knobs[1], createIncrementBinder(host, this::scrollFileType));
		bind(knobs[2], createIncrementBinder(host, this::scrollDevice));
		bind(knobs[3], createIncrementBinder(host, this::scrollCategory));
		bind(knobs[4], createIncrementBinder(host, this::scrollTag));
		bind(knobs[5], createIncrementBinder(host, this::scrollCreator));
		bind(knobs[6], createIncrementBinder(host, this::scrollFile));
	}

	private void scrollContentType(final int increment) {
		final int v = browser.selectedContentTypeIndex().get();
		browser.selectedContentTypeIndex().set(v + increment);
	}

	private void scrollDevice(final int increment) {
		if (increment > 0) {
			deviceItem.selectNext();
		} else {
			deviceItem.selectPrevious();
		}
	}

	private void scrollFileType(final int increment) {
		if (increment > 0) {
			fileTypeItem.selectNext();
		} else {
			fileTypeItem.selectPrevious();
		}
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

	private void scrollTag(final int increment) {
		if (increment > 0) {
			tagItem.selectNext();
		} else {
			tagItem.selectPrevious();
		}
	}

	private void updateDisplay() {
		sendToDisplay(0, DisplayUtil.padString("BROWSE ", 20) + "|<AUDIT>");

		if (touchedIndex == -1 || touchedIndex > 4) {
			sendToDisplay(2, //
					DisplayUtil.padString(selectedContentType, 6) + "|" + //
							DisplayUtil.padString(fileTypeElement, 6) + "|" + //
							DisplayUtil.padString(deviceElement, 6) + "|" + //
							DisplayUtil.padString(categoryElement, 6));
		} else if (touchedIndex == 0) {
			sendToDisplay(2, "Content:" + selectedContentType);
		} else if (touchedIndex == 1) {
			sendToDisplay(2, "Filetype:" + fileTypeElement);
		} else if (touchedIndex == 2) {
			sendToDisplay(2, "Device:" + deviceElement);
		} else if (touchedIndex == 3) {
			sendToDisplay(2, "Category:" + categoryElement);
		}

		if (touchedIndex == -1 || touchedIndex < 4) {
			sendToDisplay(3, //
					DisplayUtil.padString(tagElement, 6) + "|" + //
							DisplayUtil.padString(creatorElement, 6));
		} else if (touchedIndex == 4) {
			sendToDisplay(3, "Tag:" + tagElement);
		} else if (touchedIndex == 5) {
			sendToDisplay(3, "Creator:" + creatorElement);
		}

		sendToDisplay(1, "SEL:" + selectedElement);
	}

	@Override
	protected void notifyEncoderTouched(final int index, final boolean v) {
		if (!isActive()) {
			return;
		}
		if (v) {
			touchedIndex = index;
		} else {
			if (touchedIndex == index) {
				touchedIndex = -1;
			}
		}
		updateDisplay();
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
