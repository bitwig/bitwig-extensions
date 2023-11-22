package com.bitwig.extensions.framework.values;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.SettableEnumValue;

import java.util.ArrayList;
import java.util.List;

public class PadScaleHandler {

   private final List<Scale> scales;
   private final List<String> baseNotes = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
   private final int padCount;
   private final SettableEnumValue baseNotesAssignment;

   private int currentScale = 0;
   private int baseNote = 0;
   private int noteOffset = 48;

   private final SettableEnumValue scaleAssignment;
   private final List<Runnable> stateChangedListener = new ArrayList<>();

   public PadScaleHandler(ControllerHost host, List<Scale> includedScales, int padCount, boolean scaleToDocumentState) {
      this.scales = includedScales;
      this.padCount = padCount;
      DocumentState documentState = host.getDocumentState();

      if (scaleToDocumentState) {
         scaleAssignment = documentState.getEnumSetting("Pad Scale", //
            "Pads", scales.stream().map(Scale::getName).toArray(String[]::new), scales.get(0).getName());
         scaleAssignment.addValueObserver(this::handleScaleChanged);

         baseNotesAssignment = documentState.getEnumSetting("Base Note", //
            "Pads", baseNotes.stream().toArray(String[]::new), baseNotes.get(0));
         baseNotesAssignment.addValueObserver(this::handleBaseNoteChanged);
      } else {
         scaleAssignment = null;
         baseNotesAssignment = null;
      }
   }

   public void addStateChangedListener(Runnable listener) {
      this.stateChangedListener.add(listener);
   }

   public int getBaseNote() {
      return baseNote;
   }

   public Scale getCurrentScale() {
      return scales.get(currentScale);
   }

   private void handleBaseNoteChanged(String newNote) {
      int index = baseNotes.indexOf(newNote);
      if (index != -1) {
         baseNote = index;
         stateChangedListener.forEach(Runnable::run);
      }
   }

   private void handleScaleChanged(String newScale) {
      scales.stream()
         .filter(scale -> scale.getName().equals(newScale))
         .map(scale -> scales.indexOf(scale))
         .findFirst()
         .ifPresent(newIndex -> {
            currentScale = newIndex;
            stateChangedListener.forEach(Runnable::run);
         });
   }

   public int matchScale(int inNote, int dir) {
      Scale scale = scales.get(currentScale);
      int scaleIndex = (inNote % 12 + 12 - baseNote) % 12;
      if (scale.inScale(scaleIndex)) {
         return inNote;
      }
      int nextInScale = scale.nextInScale(scaleIndex);
      int diff = nextInScale - scaleIndex;
      return inNote + diff * dir;
   }

   public boolean inScale(int inNote) {
      Scale scale = scales.get(currentScale);
      int scaleIndex = (inNote % 12 + 12 - baseNote) % 12;
      return scale.inScale(scaleIndex);
   }

   public void incScaleSelection(int dir) {
      currentScale += dir;
      if (currentScale >= scales.size()) {
         currentScale = 0;
      } else if (currentScale < 0) {
         currentScale = scales.size() - 1;
      }
      if (scaleAssignment != null) {
         scaleAssignment.set(scales.get(currentScale).getName());
      }
      stateChangedListener.forEach(Runnable::run);
   }

   public void incrementNoteOffset(int dir) {
      Scale activeScale = scales.get(currentScale);
      int[] intervals = activeScale.getIntervals();
      int scaleSize = intervals.length;
      int amount = scaleSize == 12 ? 4 : 12;
      int lastNoteOffset = padCount / scaleSize * 12 - scaleSize;
      int newValue = noteOffset + amount * dir;
      if (newValue >= 0 && (newValue + lastNoteOffset) < 128) {
         noteOffset = newValue;
         stateChangedListener.forEach(Runnable::run);
      }
   }

   public int getStartNote() {
      if (getCurrentScale().getIntervals().length == 12) {
         return noteOffset + baseNote;
      }
      int octave = noteOffset / 12;
      return octave * 12 + baseNote;
   }

   public void incBaseNote(int dir) {
      int newBaseNote = baseNote + dir;
      if (newBaseNote >= 0 && newBaseNote < 12) {
         baseNote = newBaseNote;
         if (baseNotesAssignment != null) {
            baseNotesAssignment.set(baseNotes.get(baseNote));
         }
      }
   }

   public boolean isBaseNote(int note) {
      return (note + 12 - baseNote) % 12 == 0;
   }

   public boolean inScale(Integer note) {
      int noteBase = (note - baseNote + 120) % 12;
      return getCurrentScale().inScale(noteBase);
   }
}
