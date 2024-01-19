package com.bitwig.extensions.controllers.akai.apc.common.control;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

public class Encoder {
   private final RelativeHardwareKnob encoder;

   public Encoder(int ccNr, final HardwareSurface surface, MidiIn midiIn) {
      encoder = surface.createRelativeHardwareKnob("ENCODER_" + ccNr);
   
      final String matchExpr = String.format("(status==%d && data1==%d && data2>0)", Midi.CC, ccNr);
      encoder.setAdjustValueMatcher(midiIn.createRelative2sComplementValueMatcher(matchExpr, "data2", 7, 200));
      encoder.setStepSize(0.1);
   }

   public void setStepSize(final double value) {
      encoder.setStepSize(value);
   }

   public void bindParameter(final Layer layer, final Parameter parameter) {
      final RelativeValueBinding binding = new RelativeValueBinding(encoder, parameter);
      layer.addBinding(binding);
   }

   public void bind(final Layer layer, final SettableRangedValue value) {
      final RelativeValueBinding binding = new RelativeValueBinding(encoder, value);
      layer.addBinding(binding);
   }
   
   public void bind(ControllerHost host, final Layer layer, IntConsumer changeAction) {
      final HardwareActionBindable incAction = host.createAction(() -> changeAction.accept(1), () -> "+");
      final HardwareActionBindable decAction = host.createAction(() -> changeAction.accept(-1), () -> "-");
      layer.bind(encoder, host.createRelativeHardwareControlStepTarget(incAction, decAction));
   }
}
