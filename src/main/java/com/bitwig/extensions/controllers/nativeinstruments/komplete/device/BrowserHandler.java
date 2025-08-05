package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.PopupBrowser;

public class BrowserHandler {
    
    private final PopupBrowser browser;
    private boolean browserOpen;
    private boolean deviceExists;
    private final InsertionPoint insertionPoint;
    private final BooleanValue shiftHeld;
    
    public BrowserHandler(final ControllerHost host, final CursorDevice device, final BooleanValue shiftHeld) {
        this.browser = host.createPopupBrowser();
        this.browser.exists().addValueObserver(this::handleExists);
        device.exists().addValueObserver(this::handleDeviceExists);
        insertionPoint = device.replaceDeviceInsertionPoint();
        this.shiftHeld = shiftHeld;
        browser.shouldAudition().markInterested();
    }
    
    private void handleDeviceExists(final boolean exists) {
        this.deviceExists = exists;
    }
    
    public BooleanValue isOpen() {
        return this.browser.exists();
    }
    
    private void handleExists(final boolean exists) {
        this.browserOpen = exists;
    }
    
    public void navigateNext() {
        if (shiftHeld.get()) {
            browser.shouldAudition().toggle();
            return;
        }
        if (!browserOpen) {
            insertionPoint.browse();
            //host.scheduleTask(() -> browser.selectNextFile(), 100);
        } else {
            browser.selectNextFile();
        }
    }
    
    public void navigatePrevious() {
        if (!browserOpen) {
            insertionPoint.browse();
            //host.scheduleTask(() -> browser.selectPreviousFile(), 100);
        } else {
            browser.selectPreviousFile();
        }
    }
    
    public boolean canNavigateNext() {
        return deviceExists;
    }
    
    public boolean canNavigatePrevious() {
        return deviceExists;
    }
    
    public void confirm() {
        if (browserOpen) {
            browser.commit();
        }
    }
    
    public void cancel() {
        if (browserOpen) {
            browser.cancel();
        }
    }
    
    public void incrementSelection(final int inc) {
        if (!browserOpen) {
            return;
        }
        final int repeats = Math.abs(inc);
        for (int i = 0; i < repeats; i++) {
            if (inc > 0) {
                browser.selectNextFile();
            } else {
                browser.selectPreviousFile();
            }
        }
    }
    
}
