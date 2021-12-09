package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.MainUnitButton;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderMode;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.CombinedStringValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.controllers.mackie.value.StringIntValueObject;
import com.bitwig.extensions.framework.Layer;

public class BrowserConfiguration extends LayerConfiguration {

   public enum Type {
      DEVICE, PRESET
   }

   private final PopupBrowser browser;
   private LayerConfiguration previousConfig = null;
   private boolean browsingInitiated = false;
   private boolean resetState = false;
   private Type contentType = Type.DEVICE;

   private final CursorBrowserFilterItem categoryItem;
   private final CursorBrowserFilterItem tagItem;
   private final CursorBrowserFilterItem fileTypeItem;
   private final CursorBrowserFilterItem deviceTypeItem;
   // private CursorBrowserFilterItem deviceItem;
   private FilterLayerConfig currentConfig;

   private final FilterLayerConfig shiftConfig;
   private final FilterLayerConfig deviceConfig;
   private final FilterLayerConfig presetConfig;
   private final CursorBrowserResultItem resultCursorItem;
   private final StringIntValueObject resultValue;
   private final CursorBrowserFilterItem creatorItem;
   private final CursorBrowserFilterItem locationItem;

   static class FilterLayerConfig {
      private final EncoderLayer encoderLayer;
      private final DisplayLayer displayLayer;

      public FilterLayerConfig(final String name, final MixControl mixControl) {
         encoderLayer = new EncoderLayer(mixControl, name);
         encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
         final int section = mixControl.getHwControls().getSectionIndex();
         displayLayer = new DisplayLayer("DISP_" + name, section, mixControl.getDriver().getLayers(),
            mixControl.getHwControls());
         displayLayer.setUsesLevelMeteringInLcd(false);
      }

      public EncoderLayer getEncoderLayer() {
         return encoderLayer;
      }

      public DisplayLayer getDisplayLayer() {
         return displayLayer;
      }

      public boolean isActive() {
         return displayLayer.isActive();
      }
   }

   public BrowserConfiguration(final String name, final MixControl mixControl, final ControllerHost host,
                               final PopupBrowser browser) {
      super(name, mixControl);
      final int sectionIndex = mixControl.getHwControls().getSectionIndex();
      final String baseName = name + "_ENCODER_LAYER_" + sectionIndex;
      deviceConfig = new FilterLayerConfig("DEV_" + baseName, mixControl);
      presetConfig = new FilterLayerConfig("PRESET_" + baseName, mixControl);
      shiftConfig = new FilterLayerConfig("SHIFT _" + baseName, mixControl);
      currentConfig = deviceConfig;
      // deviceItem = (CursorBrowserFilterItem)
      // browser.deviceColumn().createCursorItem();
      browser.selectedContentTypeIndex().addValueObserver(contentType -> {
         if (!browsingInitiated || mixControl.getDriver().getModifier().isShiftSet()) {
            return;
         }
         if (contentType == 0) {
            currentConfig = deviceConfig;
         } else if (contentType == 1) {
            currentConfig = presetConfig;
         } else {
            currentConfig = deviceConfig;
         }
         mixControl.applyUpdate();
      });

      this.browser = browser;
      this.browser.exists().addValueObserver(browserNowOpen -> {
         if (!browsingInitiated) {
            return;
         }
         if (browserNowOpen) {
            browser.shouldAudition().set(false);
            browser.selectedContentTypeIndex().set(contentType.ordinal());
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
      creatorItem = (CursorBrowserFilterItem) browser.creatorColumn().createCursorItem();
      resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();
      locationItem = (CursorBrowserFilterItem) browser.locationColumn().createCursorItem();
      resultValue = new StringIntValueObject(resultCursorItem.name(), categoryItem.hitCount(), resultCursorItem,
         "FOUND ITEMS=%d");
      locationItem.hitCount().markInterested();

      setUpDeviceBrowsing(deviceConfig, mixControl, host, browser);
      setUpPresetBrowsing(presetConfig, mixControl, host, browser);
      setUpShiftSection(shiftConfig, mixControl, host, browser);
   }

   private void setUpPresetBrowsing(final FilterLayerConfig config, final MixControl mixControl,
                                    final ControllerHost host, final PopupBrowser browser) {
      bindBrowserItem(0, config, mixControl, deviceTypeItem, "Type");
      bindBrowserItem(1, config, mixControl, locationItem, "DevLoc");
      bindBrowserItem(2, config, mixControl, fileTypeItem, "FileTp");
      bindBrowserItem(3, config, mixControl, tagItem, "Tag");
      bindBrowserItem(4, config, mixControl, creatorItem, "Creatr");
      setUpResultSection(config, mixControl, browser);
   }

   private void setUpDeviceBrowsing(final FilterLayerConfig config, final MixControl mixControl,
                                    final ControllerHost host, final PopupBrowser browser) {
      bindBrowserItem(0, config, mixControl, deviceTypeItem, "Type");
      bindBrowserItem(1, config, mixControl, locationItem, "DevLoc");
      bindBrowserItem(2, config, mixControl, fileTypeItem, "FileTp");
      bindBrowserItem(3, config, mixControl, categoryItem, "Catgry");
      bindBrowserItem(4, config, mixControl, creatorItem, "Creatr");

      setUpResultSection(config, mixControl, browser);
   }

   private void setUpShiftSection(final FilterLayerConfig config, final MixControl mixControl,
                                  final ControllerHost host, final PopupBrowser browser) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final EncoderLayer encoderLayer = config.getEncoderLayer();
      final DisplayLayer displayLayer = config.getDisplayLayer();

      encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(0),
         hwControls.createAction(() -> browser.shouldAudition().toggle())));
      encoderLayer.addBinding(
         hwControls.createRingDisplayBinding(0, browser.shouldAudition(), RingDisplayType.FILL_LR_0));
      displayLayer.bindTitle(0, 0, new BasicStringValue("PRVIEW"));
      displayLayer.bindBool(0, browser.shouldAudition(), "< ON >", "<OFF >");

      displayLayer.bindTitle(0, 1, new BasicStringValue("CONT.T"));
      displayLayer.bindTitle(1, 1, browser.selectedContentTypeName());
      encoderLayer.addBinding(
         new ButtonBinding(hwControls.getEncoderPress(1), hwControls.createAction(() -> advanceContent(1))));
      final RelativeHardwareKnob encoder = hwControls.getEncoder(1);
      final RelativeHardwarControlBindable binding = mixControl.getDriver().createIncrementBinder(this::advanceContent);
      encoderLayer.bind(encoder, binding);

      for (int i = 1; i < 8; i++) {
         encoderLayer.addBinding(hwControls.createRingDisplayBinding(i, 0, RingDisplayType.FILL_LR_0));
      }
   }

