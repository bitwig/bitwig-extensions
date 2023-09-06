package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.TouchStrip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.HashMap;
import java.util.Map;

@Component
public class TouchStripLayer extends Layer {

   private final Layer stripModLayer;
   private final Layer pitchBendLayer;
   private final Layer notePlayLayer;
   private final MidiProcessor midiProcessor;
   private final Layer performLayer;
   private final Layer fixedLayer;
   private StripMode stripMode = StripMode.NONE;
   private int modValue = 0;
   private int pitchBend = 0;
   private int noteStripValue = 0;
   private int lastNotePlayed = -1;
   private boolean noteHeld = false;
   private RemoteControl focusedParameter;
   private Map<StripMode, Layer> stepEditLayerMap = new HashMap<>();

   @Inject
   private PadLayer padLayer;

   public TouchStripLayer(Layers layers, HwElements hwElements, MidiProcessor midiProcessor) {
      super(layers, "TOUCH_STRIP_LAYER");

      this.midiProcessor = midiProcessor;
      stripModLayer = new Layer(layers, "STRIP_MOD_LAYER");
      pitchBendLayer = new Layer(layers, "PITCH_BEND_LAYER");
      notePlayLayer = new Layer(layers, "NOTE_PLAY_LAYER");
      performLayer = new Layer(layers, "PERFORM_LAYER");

      initStepEditLayers(layers);

      fixedLayer = new Layer(layers, "FIXED_LAYER");

      hwElements.getButton(CcAssignment.MOD).bindPressed(this, () -> selectMode(StripMode.MOD));
      hwElements.getButton(CcAssignment.MOD).bindLight(this, () -> stripMode == StripMode.MOD);

      hwElements.getButton(CcAssignment.PITCH).bindPressed(this, () -> selectMode(StripMode.PITCH));
      hwElements.getButton(CcAssignment.PITCH).bindLight(this, () -> stripMode == StripMode.PITCH);

      hwElements.getButton(CcAssignment.NOTES).bindPressed(this, () -> selectMode(StripMode.NOTE));
      hwElements.getButton(CcAssignment.NOTES).bindLight(this, () -> stripMode == StripMode.NOTE);

      hwElements.getButton(CcAssignment.PERFORM).bindPressed(this, () -> selectMode(StripMode.PARAMETER));
      hwElements.getButton(CcAssignment.PERFORM).bindLight(this, () -> stripMode == StripMode.PARAMETER);

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

      touchStrip.bindValue(performLayer, value -> {
         if (focusedParameter != null) {
            focusedParameter.value().set(value, 128);
         }
      });
      touchStrip.bindStripLight(performLayer, () -> {
         if (focusedParameter != null) {
            return (int) (focusedParameter.getAsDouble() * 127);
         }
         return 0;
      });

//      hwElements.getButton(CcAssignment.FIXED_VEL).bindPressed(this, () -> handleFixedVelocityPressed());
//      hwElements.getButton(CcAssignment.FIXED_VEL).bindRelease(this, () -> handleFixedVelocityReleased());
      hwElements.getButton(CcAssignment.FIXED_VEL).bindLight(this, () -> isFixedVelocityActive());

      hwElements.getButton(CcAssignment.FIXED_VEL)
         .bindHoldDelay(this, this::handleFixedInitial, this::handleHeldFixed, this::handleFixedReleased, 200);

      touchStrip.bindStripLight(fixedLayer, () -> padLayer.getFixedVelocity());
      touchStrip.bindValue(fixedLayer, value -> padLayer.updateFixedVelocity(value));
      touchStrip.bindTouched(fixedLayer, touched -> {
      });
   }

   private void initStepEditLayers(Layers layers) {
      Layer positionLayer = new Layer(layers, "STEP_STRIP_POSITION");
      Layer randomLayer = new Layer(layers, "STEP_STRIP_RANDOM");
      Layer repeatLayer = new Layer(layers, "STEP_STRIP_REPEAT");
      Layer lengthLayer = new Layer(layers, "STEP_STRIP_LENGTH");
      Layer timbreLayer = new Layer(layers, "STEP_STRIP_TIMBRE");
      Layer pressureLayer = new Layer(layers, "STEP_STRIP_PRESSURE");
      stepEditLayerMap.put(StripMode.MOD, randomLayer);
      stepEditLayerMap.put(StripMode.PARAMETER, repeatLayer);
      stepEditLayerMap.put(StripMode.NONE, positionLayer);
      stepEditLayerMap.put(StripMode.NOTE, lengthLayer);
      stepEditLayerMap.put(StripMode.PITCH, timbreLayer);
      stepEditLayerMap.put(StripMode.PRESSURE, pressureLayer);
   }

   public Layer getStepLayer(StripMode mode) {
      return stepEditLayerMap.get(mode);
   }

   private void handleFixedInitial() {
      //
   }

   private void handleHeldFixed() {
      fixedLayer.setIsActive(true);
   }

   private void handleFixedReleased(int timePassed) {
      if (timePassed < 200) {
         padLayer.toggleFixedMode();
      }
      fixedLayer.setIsActive(false);
   }

   private boolean isFixedVelocityActive() {
      return padLayer.isFixedActive();
   }

   private void playNote(Boolean pressed) {
      noteHeld = pressed;
      if (!pressed && lastNotePlayed != -1) {
         midiProcessor.releaseNote(0, lastNotePlayed);
         lastNotePlayed = -1;
      }
   }

   public void setCurrentParameter(RemoteControl parameter) {
      this.focusedParameter = parameter;
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
      performLayer.setIsActive(stripMode == StripMode.PARAMETER);
   }


}
