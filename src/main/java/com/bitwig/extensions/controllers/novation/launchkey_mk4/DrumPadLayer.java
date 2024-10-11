package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DrumPadLayer extends Layer {
   private final Integer[] noteTable = new Integer[128];

   public DrumPadLayer(Layers layers, LaunchkeyHwElements hwElements, MidiProcessor midiProcessor) {
      super(layers, "DRUM_PAD");
   }
}
