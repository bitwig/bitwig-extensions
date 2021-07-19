package com.bitwig.extensions.controllers.mackie.layer;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.CombinedStringValueObject;
import com.bitwig.extensions.framework.Layer;

public class BrowserConfiguration extends LayerConfiguration {

	public enum Type {
		DEVICE, PRESET;
	}

	private final EncoderLayer encoderLayer;
	private final DisplayLayer displayLayer;

	private final PopupBrowser browser;
	private LayerConfiguration previousConfig = null;
	private boolean browsingInitiated = false;
	private boolean resetState = false;

	private final CursorBrowserFilterItem categoryItem;
	private final CursorBrowserFilterItem tagItem;
	private final CursorBrowserFilterItem fileTypeItem;
	private CursorBrowserFilterItem deviceTypeItem;
	// private CursorBrowserFilterItem deviceItem;

	public BrowserConfiguration(final String name, final MixControl mixControl, final ControllerHost host,
			final PopupBrowser browser) {
		super(name, mixControl);
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		encoderLayer = new EncoderLayer(mixControl, name + "_ENCODER_LAYER_" + sectionIndex);
		encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
		displayLayer = new DisplayLayer(name, this.mixControl);
		// deviceItem = (CursorBrowserFilterItem)
		// browser.deviceColumn().createCursorItem();

		this.browser = browser;
		this.browser.exists().addValueObserver(browserNowOpen -> {
			if (!browsingInitiated) {
				return;
			}
			if (browserNowOpen) {
				browser.shouldAudition().set(false);
				browser.selectedContentTypeIndex().set(0);
				resetState = true;
				previousConfig = mixControl.getCurrentConfig();
				mixControl.setConfiguration(this);
			} else if (previousConfig != null) {
				if (resetState) {
					mixControl.setConfiguration(previousConfig);
				}
				browsingInitiated = false;
			}
		});
		deviceTypeItem = (CursorBrowserFilterItem) browser.deviceTypeColumn().createCursorItem();
		fileTypeItem = (CursorBrowserFilterItem) browser.fileTypeColumn().createCursorItem();
		categoryItem = (CursorBrowserFilterItem) browser.categoryColumn().createCursorItem();
		tagItem = (CursorBrowserFilterItem) browser.tagColumn().createCursorItem();

		setUpDeviceBrowsing(mixControl, host, browser);
	}

	private void setUpDeviceBrowsing(final MixControl mixControl, final ControllerHost host,
			final PopupBrowser browser) {
		final HardwareButton enterButton = mixControl.getDriver().getEnterButton();
		final HardwareButton cancelButton = mixControl.getDriver().getCancelButton();

		final MixerSectionHardware hwControls = mixControl.getHwControls();

		bindBrowserItem(0, hwControls, host, deviceTypeItem, "Type");
		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(0),
				hwControls.createAction(() -> browser.shouldAudition().toggle())));

		final CursorBrowserFilterItem locationItem = (CursorBrowserFilterItem) browser.locationColumn()
				.createCursorItem();

		bindBrowserItem(1, hwControls, host, locationItem, "DevLoc");
		locationItem.hitCount().markInterested();

		bindBrowserItem(2, hwControls, host, fileTypeItem, "FlType");

		bindBrowserItem(3, hwControls, host, categoryItem, "Catgry");

		final CursorBrowserFilterItem creatorItem = (CursorBrowserFilterItem) browser.creatorColumn()
				.createCursorItem();
		bindBrowserItem(4, hwControls, host, creatorItem, "Creatr");

		displayLayer.bindName(0, 6, new CombinedStringValueObject("<Cncl>"));
		final HardwareActionBindable cancelAction = hwControls.createAction(() -> browser.cancel());
		final HardwareActionBindable commitAction = hwControls.createAction(() -> browser.commit());

		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(6), cancelAction));
		encoderLayer.bindPressed(cancelButton, cancelAction);

		final BrowserResultsItem resultCursorItem = browser.resultsColumn().createCursorItem();
		displayLayer.bindName(0, 7, new CombinedStringValueObject("<Okay>"), resultCursorItem, "<---->");
		displayLayer.bindName(1, 6, 2, resultCursorItem.name(), resultCursorItem, "");

		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(7), commitAction));
		encoderLayer.bindPressed(enterButton, commitAction);
		final RelativeHardwarControlBindable resultSelectionBinding = createIncrementBinder(host, v -> {
			if (v < 0) {
				browser.selectPreviousFile();
			} else {
				browser.selectNextFile();
			}
		});
		encoderLayer.bind(hwControls.getEncoder(6), resultSelectionBinding);
		encoderLayer.bind(hwControls.getEncoder(7), resultSelectionBinding);

		tagItem.hitCount().markInterested();
	}

	private void bindBrowserItem(final int index, final MixerSectionHardware hwControls, final ControllerHost host,
			final CursorBrowserFilterItem browserCursorItem, final String name) {
		displayLayer.bindName(0, index, new CombinedStringValueObject(name), browserCursorItem, "");
		displayLayer.bindName(1, index, browserCursorItem.name(), browserCursorItem, "");
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 11, RingDisplayType.FILL_LR_0));
		final RelativeHardwareKnob encoder = hwControls.getEncoder(index);

		final RelativeHardwarControlBindable binding = createIncrementBinder(host, v -> {
			if (v < 0) {
				browserCursorItem.selectPrevious();
			} else {
				browserCursorItem.selectNext();
			}
		});
		encoderLayer.bind(encoder, binding);
	}

	protected RelativeHardwarControlBindable createIncrementBinder(final ControllerHost host,
			final IntConsumer consumer) {
		return host.createRelativeHardwareControlStepTarget(//
				host.createAction(() -> consumer.accept(1), () -> "+"),
				host.createAction(() -> consumer.accept(-1), () -> "-"));
	}

	public void setBrowsingInitiated(final boolean browsingInitiated, final Type type) {
		this.browsingInitiated = browsingInitiated;
		deviceTypeItem.selectFirst();
	}

	public boolean isBrowsingInitiated() {
		return browsingInitiated;
	}

	public PopupBrowser getBrowser() {
		return browser;
	}

	public boolean isActive() {
		return encoderLayer.isActive();
	}

	public void forceClose() {
		if (browser.exists().get()) {
			resetState = false;
			browser.cancel();
		}
	}

	@Override
	public Layer getFaderLayer() {
		final boolean isMixerGlobal = this.mixControl.driver.getGlobalViewActive().get();
		if (isMixerGlobal) {
			return this.mixControl.globalGroup.getFaderLayer(ParamElement.VOLUME);
		}
		return this.mixControl.mainGroup.getFaderLayer(ParamElement.VOLUME);
	}

	@Override
	public EncoderLayer getEncoderLayer() {
		return encoderLayer;
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		return displayLayer;
	}

}
