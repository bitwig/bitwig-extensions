package com.bitwig.extensions.controllers.arturia.keylab.mk3;

import java.util.Arrays;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DrumPadLayer extends Layer {
   protected final int[] noteToPad = new int[128];
   protected final int[] padToNote = new int[16];
   private final Integer[] noteTable = new Integer[128];
   private final NoteInput noteInput;

   public DrumPadLayer(Layers layers, KeylabHardwareElements hwElements, MidiProcessor midiProcessor) {
      super(layers, "DRUM_LAYER");
      Arrays.fill(noteTable, -1);
      Arrays.fill(noteToPad, -1);
      Arrays.fill(padToNote, -1);
      for (int i = 0; i < 48; i++) {
         noteTable[i + 0x24] = i + 0x24;
      }
      this.noteInput = midiProcessor.getDawMainInput();
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      this.noteInput.setKeyTranslationTable(noteTable);
      this.noteInput.setShouldConsumeEvents(false);
      KeylabMk3ControllerExtension.println("Activate");
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
   }


}
