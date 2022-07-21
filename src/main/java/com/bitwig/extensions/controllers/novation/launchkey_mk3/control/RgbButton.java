package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RgbButton {
   protected final int index;
   protected final int number;
   protected final MidiOut midiOut;
   protected final HardwareButton hwButton;
   protected final MultiStateHardwareLight hwLight;
   protected final ControllerHost host;
   protected int channel;

   public RgbButton(final LaunchkeyMk3Extension driver, final String name, final int index, final int noteCcNr,
                    final int channel) {
      number = noteCcNr;
      this.channel = channel;
      final HardwareSurface surface = driver.getSurface();
      this.index = index;
      host = driver.getHost();
      midiOut = driver.getMidiOut();
      final String indexedName = name + "_" + (index + 1);
      hwButton = surface.createHardwareButton(indexedName);
      hwLight = surface.createMultiStateHardwareLight("PAD_LIGHT_" + indexedName);
      hwButton.setBackgroundLight(hwLight);
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target, final Supplier<RgbState> lightSource) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      layer.bindLightState(lightSource::get, hwLight);
   }

   public void bindPressed(final Layer layer, final Runnable action, final Supplier<RgbState> lightSource) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(lightSource::get, hwLight);
   }

}
