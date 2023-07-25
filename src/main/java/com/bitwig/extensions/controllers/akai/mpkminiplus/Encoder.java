package com.bitwig.extensions.controllers.akai.mpkminiplus;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

public class Encoder {
   private final RelativeHardwareKnob encoder;

   public Encoder(int ccNr, final HardwareSurface surface, MidiIn midiIn) {
      encoder = surface.createRelativeHardwareKnob("ENCODER_" + ccNr);
      final String matchExpr = String.format("(status==%d && data1==%d && data2>0)", Midi.CC, ccNr);
      encoder.setAdjustValueMatcher(midiIn.createRelative2sComplementValueMatcher(matchExpr, "data2", 7, 100));
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
}
