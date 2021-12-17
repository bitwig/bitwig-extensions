package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.NoteStep;

import java.util.Collection;
import java.util.HashMap;

public class NoteStepSlot {
   private final HashMap<Integer, NoteStep> map = new HashMap<>();
   private final int slotNr;

   public NoteStepSlot(final int slotNumber) {
      slotNr = slotNumber;
   }

   public void updateNote(final NoteStep step) {
      if (step.state() == NoteStep.State.NoteOn) {
         map.put(step.y(), step);
      } else {
         map.remove(step.y());
      }
   }

   public int getSlotNr() {
      return slotNr;
   }

   public void clear() {
      map.clear();
   }

   public boolean hasNotes() {
      return !map.isEmpty();
   }

   public Collection<NoteStep> steps() {
      return map.values();
   }

   public NoteStepSlot copy() {
      final NoteStepSlot copy = new NoteStepSlot(slotNr);
      copy.map.putAll(map);
      return copy;
   }
}
