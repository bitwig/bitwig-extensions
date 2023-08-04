package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class TouchStrip {
   private final AbsoluteHardwareKnob absoluteHwControl;
   private final MultiStateHardwareLight stripLight;
   private final MidiProcessor midiProcessor;
   private GateButton touchElement;

   public TouchStrip(HardwareSurface surface, MidiProcessor midiProcessor, GateButton touchElement) {
      this.touchElement = touchElement;
      this.midiProcessor = midiProcessor;
      absoluteHwControl = surface.createAbsoluteHardwareKnob("TOUCH_STRIP");
      absoluteHwControl.setAdjustValueMatcher(midiProcessor.getMidiIn().createAbsoluteCCValueMatcher(0, 1));
      this.stripLight = surface.createMultiStateHardwareLight("STRIP_LIGHT");
      stripLight.state().onUpdateHardware(this::updateState);
   }

   public void bindStripLight(Layer layer, IntSupplier valueSuppliers) {
      layer.bindLightState(() -> TouchStripLightState.of(valueSuppliers.getAsInt()), stripLight);
   }

   public void bindValue(Layer layer, IntConsumer value) {
      layer.bind(absoluteHwControl, dv -> {
         int v = (int) Math.round(dv * 127.0);
         value.accept(v);
      });
   }

   private void updateState(InternalHardwareLightState state) {
      if (state instanceof TouchStripLightState lightState) {
         midiProcessor.sendMidiCC(01, lightState.getValue());
      }
   }

   public void bindTouched(Layer layer, Consumer<Boolean> touch) {
      touchElement.bindPressed(layer, () -> touch.accept(true));
      touchElement.bindRelease(layer, () -> touch.accept(false));
   }
}
