package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;

public class ModeButton<T extends ModeType> {

   private final HardwareButton hwButton;
   private final T mode;

   public ModeButton(final String id, final LaunchkeyMk3Extension driver, final int ccNr, final T mode) {
      final MidiIn midiIn = driver.getMidiIn();
      final HardwareSurface surface = driver.getSurface();
      this.mode = mode;
      hwButton = surface.createHardwareButton(id + "_" + mode);
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, ccNr, mode.getId()));
   }

   public void bind(final Layer layer, final Consumer<T> modeHandler) {
      layer.bind(hwButton, hwButton.pressedAction(), () -> modeHandler.accept(mode));
   }

}
