package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.HwElements;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.ViewControl;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class BrowserLayer extends Layer {
   private static final int BUTTON_WAIT_UTIL_REPEAT = 500;
   private static final int HOLD_REPEAT_FREQUENCY = 200;
   @Inject
   private MainScreenSection mainScreenSection;
   @Inject
   private LcdDisplay display;

   private final PopupBrowser browser;
   private final CursorBrowserResultItem resultCursorItem;
   private String[] contentTypeNames = new String[0];
   private String currentContentType = "";
   private final CursorBrowserFilterItem categoryItem;

   private final BooleanValueObject browsingInitiated = new BooleanValueObject();
   private PinnableCursorDevice cursorDevice;
   private CursorTrack cursorTrack;
   private boolean enforceDeviceContent = true;

   private int encoderClickCount = 0;

   public BrowserLayer(Layers layers, ControllerHost host) {
      super(layers, "BROWSER_LAYER");
      browser = host.createPopupBrowser();
      browser.exists().addValueObserver(this::handleBrowserOpened);
      browser.contentTypeNames().addValueObserver(contentTypeNames -> this.contentTypeNames = contentTypeNames);
      resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();
      categoryItem = (CursorBrowserFilterItem) browser.categoryColumn().createCursorItem();
      categoryItem.hasNext().markInterested();
      categoryItem.hasPrevious().markInterested();
      //resultCursorItem.exists().addValueObserver(this::handleResultItemCursorExists);
      browser.selectedContentTypeIndex().addValueObserver(selected -> {
         if (selected < contentTypeNames.length) {
            currentContentType = contentTypeNames[selected];
            if (enforceDeviceContent) {
               enforceDeviceContent = false;
               forceDeviceContent();
            }
         }
      });
   }

   private void forceDeviceContent() {
      int index = -1;
      for (int i = 0; i < contentTypeNames.length; i++) {
         if (contentTypeNames[i].equals("Devices")) {
            index = i;
            break;
         }
      }
      if (index != -1) {
         browser.selectedContentTypeIndex().set(index);
      }
   }

   @PostConstruct
   public void init(ViewControl viewCursorControl, HwElements hwElements) {
      cursorDevice = viewCursorControl.getCursorDevice();
      cursorTrack = viewCursorControl.getCursorTrack();
      cursorDevice.exists().markInterested();
      RelativeHardwareKnob encoder = hwElements.getMainEncoder();
      HardwareButton encoderPress = hwElements.getEncoderPress();
      hwElements.bindEncoder(this, encoder, this::mainEncoderAction);
      bindContextButtons();
      ContextPageConfiguration contextPage = mainScreenSection.getContextPage();

      BasicStringValue resultNameValue = new BasicStringValue("");
      resultCursorItem.name()
         .addValueObserver(name -> resultNameValue.set(getResultData(resultCursorItem.exists().get(), name)));
      resultCursorItem.exists()
         .addValueObserver(exists -> resultNameValue.set(getResultData(exists, resultCursorItem.name().get())));


      this.addBinding(new MainViewDisplayBinding(contextPage, display, resultNameValue, categoryItem.name()));
      this.bindPressed(encoderPress, () -> {
         if (resultCursorItem.exists().get()) {
            if (encoderClickCount == 0) {
               display.sendPopup("Click again ", "to load", KeylabIcon.NONE);
               encoderClickCount++;
            } else {
               browser.commit();
               encoderClickCount = 0;
            }
         } else {
            display.sendPopup("Nothing selected ", "", KeylabIcon.NONE);
         }
      });
   }

   private String getResultData(boolean exists, String resultName) {
      return !exists || resultName.isBlank() ? "<no selection>" : resultName;
   }

   private void bindContextButtons() {
      RgbButton context1 = mainScreenSection.getContextButton(0);
      Layer navigationLayer = mainScreenSection.getLayer();
      ContextPageConfiguration contextPage = mainScreenSection.getContextPage();
      context1.bindPressed(navigationLayer, this::toggleBrowserAction);
      FooterIconDisplayBinding footerIconBinding = new FooterIconDisplayBinding(contextPage, display, browsingInitiated,
         0, ContextPart.FrameType.FRAME_SMALL, ContextPart.FrameType.BAR);
      navigationLayer.addBinding(footerIconBinding);
      context1.bindLight(navigationLayer, () -> this.isActive() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
      RgbButton context3 = mainScreenSection.getContextButton(2);
      RgbButton context4 = mainScreenSection.getContextButton(3);
      context3.bindRepeatHold(this, categoryItem::selectPrevious, BUTTON_WAIT_UTIL_REPEAT, HOLD_REPEAT_FREQUENCY);
      context3.bindLight(this,
         () -> categoryItem.hasPrevious().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
      context4.bindRepeatHold(this, categoryItem::selectNext, BUTTON_WAIT_UTIL_REPEAT, HOLD_REPEAT_FREQUENCY);
      context4.bindLight(this, () -> categoryItem.hasNext().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);
   }

   private void toggleBrowserAction() {
      this.toggleIsActive();
      if (this.isActive()) {
         openBrowser();
      } else {
         exitBrowser();
      }
   }

   private void handleBrowserOpened(boolean exists) {
//      if (browsingInitiated.get()) {
//         browser.shouldAudition().set(false);
//      }
      // driver.browserDisplayMode(exists);
      if (!exists) {
         browsingInitiated.set(false);
         this.setIsActive(false);
      }
   }

   private void mainEncoderAction(int increment) {
      //driver.getOled().enableValues(DisplayMode.BROWSER);
      //browser.shouldAudition().set(false);
      if (increment > 0) {
         resultCursorItem.selectNext();
      } else {
         resultCursorItem.selectPrevious();
      }
   }

   private void openBrowser() {
      encoderClickCount = 0;
      if (cursorDevice.exists().get()) {
         browsingInitiated.set(true);
         enforceDeviceContent = true;
         cursorDevice.replaceDeviceInsertionPoint().browse();
      } else {
         browsingInitiated.set(true);
         cursorTrack.endOfDeviceChainInsertionPoint().browse();
      }
   }

   private void exitBrowser() {
      encoderClickCount = 0;
      if (browser.exists().get()) {
         browser.cancel();
      }
      browsingInitiated.set(false);
      this.setIsActive(true);
   }

}
