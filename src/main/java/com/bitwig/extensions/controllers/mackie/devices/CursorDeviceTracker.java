package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.section.InfoSource;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class CursorDeviceTracker implements DeviceManager {
	private final CursorRemoteControlsPage remote;
	private InfoSource infoSource = null;
	private String currentDeviceInfo = "";
	private String currentPageInfo = "";
	private DisplayLayer infoLayer;
	private DeviceTypeFollower currentFollower;

	private final PinnableCursorDevice cursorDevice;

	public CursorDeviceTracker(final CursorDeviceControl deviceControl, final DeviceTypeFollower initial) {
		cursorDevice = deviceControl.getCursorDevice();
		remote = deviceControl.getRemotes();
		currentFollower = initial;

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
		cursorDevice.name().addValueObserver(newName -> {
			currentDeviceInfo = getDeviceInfo(newName, cursorDevice.presetName().get());
			if (infoSource == InfoSource.NAV_VERTICAL) {
				infoLayer.setMainText(currentDeviceInfo, "", false);
				infoLayer.invokeRefresh();
			}
		});
		cursorDevice.presetName().addValueObserver(newName -> {
			currentDeviceInfo = getDeviceInfo(cursorDevice.name().get(), newName);
			if (infoSource == InfoSource.NAV_VERTICAL) {
				infoLayer.setMainText(currentDeviceInfo, "", false);
				infoLayer.invokeRefresh();
			}
		});

		cursorDevice.isEnabled().markInterested();
		currentDeviceInfo = getDeviceInfo(cursorDevice.name().get(), cursorDevice.presetName().get());
		currentPageInfo = getPageInfo(remote.pageNames().get(), remote.selectedPageIndex().get());
	}

	@Override
	public void setCurrentFollower(final DeviceTypeFollower currentFollower) {
		this.currentFollower = currentFollower;
	}

	@Override
	public DeviceTypeFollower getCurrentFollower() {
		return currentFollower;
	}

	private String getPageInfo(final String[] pages, final int index) {
		if (pages.length == 0 || index < 0 || index >= pages.length) {
			return "No parameter pages";
		}
		return "Parameter Page : " + pages[index];
	}

	@Override
	public int getPageCount() {
		return remote.pageCount().get();
	}

	private String getDeviceInfo(final String deviceName, final String presetName) {
		return currentFollower.getPotMode().getTypeName() + ":" + deviceName + "   Preset:" + presetName;
	}

	@Override
	public boolean isSpecificDevicePresent() {
		return currentFollower.getFocusDevice().exists().get();
	}

	@Override
	public void initiateBrowsing(final BrowserConfiguration browser, final Type type) {
		if (browser.isBrowserActive()) {
			browser.forceClose();
		}
		browser.setBrowsingInitiated(true, type);
		currentFollower.initiateBrowsing();
	}

	@Override
	public void addBrowsing(final BrowserConfiguration browser, final boolean after) {
		browser.setBrowsingInitiated(true, Type.DEVICE);
		if (after) {
			currentFollower.addNewDeviceAfter();
		} else {
			currentFollower.addNewDeviceBefore();
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
			currentDeviceInfo = getDeviceInfo(cursorDevice.name().get(), cursorDevice.presetName().get());
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

	@Override
	public Parameter getParameter(final int index) {
		return remote.getParameter(index);
	}

	@Override
	public ParameterPage getParameterPage(final int index) {
		return null;
	}

	@Override
	public void navigateDeviceParameters(final int direction) {
		if (direction < 0) {
			remote.selectPrevious();
		} else {
			remote.selectNext();
		}
	}

	@Override
	public void handleResetInvoked(final int index, final ModifierValueObject modifier) {
	}

}
