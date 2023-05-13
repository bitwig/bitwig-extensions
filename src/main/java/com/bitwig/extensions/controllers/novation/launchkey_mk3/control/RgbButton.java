package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.framework.Layer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RgbButton {
   protected final int index;
   protected final int number;
   protected final MidiOut midiOut;
   protected final HardwareButton hwButton;
   protected final MultiStateHardwareLight hwLight;
   protected final ControllerHost host;
   protected int channel;

   public RgbButton(final LaunchkeyMk3Extension driver, final String name, final int index, final int channel,
                    final int noteCcNr) {
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

   public int getNumber() {
      return number;
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target, final Supplier<RgbState> lightSource) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      layer.bindLightState(lightSource::get, hwLight);
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target, final RgbState downColor,
                             final RgbState releaseColor) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      hwButton.isPressed().markInterested();
      layer.bindLightState(() -> hwButton.isPressed().get() ? downColor : releaseColor, hwLight);
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target, final BooleanSupplier active,
                             final RgbState downColor, final RgbState releaseColor) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      hwButton.isPressed().markInterested();
      layer.bindLightState(() -> {
         if (active.getAsBoolean()) {
            return hwButton.isPressed().get() ? downColor : releaseColor;
         } else {
            return RgbState.OFF;
         }
      }, hwLight);
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target,
                             final Function<Boolean, RgbState> colorProvider) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
      layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
      hwButton.isPressed().markInterested();
      layer.bindLightState(() -> colorProvider.apply(hwButton.isPressed().get()), hwLight);
   }


   public void bindPressed(final Layer layer, final Runnable action, final Supplier<RgbState> lightSource) {
      layer.bind(hwButton, hwButton.pressedAction(), action);
      layer.bindLightState(lightSource::get, hwLight);
   }

}
