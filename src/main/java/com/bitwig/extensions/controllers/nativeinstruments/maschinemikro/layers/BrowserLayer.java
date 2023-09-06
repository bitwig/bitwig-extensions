package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.DebugOutMk;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component(priority = 0)
public class BrowserLayer extends Layer {
   private final PopupBrowser browser;
   private final CursorTrack cursorTrack;
   private final CursorBrowserResultItem resultCursorItem;
   private boolean browsingInitiated = false;
   private boolean isSelecting = false;
   private final PinnableCursorDevice cursorDevice;
   private String[] contentTypeNames = new String[0];
   private String currentContentType = "";

   @Inject
   private ModifierLayer modifierLayer;

   public BrowserLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                       ControllerHost host) {
      super(layers, "ENCODER_LAYER");
      browser = host.createPopupBrowser();
      browser.exists().addValueObserver(exists -> handleBrowserOpened(exists));
      cursorDevice = viewControl.getCursorDevice();
      cursorDevice.exists().markInterested();
      cursorTrack = viewControl.getCursorTrack();
      browser.contentTypeNames().addValueObserver(contentTypeNames -> this.contentTypeNames = contentTypeNames);
      browser.selectedContentTypeIndex().markInterested();
      resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();

      browser.selectedContentTypeIndex().addValueObserver(selected -> {
         if (selected < contentTypeNames.length) {
            currentContentType = contentTypeNames[selected];
            final boolean selectionExists = resultCursorItem.exists().get();
         }
      });
      hwElements.getButton(CcAssignment.FAVORITE).bind(this, () -> handleFavorite());
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindIsPressed(this, this::handleEncoderPressed);
   }

   private void handleEncoderPressed(boolean isPressed) {
      if (!isPressed || !browser.exists().get()) {
         return;
      }
//      if (isSelecting) {
//         browser.shouldAudition().set(true);
//         isSelecting = false;
//      } else {
//         browser.commit();
//      }
      browser.commit();
   }

   private void handleEncoder(int dir) {
      if (modifierLayer.getShiftHeld().get()) {
         final int index = browser.selectedContentTypeIndex().get();
         if (index >= 0 && index < contentTypeNames.length) {
            int next = index + dir;
            next = next < 0 ? contentTypeNames.length - 1 : (next >= contentTypeNames.length) ? 0 : next;
            browser.selectedContentTypeIndex().set(next);
         }
      } else {
         browser.shouldAudition().set(false);
         isSelecting = true;
         if (dir > 0) {
            resultCursorItem.selectNext();
         } else {
            resultCursorItem.selectPrevious();
         }
      }
   }

   private void handleBrowserOpened(boolean exists) {
      setIsActive(exists);
      if (browsingInitiated) {
         browser.shouldAudition().set(false);
         isSelecting = true;
      }
      //driver.browserDisplayMode(exists);
      if (!exists) {
         browsingInitiated = false;
      } else {
         isSelecting = true;
      }
   }

   private boolean isBrowsing() {
      return browser.exists().get();
   }

   private void handleFavorite() {
      DebugOutMk.println(" FAV Pressed ");
   }

   public void handleBrowserActivation() {
      if (browser.exists().get()) {
         browser.cancel();
         this.setIsActive(false);
      } else {
         this.setIsActive(true);
         if (cursorDevice.exists().get()) {
            browsingInitiated = true;
            cursorDevice.replaceDeviceInsertionPoint().browse();
         } else {
            browsingInitiated = true;
            cursorTrack.endOfDeviceChainInsertionPoint().browse();
         }
      }
   }

}
