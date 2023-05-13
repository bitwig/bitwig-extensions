package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;

public class Button {
   private final HardwareButton hwButton;
   private final int ccNr;

   public Button(final LaunchkeyMk3Extension driver, final String name, final int ccNr, final int channel) {
      final HardwareSurface surface = driver.getSurface();
      hwButton = surface.createHardwareButton(name);
      this.ccNr = ccNr;
      final MidiIn midiIn = driver.getMidiIn();
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0));
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
   }

   public void bindPressed(final Layer layer, final Runnable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
   }

   public void bind(final Layer layer, final HardwareActionBindable action) {
      layer.bind(hwButton, hwButton.pressedAction(), action::invoke);
   }

   public void bindToggle(final Layer layer, final SettableBooleanValue value) {
      layer.bind(hwButton, hwButton.pressedAction(), value::toggle);
   }
}
