package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchpadDeviceConfig;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class LaunchpadMiniMk3ControllerExtension extends AbstractLaunchpadMk3Extension {

   private static final String DEVICE_RESPONSE = "f07e00060200202913010000";
   private long modeButtonDownTime = 0;

   protected LaunchpadMiniMk3ControllerExtension(final ControllerExtensionDefinition definition,
                                                 final ControllerHost host) {
      super(definition, host, new LaunchpadDeviceConfig("LAUNCHPADMINIMK3", 0xD, 0xB5, 0xB4, true));
   }

   @Override
   public void init() {
      final Context diContext = super.initContext();
      // Main Grid Buttons counting from top to bottom
      final Layer mainLayer = diContext.createLayer("MainLayer");

      initModeButtons(diContext, mainLayer);
      currentLayer = sessionLayer;
      mainLayer.activate();

      diContext.activate();
      midiProcessor.sendDeviceInquiry();
   }

   private void initModeButtons(final Context diContext, final Layer layer) {
      final HwElements hwElements = diContext.getService(HwElements.class);
      final LabeledButton sessionButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.SESSION);
      sessionButton.bindPressed(layer, () -> {
         modeSwitch();
         modeButtonDownTime = System.currentTimeMillis();
      });

      final LabeledButton drumsButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.DRUMS);
      drumsButton.bindPressed(layer, () -> mode = LpMode.DRUMS);

      final LabeledButton keysButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.KEYS);
      keysButton.bindPressed(layer, () -> mode = LpMode.KEYS);

      final LabeledButton userButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.USER);
      userButton.bindPressed(layer, () -> mode = LpMode.CUSTOM);
      final MultiStateHardwareLight novationLight = hwElements.getNovationLight();
      layer.bindLightState(() -> RgbState.of(43), novationLight);
   }

   private void modeSwitch() {
      final long clickTime = (System.currentTimeMillis() - modeButtonDownTime);
      if (mode == LpMode.SESSION) {
         if (clickTime < 500) {
            changeMode(LpMode.OVERVIEW);
            midiProcessor.setButtonLed(0x14, 0x29);
            modeButtonDownTime = 0;
         }
      } else {
         changeMode(LpMode.SESSION);
         midiProcessor.setButtonLed(0x14, 0x18);
         modeButtonDownTime = 0;
      }
   }

   private void changeMode(final LpMode mode) {
      if (this.mode == mode) {
         return;
      }
      if (this.mode == LpMode.OVERVIEW) {
         overviewLayer.setIsActive(false);
      }
      if (mode == LpMode.OVERVIEW) {
         overviewLayer.setIsActive(true);
      }
      sessionLayer.setMode(mode);
      this.mode = mode;
   }

   @Override
   protected void handleSysEx(final String sysExString) {
      if (sysExString.startsWith(DEVICE_RESPONSE)) {
         midiProcessor.enableDawMode(true);
         midiProcessor.toLayout(0);
         pause(20);
         hwElements.refresh();
         midiProcessor.setButtonLed(0x14, 0x18);
      }
   }

   private void pause(int time) {
      try {
         Thread.sleep(time);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

}
