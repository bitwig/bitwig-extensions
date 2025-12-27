package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class RemotesDisplayControl {
    public static final String BLANK_STRING = "--";
    
    private final StringValue deviceName;
    
    private boolean active = false;
    protected final LineDisplay mainDisplay;
    private final BasicStringValue pageName = new BasicStringValue();
    
    private int pageCount;
    private int pageIndex;
    private String[] pageNames = new String[0];
    
    public RemotesDisplayControl(final StringValue deviceNameValue, final LineDisplay lineDisplay,
        final CursorRemoteControlsPage deviceRemotes) {
        this.mainDisplay = lineDisplay;
        this.deviceName = deviceNameValue;
        deviceNameValue.addValueObserver(this::handleDeviceNameChanged);
        deviceRemotes.selectedPageIndex().addValueObserver(this::handlePageIndex);
        deviceRemotes.pageCount().addValueObserver(this::handlePageCount);
        deviceRemotes.pageNames().addValueObserver(this::handlePageNames);
        pageName.addValueObserver(this::handlePageNameChanged);
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        updateDisplay();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void updateDisplay() {
        if (isActive()) {
            if (deviceName.get().isBlank()) {
                mainDisplay.setText(0, 1, BLANK_STRING);
                mainDisplay.setText(0, 2, BLANK_STRING);
            } else {
                mainDisplay.setText(0, 1, deviceName.get());
                mainDisplay.setText(0, 2, pageName.get().isBlank() ? BLANK_STRING : pageName.get());
            }
        }
    }
    
    private void handleDeviceNameChanged(final String name) {
        if (isActive()) {
            mainDisplay.setText(0, 1, name.isBlank() ? BLANK_STRING : name);
        }
    }
    
    private void handlePageNameChanged(final String page) {
        if (isActive()) {
            mainDisplay.setText(0, 2, page);
        }
    }
    
    private void handlePageNames(final String[] pageNames) {
        this.pageNames = pageNames;
        if (pageIndex < this.pageNames.length) {
            pageName.set(this.pageNames[pageIndex]);
        } else if (pageNames.length == 0) {
            pageName.set(BLANK_STRING);
        }
    }
    
    private void handlePageCount(final int pageCount) {
        if (pageCount == -1) {
            pageName.set(BLANK_STRING);
            return;
        }
        this.pageCount = pageCount;
    }
    
    private void handlePageIndex(final int pageIndex) {
        if (pageIndex == -1) {
            pageName.set(BLANK_STRING);
            return;
        }
        this.pageIndex = pageIndex;
        if (pageIndex < this.pageNames.length) {
            pageName.set(this.pageNames[pageIndex]);
        }
    }
    
    public boolean canScrollRight() {
        return pageIndex < pageCount - 1;
    }
    
    public boolean canScrollLeft() {
        return pageIndex > 0;
    }
    
    
}
