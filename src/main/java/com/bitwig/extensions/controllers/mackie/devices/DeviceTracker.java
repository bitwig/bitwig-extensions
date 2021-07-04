package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.layer.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.InfoSource;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class DeviceTracker implements DeviceManager {
	private final String typeName;
	private final Device device;
	private final CursorRemoteControlsPage remote;
	private final DeviceBank deviceBank;
	private final BooleanValue cursorOnDevice;
	private final boolean canTrackMultiple;
	private DeviceBank overallBank;
	private int bankIndex = 0;

	private InfoSource infoSource = null;
	private String currentDeviceInfo = "";
	private String currentPageInfo = "";
	private DisplayLayer infoLayer;

	public DeviceTracker(final MackieMcuProExtension driver, final String typeName, final DeviceMatcher matcher,
			final boolean canTrackMultiple) {
		deviceBank = driver.getCursorTrack().createDeviceBank(1);
		this.typeName = typeName;
		this.canTrackMultiple = canTrackMultiple;
		deviceBank.setDeviceMatcher(matcher);
		device = deviceBank.getItemAt(0);
		deviceBank.canScrollForwards().markInterested();
		cursorOnDevice = device.createEqualsValue(driver.getCursorDevice());
		remote = device.createCursorRemoteControlsPage(8);

		remote.pageNames().addValueObserver(pages -> {
			currentPageInfo = getPageInfo(pages, remote.selectedPageIndex().get());
			if (infoSource == InfoSource.NAV_HORIZONTAL) {
				infoLayer.setMainText(currentPageInfo, "", false);
				infoLayer.invokeRefresh();
			}
		});
		remote.selectedPageIndex().addValueObserver(pageIndex -> {
			currentPageInfo = getPageInfo(remote.pageNames().get(), pageIndex);
			if (infoSource == InfoSource.NAV_HORIZONTAL) {
				infoLayer.setMainText(currentPageInfo, "", false);
				infoLayer.invokeRefresh();
			}
		});

		device.name().addValueObserver(newName -> {
			currentDeviceInfo = getDeviceInfo(newName, device.presetName().get());
			if (infoSource == InfoSource.NAV_VERTICAL) {
				infoLayer.setMainText(currentDeviceInfo, "", false);
				infoLayer.invokeRefresh();
			}
		});
		device.presetName().addValueObserver(newName -> {
			currentDeviceInfo = getDeviceInfo(device.name().get(), newName);
			if (infoSource == InfoSource.NAV_VERTICAL) {
				infoLayer.setMainText(currentDeviceInfo, "", false);
				infoLayer.invokeRefresh();
			}
		});

		device.exists().markInterested();
		device.isEnabled().markInterested();

		currentDeviceInfo = getDeviceInfo(device.name().get(), device.presetName().get());
		currentPageInfo = getPageInfo(remote.pageNames().get(), remote.selectedPageIndex().get());

		if (canTrackMultiple) {
			initBankTracking(driver);
		}
	}

	@Override
	public void setInfoLayer(final DisplayLayer infoLayer) {
		this.infoLayer = infoLayer;
	}

	@Override
	public void enableInfo(final InfoSource type) {
		infoSource = type;
		if (infoSource == InfoSource.NAV_VERTICAL) {
			currentDeviceInfo = getDeviceInfo(device.name().get(), device.presetName().get());
			infoLayer.setMainText(currentDeviceInfo, "", false);
		} else if (infoSource == InfoSource.NAV_HORIZONTAL) {
			currentPageInfo = getPageInfo(remote.pageNames().get(), remote.selectedPageIndex().get());
			infoLayer.setMainText(currentPageInfo, "", false);
		}
	}

	@Override
	public void disableInfo() {
		infoSource = null;
	}

	@Override
	public InfoSource getInfoSource() {
		return infoSource;
	}

	private String getPageInfo(final String[] pages, final int index) {
		if (pages.length == 0 || index < 0 || index >= pages.length) {
			return "No parameter pages";
		}
		return "Parameter Page : " + pages[index];
	}

	private String getDeviceInfo(final String deviceName, final String presetName) {
		return typeName + ":" + deviceName + "   Preset:" + presetName;
	}

	private void initBankTracking(final MackieMcuProExtension driver) {
		overallBank = driver.getCursorTrack().createDeviceBank(8);
		overallBank.canScrollBackwards().markInterested();
		overallBank.canScrollForwards().markInterested();
		for (int i = 0; i < overallBank.getSizeOfBank(); i++) {
			final int which = i;
			final BooleanValue evo = overallBank.getItemAt(which).createEqualsValue(device);
			evo.addValueObserver(v -> {
				if (v) {
					if (which == 0 && overallBank.canScrollBackwards().get()) {
						overallBank.scrollBackwards();
					} else if (which == overallBank.getSizeOfBank() - 1 && overallBank.canScrollForwards().get()) {
						overallBank.scrollForwards();
					}
					bankIndex = which;
				}
			});
		}
	}

	@Override
	public void moveDeviceToLeft() {
		if (overallBank == null) {
			return;
		}
		if (bankIndex > 0) {
			final Device nextDevice = overallBank.getItemAt(bankIndex - 1);
			nextDevice.beforeDeviceInsertionPoint().moveDevices(device);
		}
	}

	@Override
	public void moveDeviceToRight() {
		if (overallBank == null) {
			return;
		}
		if (bankIndex < overallBank.getSizeOfBank() - 1) {
			final Device nextDevice = overallBank.getItemAt(bankIndex + 1);
			nextDevice.afterDeviceInsertionPoint().moveDevices(device);
		}
	}

	@Override
	public void removeDevice() {
		device.deleteObject();
	}

	public void handleReset(final int index, final Parameter parameter, final ModifierValueObject modifier) {
		if (modifier.isOptionSet()) {
			device.isEnabled().toggle();
		} else if (modifier.isControlSet()) {
			parameter.restoreAutomationControl();
		} else {
			parameter.reset();
		}
	}

	@Override
	public Device getDevice() {
		return device;
	}

	@Override
	public BooleanValue getCursorOnDevice() {
		return cursorOnDevice;
	}

	@Override
	public CursorRemoteControlsPage getRemote() {
		return remote;
	}

	@Override
	public boolean isCanTrackMultiple() {
		return canTrackMultiple;
	}

	public void selectPreviousDevice() {
		deviceBank.scrollBackwards();
	}

	public void selectNextDevice() {
		deviceBank.scrollForwards();
	}

	public void selectPreviousParameterPage() {
		remote.selectPrevious();
	}

	public void selectNextParameterPage() {
		remote.selectNext();
	}

	public Parameter getParameter(final int i) {
		return remote.getParameter(i);
	}

	public boolean exists() {
		return device.exists().get();
	}
}
