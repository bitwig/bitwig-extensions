package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;

public class AutoDisplayLayer extends DisplayLayer {
   String automationMode;

   public AutoDisplayLayer(final MaschineExtension driver, final String name) {
      super(driver, name);
      final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
      final ModeButton[] buttons = driver.getDisplayButtons();

      Transport transport = driver.getTranport();

      transport.automationWriteMode().addValueObserver(value -> automationMode = value);
      bindPressed(buttons[0], () -> transport.automationWriteMode().set("latch"));
      bindPressed(buttons[1], () -> transport.automationWriteMode().set("touch"));
      bindPressed(buttons[2], () -> transport.automationWriteMode().set("write"));
      bindLightState(() -> automationMode == "latch", buttons[0]);
      bindLightState(() -> automationMode == "touch", buttons[1]);
      bindLightState(() -> automationMode == "write", buttons[2]);
   }

   @Override
   protected void doActivate() {
      super.doActivate();

      displayInfo();
      //updateValues();
   }

   private void displayInfo() {
      sendToDisplay(2, "Latch | Touch | Write");
      sendToDisplay(3, "");
   }

   @Override
   protected void notifyEncoderTouched(int index, boolean v) {

   }

   @Override
   protected void doNotifyMainTouched(boolean touched) {

   }

   @Override
   protected void doNotifyMacroDown(boolean active) {

   }
}
