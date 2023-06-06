package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class VeloctiyHandler {
   protected final Integer[] velTable = new Integer[128];
   protected final BooleanValueObject fixed = new BooleanValueObject(); // Needs to shared with Pad Mode
   private final NoteInput noteInput;

   private int fixedVelocity = 100;

   public VeloctiyHandler(final NoteInput noteInput) {
      this.noteInput = noteInput;
      for (int i = 0; i < 128; i++) {
         velTable[i] = Integer.valueOf(i);
      }
   }

   public int getFixedVelocity() {
      return fixedVelocity;
   }

   public void assingTranslationTable(final NoteInput noteInput) {
      noteInput.setVelocityTranslationTable(velTable);
   }

   public BooleanValueObject getFixed() {
      return fixed;
   }

   public void toggleFixedValue() {
      if (fixed.get()) {
         fixed.toggle();
         for (int i = 0; i < 128; i++) {
            velTable[i] = Integer.valueOf(i);
         }
      } else {
         fixed.toggle();
         for (int i = 0; i < 128; i++) {
            velTable[i] = Integer.valueOf(fixedVelocity);
         }
      }
      noteInput.setVelocityTranslationTable(velTable);
   }

   public void inc(final int incval) {
      int newValue = Math.min(Math.max(1, fixedVelocity + incval), 127);
      if (newValue != fixedVelocity) {
         fixedVelocity = newValue;
         if (fixed.get()) {
            for (int i = 0; i < 128; i++) {
               velTable[i] = Integer.valueOf(fixedVelocity);
            }
            noteInput.setVelocityTranslationTable(velTable);
         }
      }
   }

}
