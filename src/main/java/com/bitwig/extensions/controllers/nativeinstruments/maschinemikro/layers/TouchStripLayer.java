package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.FocusClip;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.TouchStrip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class TouchStripLayer extends Layer {

   private final Layer stripModLayer;
   private final Layer pitchBendLayer;
   private final Layer notePlayLayer;
   private final MidiProcessor midiProcessor;
   private StripMode stripMode = StripMode.NONE;
   private int modValue = 0;
   private int pitchBend = 0;
   private int noteStripValue = 0;
   private int lastNotePlayed = -1;
   private boolean noteHeld = false;

   @Inject
   private PadLayer padLayer;

   public TouchStripLayer(Layers layers, HwElements hwElements, MidiProcessor midiProcessor, FocusClip focusClip) {
      super(layers, "TOUCH_STRIP_LAYER");

      this.midiProcessor = midiProcessor;
      stripModLayer = new Layer(layers, "STRIP_MOD_LAYER");
      pitchBendLayer = new Layer(layers, "PITCH_BEND_LAYER");
      notePlayLayer = new Layer(layers, "NOTEP_PLAY_LAYER");

      hwElements.getButton(CcAssignment.MOD).bindPressed(this, () -> selectMode(StripMode.MOD));
      hwElements.getButton(CcAssignment.MOD).bindLight(this, () -> stripMode == StripMode.MOD);

      hwElements.getButton(CcAssignment.PITCH).bindPressed(this, () -> selectMode(StripMode.PITCH));
      hwElements.getButton(CcAssignment.PITCH).bindLight(this, () -> stripMode == StripMode.PITCH);

      hwElements.getButton(CcAssignment.NOTES).bindPressed(this, () -> selectMode(StripMode.NOTE));
      hwElements.getButton(CcAssignment.NOTES).bindLight(this, () -> stripMode == StripMode.NOTE);

      TouchStrip touchStrip = hwElements.getTouchStrip();

      touchStrip.bindValue(stripModLayer, value -> {
         midiProcessor.sendCcToNoteInput(1, value);
         modValue = value;
      });
      touchStrip.bindStripLight(stripModLayer, () -> modValue);
      touchStrip.bindTouched(stripModLayer, touched -> {
      });

      touchStrip.bindTouched(pitchBendLayer, touched -> {
         if (!touched) {
            pitchBend = 0;
            midiProcessor.sendPitchBendToNoteInput(0, 0);
         }
      });
      touchStrip.bindStripLight(pitchBendLayer, () -> pitchBend + 64);
      touchStrip.bindValue(pitchBendLayer, value -> {
         pitchBend = value - 64;
         double dv = pitchBend / (pitchBend < 0 ? 64.0 : 63.0);
         midiProcessor.sendPitchBendToNoteInput(0, dv);
      });

      touchStrip.bindTouched(notePlayLayer, this::playNote);
      touchStrip.bindStripLight(notePlayLayer, () -> noteHeld ? Math.max(noteStripValue, 4) : 0);
      touchStrip.bindValue(notePlayLayer, this::selectNote);
   }

   private void playNote(Boolean pressed) {
      noteHeld = pressed;
      if (!pressed && lastNotePlayed != -1) {
         midiProcessor.releaseNote(0, lastNotePlayed);
         lastNotePlayed = -1;
      }
   }

   private void selectNote(int stripValue) {
      noteStripValue = stripValue;
      int note = padLayer.filterToView(noteStripValue);
      if (noteHeld && note != lastNotePlayed) {
         if (lastNotePlayed != -1) {
            midiProcessor.releaseNote(0, lastNotePlayed);
         }
         int velocity = padLayer.getFixedVelocity();
         if (note != -1) {
            midiProcessor.playNote(0, note, velocity);
            lastNotePlayed = note;
         }
      }
   }

   @Activate
   public void handleActivate() {
      setIsActive(true);
   }

   private void selectMode(StripMode mode) {
      stripMode = stripMode == mode ? StripMode.NONE : mode;
      updateLayers();
   }


   private void updateLayers() {
      stripModLayer.setIsActive(stripMode == StripMode.MOD);
      pitchBendLayer.setIsActive(stripMode == StripMode.PITCH);
      notePlayLayer.setIsActive(stripMode == StripMode.NOTE);
   }
}
