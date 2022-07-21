package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;
import com.bitwig.extensions.framework.Layer;

public class DeviceSelectionLayer extends Layer {

   private final ControllerHost host;

   public DeviceSelectionLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "SESSION_LAYER");

      final RgbNoteButton[] buttons = driver.getHwControl().getDeviceButtons();

      for (int i = 0; i < 16; i++) {
         buttons[i].bindPressed(this, () -> {
         }, () -> RgbState.of(34));
      }

      host = driver.getHost();
      final RgbCcButton navUpButton = driver.getHwControl().getNavUpButton();
      navUpButton.bindPressed(this, () -> {

      }, () -> {
         return RgbState.RED_LO;
      });
      final RgbCcButton navDownButton = driver.getHwControl().getNavDownButton();
      navDownButton.bindPressed(this, () -> {
      }, () -> {
         return RgbState.RED_LO;
      });
   }
}
