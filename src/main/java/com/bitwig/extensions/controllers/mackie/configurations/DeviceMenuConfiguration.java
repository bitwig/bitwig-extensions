package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class DeviceMenuConfiguration {
	protected final MenuModeLayerConfiguration menuControl;
	protected final MenuModeLayerConfiguration shiftMenuControl;
	private final MixControl mixControl;
	private DeviceManager deviceManager;

	public DeviceMenuConfiguration(final String name, final MixControl mixControl) {
		this.mixControl = mixControl;
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		menuControl = createMenuControl(name + "_MENU_" + sectionIndex, mixControl);
		shiftMenuControl = createMenuControl(name + "_MENU_S_" + sectionIndex, mixControl);
	}

	private MenuModeLayerConfiguration createMenuControl(final String name, final MixControl mixControl) {
		final MenuModeLayerConfiguration menuControl = new MenuModeLayerConfiguration(name, mixControl);
		menuControl.getDisplayLayer(0).displayFullTextMode(true);
		return menuControl;
	}

	public void initMenuControl(final CursorDeviceControl cursorDeviceControl) {
		final PinnableCursorDevice device = cursorDeviceControl.getCursorDevice();
		final MackieMcuProExtension driver = mixControl.getDriver();
		MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menuControl);
		builder.bindBool(device.isEnabled(), "ACTIVE", "<BYPS>", device, "<NODV>", () -> device.isEnabled().toggle());
		builder.bindBool(device.isPinned(), "PINNED", "<PIN>", device, "<NODV>", () -> device.isPinned().toggle());
		builder.bindFixed("<Move", () -> cursorDeviceControl.moveDeviceLeft());
		builder.bindFixed("Move>", () -> cursorDeviceControl.moveDeviceRight());
		builder.bindFixed("REMOVE", () -> cursorDeviceControl.getCursorDevice().deleteObject());
		builder.bindFixed("<ADD", () -> deviceManager.addBrowsing(driver.getBrowserConfiguration(), false));
		builder.bindFixed("ADD>", () -> deviceManager.addBrowsing(driver.getBrowserConfiguration(), true));
		builder.bindFixed("BROWSE",
				() -> deviceManager.initiateBrowsing(driver.getBrowserConfiguration(), Type.DEVICE));

		menuControl.setTextEvaluation(name -> {
			final DisplayLayer menuLayer = menuControl.getDisplayLayer(0);
			menuLayer.setText(0, "Device: " + name, false);
			menuLayer.enableFullTextMode(0, true);
		});

		builder = new MenuDisplayLayerBuilder(shiftMenuControl);

		final NestingNavigator navigator = new NestingNavigator(cursorDeviceControl);
		shiftMenuControl
				.setTextEvaluation(name -> shiftMainTitleEvaluation(name, cursorDeviceControl.getLayerDeviceInfo()));
		cursorDeviceControl.getCursorLayerItem().name().addValueObserver(name -> {
			shiftMainTitleEvaluation(cursorDeviceControl.getCursorDevice().name().get(),
					cursorDeviceControl.getLayerDeviceInfo());
		});

		for (int i = 0; i < 7; i++) {
			final int slotIndex = i;
			builder.bindEncAction(navigator.getSection(slotIndex), index -> navigator.doAction(slotIndex));
		}
	}

	private void shiftMainTitleEvaluation(final String deviceName, final String layerDeviceInfo) {
		final DisplayLayer shiftMenuLayer = shiftMenuControl.getDisplayLayer(0);

		shiftMenuLayer.setText(0, "Device: " + deviceName + " Navigation " + layerDeviceInfo, false);
		shiftMenuLayer.enableFullTextMode(0, true);
	}

	public void evaluateTextDisplay(final String deviceName) {
		menuControl.evaluateTextDisplay(deviceName);
		shiftMenuControl.evaluateTextDisplay(deviceName);
	}

	public boolean applyModifier(final ModifierValueObject modvalue) {
		if (shiftMenuControl.getDisplayLayer(0).isActive() && !modvalue.isShiftSet()) {
			return true;
		}
		if (menuControl.getDisplayLayer(0).isActive() && modvalue.isShiftSet()) {
			return true;
		}
		return false;
	}

	public void setDeviceManager(final DeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}

	public MenuModeLayerConfiguration getMenuControl() {
		return menuControl;
	}

	public MenuModeLayerConfiguration getShiftMenuControl() {
		return shiftMenuControl;
	}

	public EncoderLayer getEncoderLayer() {
		if (mixControl.getDriver().getModifier().isShiftSet()) {
			return shiftMenuControl.getEncoderLayer();
		}
		return menuControl.getEncoderLayer();
	}

	public DisplayLayer getDisplayLayer() {
		if (mixControl.getDriver().getModifier().isShiftSet()) {
			return shiftMenuControl.getDisplayLayer(0);
		}
		return menuControl.getDisplayLayer(0);
	}
}