   private void advanceContent(final int amount) {
      int nextIndex = browser.selectedContentTypeIndex().get() + amount;
      nextIndex = nextIndex < 0 ? 1 : nextIndex > 1 ? 0 : nextIndex;
      browser.selectedContentTypeIndex().set(nextIndex);
   }

   private void setUpResultSection(final FilterLayerConfig config, final MixControl control,
                                   final PopupBrowser browser) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final MainUnitButton enterButton = mixControl.getDriver().getEnterButton();
      final MainUnitButton cancelButton = mixControl.getDriver().getCancelButton();
      final EncoderLayer encoderLayer = config.getEncoderLayer();
      final DisplayLayer displayLayer = config.getDisplayLayer();

      encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(0),
         hwControls.createAction(() -> browser.shouldAudition().toggle())));
      encoderLayer.addBinding(
         new ButtonBinding(hwControls.getEncoderPress(1), hwControls.createAction(() -> advanceContent(1))));
      displayLayer.bindTitle(0, 5, new CombinedStringValueObject("<Cncl>"));
      final HardwareActionBindable cancelAction = hwControls.createAction(browser::cancel);
      final HardwareActionBindable commitAction = hwControls.createAction(browser::commit);

      encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(5), cancelAction));
      cancelButton.bindPressed(encoderLayer, cancelAction);

      displayLayer.bindTitle(0, 7, new CombinedStringValueObject("<Okay>"), resultCursorItem, "<---->");
      displayLayer.bindTitle(1, 5, 3, resultValue, "<>");

      encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(7), commitAction));
      enterButton.bindPressed(encoderLayer, commitAction);
      final RelativeHardwarControlBindable resultSelectionBinding = control.getDriver().createIncrementBinder(v -> {
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

   private void bindBrowserItem(final int index, final FilterLayerConfig config, final MixControl mixControl,
                                final CursorBrowserFilterItem browserCursorItem, final String name) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final EncoderLayer encoderLayer = config.getEncoderLayer();
      final DisplayLayer displayLayer = config.getDisplayLayer();

      displayLayer.bindTitle(0, index, new CombinedStringValueObject(name), browserCursorItem, "");
      displayLayer.bindNameTemp(1, index, 3, browserCursorItem.name(), browserCursorItem, "");
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 11, RingDisplayType.FILL_LR_0));
      final RelativeHardwareKnob encoder = hwControls.getEncoder(index);

      final RelativeHardwarControlBindable binding = mixControl.getDriver().createIncrementBinder(v -> {
         if (v < 0) {
            browserCursorItem.selectPrevious();
         } else {
            browserCursorItem.selectNext();
         }
         displayLayer.tickExpansion(index);
      });
      encoderLayer.bind(encoder, binding);
   }

   public void setBrowsingInitiated(final boolean browsingInitiated, final Type type) {
      this.browsingInitiated = browsingInitiated;
      contentType = type;
      deviceTypeItem.selectFirst();
   }

   public boolean isBrowsingInitiated() {
      return browsingInitiated;
   }

   public PopupBrowser getBrowser() {
      return browser;
   }

   @Override
   public boolean applyModifier(final ModifierValueObject modvalue) {
      if (!isActive()) {
         return false;
      }
      if (modvalue.isShiftSet()) {
         currentConfig = shiftConfig;
      } else {
         final int selectContentTypeIndex = browser.selectedContentTypeIndex().get();
         if (selectContentTypeIndex == 0) {
            currentConfig = deviceConfig;
         } else if (selectContentTypeIndex == 1) {
            currentConfig = presetConfig;
         }
      }
      return !currentConfig.isActive();
   }

   public boolean isBrowserActive() {
      return browser.exists().get();
   }

   public void forceClose() {
      if (browser.exists().get()) {
         resetState = false;
         browser.cancel();
      }
   }

   public void endUserBrowsing() {
      if (browser.exists().get() && browsingInitiated) {
         resetState = false;
         browser.cancel();
      }
   }

   @Override
   public Layer getFaderLayer() {
      return mixControl.getActiveMixGroup().getFaderLayer(ParamElement.VOLUME);
   }

   @Override
   public EncoderLayer getEncoderLayer() {
      return currentConfig.getEncoderLayer();
   }

   @Override
   public DisplayLayer getDisplayLayer(final int which) {
      return currentConfig.getDisplayLayer();
   }

   @Override
   public DisplayLayer getBottomDisplayLayer(final int which) {
      return getMixControl().getActiveMixGroup().getDisplayConfiguration(ParamElement.VOLUME);
   }
}
