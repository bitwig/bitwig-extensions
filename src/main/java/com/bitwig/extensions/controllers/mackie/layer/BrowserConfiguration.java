package com.bitwig.extensions.controllers.mackie.layer;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;
import com.bitwig.extension.controller.api.CursorBrowserResultItem;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.CombinedStringValueObject;
import com.bitwig.extensions.controllers.mackie.value.StringIntValueObject;
import com.bitwig.extensions.framework.Layer;

public class BrowserConfiguration extends LayerConfiguration {

	public enum Type {
		DEVICE, PRESET;
	}

	private final PopupBrowser browser;
	private LayerConfiguration previousConfig = null;
	private boolean browsingInitiated = false;
	private boolean resetState = false;

	private final CursorBrowserFilterItem categoryItem;
	private final CursorBrowserFilterItem tagItem;
	private final CursorBrowserFilterItem fileTypeItem;
	private CursorBrowserFilterItem deviceTypeItem;
	// private CursorBrowserFilterItem deviceItem;
	private FilterLayerConfig deviceConfig;
	private FilterLayerConfig currentConfig;
	private FilterLayerConfig presetConfig;
	private CursorBrowserResultItem resultCursorItem;
	private StringIntValueObject resultValue;

	static class FilterLayerConfig {
		private final EncoderLayer encoderLayer;
		private final DisplayLayer displayLayer;

		public FilterLayerConfig(final String name, final MixControl mixControl) {
			encoderLayer = new EncoderLayer(mixControl, name);
			encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
			displayLayer = new DisplayLayer("DISP_" + name, mixControl);
		}

		public EncoderLayer getEncoderLayer() {
			return encoderLayer;
		};

		public DisplayLayer getDisplayLayer() {
			return displayLayer;
		}

	}

	public BrowserConfiguration(final String name, final MixControl mixControl, final ControllerHost host,
			final PopupBrowser browser) {
		super(name, mixControl);
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		final String baseName = name + "_ENCODER_LAYER_" + sectionIndex;
		deviceConfig = new FilterLayerConfig("DEV_" + baseName, mixControl);
		presetConfig = new FilterLayerConfig("PRESET_" + baseName, mixControl);
		currentConfig = deviceConfig;
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
		resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();
		resultValue = new StringIntValueObject(resultCursorItem.name(), categoryItem.hitCount(), resultCursorItem,
				"FOUND ITEMS=%d");

		setUpDeviceBrowsing(deviceConfig, mixControl, host, browser);
		setUpPresetBrowsing(presetConfig, mixControl, host, browser);
	}

	private void setUpPresetBrowsing(final FilterLayerConfig config, final MixControl mixControl,
			final ControllerHost host, final PopupBrowser browser2) {
		final HardwareButton enterButton = mixControl.getDriver().getEnterButton();
		final HardwareButton cancelButton = mixControl.getDriver().getCancelButton();
		final EncoderLayer encoderLayer = config.getEncoderLayer();
		final DisplayLayer displayLayer = config.getDisplayLayer();
		final MixerSectionHardware hwControls = mixControl.getHwControls();

	}

	private void setUpDeviceBrowsing(final FilterLayerConfig config, final MixControl mixControl,
			final ControllerHost host, final PopupBrowser browser) {
		final HardwareButton enterButton = mixControl.getDriver().getEnterButton();
		final HardwareButton cancelButton = mixControl.getDriver().getCancelButton();
		final EncoderLayer encoderLayer = config.getEncoderLayer();
		final DisplayLayer displayLayer = config.getDisplayLayer();
		final MixerSectionHardware hwControls = mixControl.getHwControls();

		bindBrowserItem(0, config, hwControls, host, deviceTypeItem, "Type");
		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(0),
				hwControls.createAction(() -> browser.shouldAudition().toggle())));

		final CursorBrowserFilterItem locationItem = (CursorBrowserFilterItem) browser.locationColumn()
				.createCursorItem();

		bindBrowserItem(1, config, hwControls, host, locationItem, "DevLoc");
		locationItem.hitCount().markInterested();

		bindBrowserItem(2, config, hwControls, host, fileTypeItem, "FlType");

		bindBrowserItem(3, config, hwControls, host, categoryItem, "Catgry");

		final CursorBrowserFilterItem creatorItem = (CursorBrowserFilterItem) browser.creatorColumn()
				.createCursorItem();
		bindBrowserItem(4, config, hwControls, host, creatorItem, "Creatr");

		displayLayer.bindName(0, 5, new CombinedStringValueObject("<Cncl>"));
		final HardwareActionBindable cancelAction = hwControls.createAction(() -> browser.cancel());
		final HardwareActionBindable commitAction = hwControls.createAction(() -> browser.commit());

		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(5), cancelAction));
		encoderLayer.bindPressed(cancelButton, cancelAction);

		displayLayer.bindName(0, 7, new CombinedStringValueObject("<Okay>"), resultCursorItem, "<---->");
		displayLayer.bindName(1, 5, 3, resultValue, "<>");

		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(7), commitAction));
		encoderLayer.bindPressed(enterButton, commitAction);
		final RelativeHardwarControlBindable resultSelectionBinding = createIncrementBinder(host, v -> {
			if (v < 0) {
				browser.selectPreviousFile();
			} else {
				browser.selectNextFile();
			}
		});
		encoderLayer.bind(hwControls.getEncoder(5), resultSelectionBinding);
		encoderLayer.bind(hwControls.getEncoder(6), resultSelectionBinding);
		encoderLayer.bind(hwControls.getEncoder(7), resultSelectionBinding);
	}

	private void bindBrowserItem(final int index, final FilterLayerConfig config, final MixerSectionHardware hwControls,
			final ControllerHost host, final CursorBrowserFilterItem browserCursorItem, final String name) {
		final EncoderLayer encoderLayer = deviceConfig.getEncoderLayer();
		final DisplayLayer displayLayer = deviceConfig.getDisplayLayer();

		displayLayer.bindName(0, index, new CombinedStringValueObject(name), browserCursorItem, "");
		displayLayer.bindNameTemp(1, index, 3, browserCursorItem.name(), browserCursorItem, "");
		encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 11, RingDisplayType.FILL_LR_0));
		final RelativeHardwareKnob encoder = hwControls.getEncoder(index);

		final RelativeHardwarControlBindable binding = createIncrementBinder(host, v -> {
			if (v < 0) {
				browserCursorItem.selectPrevious();
			} else {
				browserCursorItem.selectNext();
			}
			displayLayer.tickExpansion(index);
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
		return currentConfig.getEncoderLayer().isActive();
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
		return currentConfig.getEncoderLayer();
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		return currentConfig.getDisplayLayer();
	}

}
