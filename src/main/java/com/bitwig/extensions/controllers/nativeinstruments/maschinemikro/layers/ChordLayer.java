package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.List;

@Component
public class ChordLayer extends Layer {

   private final MidiProcessor midiProcessor;

   static class Chord {
      private final int[] notes;

      public Chord(int... notes) {
         this.notes = notes;
      }
   }

   private List<Chord> chords = List.of(new Chord(0, 3, 5), new Chord(0, 3, 5, 7), new Chord(0, 4, 5, 7),
      new Chord(0, 5, 7), new Chord(0, 2, 5, 11), new Chord(0, 3, 5, 12, 15), new Chord(0, 4, 5, 12, 15),
      new Chord(05, 12, 15));

   @Inject
   PadLayer padLayer;

   private boolean[] playing = new boolean[8];

   public ChordLayer(Layers layers, HwElements hwElements, MidiProcessor midiProcessor, ControllerHost host) {
      super(layers, "CHORD_LAYER");

      this.midiProcessor = midiProcessor;

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < 8; i++) {
         final int index = i;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this, () -> RgbColor.OFF);
         button.bindPressed(this, () -> {
         });
         button.bindRelease(this, () -> {
         });
      }
      for (int i = 8; i < 16; i++) {
         final int index = i - 8;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this,
            () -> playing[index] ? RgbColor.WHITE : RgbColor.GREEN.brightness(ColorBrightness.DIMMED));
         button.bindPressed(this, velocity -> handlePlayed(index, velocity));
         button.bindRelease(this, () -> handleReleased(index));
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindPressed(this, () -> handleEncoderPress(true));
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindRelease(this, () -> handleEncoderPress(false));
   }

   private void handleEncoder(int dir) {

   }

   private void handleEncoderPress(boolean pressed) {
   }

   void handlePlayed(int index, double velocity) {
      playChord(chords.get(index), velocity);
      playing[index] = true;
   }

   void handleReleased(int index) {
      releaseChord(chords.get(index));
      playing[index] = false;
   }

   private void playChord(Chord chord, double velocity) {
      int intVelocity = (int) (velocity * 127);
      for (int note : chord.notes) {
         midiProcessor.sendRawNoteOn(note + 48, intVelocity);
      }
   }

   private void releaseChord(Chord chord) {
      for (int note : chord.notes) {
         midiProcessor.sendNoteOff(note + 48);
      }
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      DebugOutMk.println(" ACTIVATE CHORD");
      midiProcessor.getNoteInput().setShouldConsumeEvents(false);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      DebugOutMk.println(" xxxx CHORD");
      midiProcessor.getNoteInput().setShouldConsumeEvents(true);
   }
}
