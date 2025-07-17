package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.PopupBrowser;

public class BrowserHandler {
    
    private final PopupBrowser browser;
    private boolean browserOpen;
    private final CursorDevice device;
    private boolean deviceExists;
    private final InsertionPoint insertionPoint;
    private final ControllerHost host;
    private final BooleanValue shiftHeld;
    private boolean wasOpenedByDevice = false;
    
    public BrowserHandler(final ControllerHost host, final CursorDevice device, final BooleanValue shiftHeld) {
        this.host = host;
        this.browser = host.createPopupBrowser();
        this.browser.exists().addValueObserver(this::handleExists);
        this.device = device;
        this.device.exists().addValueObserver(this::handleDeviceExists);
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
            host.scheduleTask(() -> browser.selectNextFile(), 100);
            wasOpenedByDevice = true;
        } else {
            browser.selectNextFile();
        }
    }
    
    public void navigatePrevious() {
        if (!browserOpen) {
            insertionPoint.browse();
            host.scheduleTask(() -> browser.selectPreviousFile(), 100);
            wasOpenedByDevice = false;
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
        wasOpenedByDevice = false;
    }
    
    public void cancel() {
        if (browserOpen) {
            browser.cancel();
        }
        wasOpenedByDevice = false;
    }
    
    public void incrementSelection(final int inc) {
        if (!browserOpen) {
            return;
        }
        
        if (inc > 0) {
            browser.selectNextFile();
        } else {
            browser.selectPreviousFile();
        }
    }
    
    public void forceCancel() {
        if (wasOpenedByDevice) {
            browser.cancel();
        }
    }
}
