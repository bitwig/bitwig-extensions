package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.controller.api.CursorBrowserResultItem;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;

public class BrowserLayer extends Layer {

    private static final String NO_SELECTION_MSG = "<no selection>";
    public static final String FORMAT_BROWSE_TITLE = "Browser: %s";

    private final MiniLab3Extension driver;

    private final PopupBrowser browser;
    private final PinnableCursorDevice cursorDevice;
    private final CursorTrack cursorTrack;
    private final CursorBrowserResultItem resultCursorItem;
    private String[] contentTypeNames = new String[0];
    private String currentContentType = "";

    private boolean browsingInitiated = false;
    private boolean isSelecting = false;

    public BrowserLayer(final MiniLab3Extension driver) {
        super(driver.getLayers(), "BROWSER_LAYER");
        browser = driver.getHost().createPopupBrowser();
        cursorDevice = driver.getCursorDevice();
        cursorTrack = driver.getCursorTrack();
        cursorDevice.exists().markInterested();
        this.driver = driver;
        browser.exists().markInterested();
        browser.exists().addValueObserver(exists -> {
            setIsActive(exists);
            if (browsingInitiated) {
                browser.shouldAudition().set(false);
                isSelecting = true;
            }
            driver.browserDisplayMode(exists);
            if (!exists) {
                browsingInitiated = false;
            } else {
                updateInfo();
                isSelecting = true;
            }
        });
        browser.contentTypeNames().addValueObserver(contentTypeNames -> this.contentTypeNames = contentTypeNames);
        browser.selectedContentTypeIndex().markInterested();
        resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();
        resultCursorItem.exists()
                .addValueObserver(selectionExists -> driver.getOled()
                        .sendTextInfo(DisplayMode.BROWSER, String.format(FORMAT_BROWSE_TITLE, currentContentType),
                                selectionExists ? resultCursorItem.name().get() : NO_SELECTION_MSG, false));
        resultCursorItem.name()
                .addValueObserver(selected -> driver.getOled()
                        .sendTextInfo(DisplayMode.BROWSER, String.format(FORMAT_BROWSE_TITLE, currentContentType),
                                selected, false));

        browser.selectedContentTypeIndex().addValueObserver(selected -> {
            if (selected < contentTypeNames.length) {
                currentContentType = contentTypeNames[selected];
                final boolean selectionExists = resultCursorItem.exists().get();
                driver.getOled()
                        .sendTextInfo(DisplayMode.BROWSER, String.format(FORMAT_BROWSE_TITLE, currentContentType),
                                selectionExists ? resultCursorItem.name().get() : NO_SELECTION_MSG, false);
            }
        });

        final RelativeHardwareKnob mainEncoder = driver.getMainEncoder();
        final RelativeHardwareKnob shiftEncoder = driver.getShiftMainEncoder();
        driver.bindEncoder(this, mainEncoder, inc -> scrollResultItem(resultCursorItem, inc));
        driver.bindEncoder(this, shiftEncoder, this::scrollContentType);
    }

    public void updateInfo() {
        driver.getOled()
                .sendTextInfo(DisplayMode.BROWSER, String.format(FORMAT_BROWSE_TITLE, currentContentType),
                        resultCursorItem.exists().get() ? resultCursorItem.name().get() : NO_SELECTION_MSG, false);
    }

    private void scrollResultItem(final CursorBrowserResultItem resultCursorItem, final int inc) {
        driver.getOled().enableValues(DisplayMode.BROWSER);
        browser.shouldAudition().set(false);
        isSelecting = true;
        if (inc > 0) {
            resultCursorItem.selectNext();
        } else {
            resultCursorItem.selectPrevious();
        }
    }

    private void scrollContentType(final int inc) {
        final int index = browser.selectedContentTypeIndex().get();
        if (index >= 0 && index < contentTypeNames.length) {
            driver.getOled().enableValues(DisplayMode.BROWSER);
            int next = index + inc;
            next = next < 0 ? contentTypeNames.length - 1 : (next >= contentTypeNames.length) ? 0 : next;
            browser.selectedContentTypeIndex().set(next);
        }
    }

    public void shiftPressAction() {
        if (browser.exists().get()) {
            browser.cancel();
        } else {
            if (cursorDevice.exists().get()) {
                browsingInitiated = true;
                cursorDevice.replaceDeviceInsertionPoint().browse();
            } else {
                browsingInitiated = true;
                cursorTrack.endOfDeviceChainInsertionPoint().browse();
            }
        }
    }

    public void pressAction(final boolean down) {
        if (!down || !browser.exists().get()) {
            return;
        }
        if (isSelecting) {
            browser.shouldAudition().set(true);
            isSelecting = false;
            driver.getOled().enableValues(DisplayMode.BROWSER_INFO);
            driver.getOled().sendTextInfo(DisplayMode.BROWSER_INFO, "", "Click again to load", true);
        } else {
            browser.commit();
            driver.notifyTurn();  // makes sure release doesn't trigger scene
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }


}
