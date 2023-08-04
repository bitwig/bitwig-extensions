package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLed;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.ScaleLayer;
import com.bitwig.extensions.framework.values.Scale;

import java.util.List;

public class KeyboardMode extends BasicKeyPlayingMode {

   private static final List<Scale> scales = List.of(Scale.CHROMATIC, Scale.MAJOR, Scale.MINOR, Scale.PENTATONIC,
      Scale.PENTATONIC_MINOR, Scale.DORIAN, Scale.PHRYGIAN, Scale.LYDIAN, Scale.MIXOLYDIAN, Scale.LOCRIAN,
      Scale.DIMINISHED, Scale.MAJOR_BLUES, Scale.MINOR_BLUES, Scale.WHOLE_TONE, Scale.ARABIAN, Scale.EGYPTIAN,
      Scale.GYPSI, Scale.SPANISH);

   private static final RgbLed BASENOTE_COLOR = RgbLed.of(73);
   private static final RgbLed BASENOTE_COLOR_ON = RgbLed.of(75);

   private Scale currentScale = scales.get(0);
   private final boolean[] isBaseNote = new boolean[16];

   private int baseNote = 0;
   private int octaveOffset = 0;

   private int currentScaleIndex = 0;
   private int trackColor = 0;
   
   public KeyboardMode(final MaschineExtension driver, //
                       final String name, //
                       final NoteFocusHandler noteFocusHandler, //
                       final VeloctiyHandler velocityHandler, //
                       final ScaleLayer associatedDisplay) {
      super(driver, name, noteFocusHandler, velocityHandler, associatedDisplay);

      associatedDisplay.setKeyboardLayer(this);
      final NoteInput noteInput = driver.getNoteInput();
      noteInput.setShouldConsumeEvents(false);
      noteInput.setKeyTranslationTable(noteTable);
      velocityHandler.assingTranslationTable(noteInput);
      final PadButton[] buttons = driver.getPadButtons();
      final CursorTrack cursorTrack = driver.getCursorTrack();
      cursorTrack.color().addValueObserver((r, g, b) -> trackColor = NIColorUtil.convertColorX(r, g, b));
      for (int i = 0; i < buttons.length; i++) {
         final int index = i;
         final PadButton button = buttons[i];
         bindLightState(() -> computeGridLedState(index), button);
         bindShift(button);
         selectLayer.bindPressed(button, () -> selectPad(index));
      }
   }

   public Scale getCurrentScale() {
      return currentScale;
   }

   public int getNextNote(final int noteVal, final int amount) {
      return currentScale.getNextNote(noteVal, baseNote, amount);
   }

   private void selectPad(final int index) {
      noteFocusHandler.notifyNoteSelected(noteTable[index + PadButton.PAD_NOTE_OFFSET]);
   }

   public void setAltMode(final PadMode altMode) {
      final MaschineExtension driver = getDriver();
      driver.getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
         if (isActive() && hasDrumPads) {
            driver.setMode(altMode);
            if (altMode.getAssociatedDisplay() != null //
               && driver.getCurrentDisplayMode().isPadRelatedMode()) {
               driver.setDisplayMode(altMode.getAssociatedDisplay());
            }
         }
      });
   }

   public void incScale(final int dir) {
      final int nxtIndex = currentScaleIndex + dir;
      if (nxtIndex >= 0 && nxtIndex < scales.size()) {
         currentScale = scales.get(nxtIndex);
         currentScaleIndex = nxtIndex;
         while (currentScale.highestNote(60 + octaveOffset * 12 + baseNote, 16) > 127) {
            octaveOffset--;
         }
         applyScale();
      }
   }

   public void incOctave(final int incval) {
      final int newOctave = octaveOffset + incval;
      final int startNote = 60 + newOctave * 12 + baseNote;
      if (startNote > 0 && currentScale.highestNote(startNote, 16) < 128) {
         octaveOffset = newOctave;
         applyScale();
      }
   }

   public void incSemi(final int incval) {
      int newSemi = baseNote + incval;
      int newOctave = octaveOffset;
      if (newSemi < 0) {
         newOctave--;
         newSemi = 12 + incval;
      }
      if (newSemi > 11) {
         newOctave++;
         newSemi -= 12;
      }

      final int startNote = 60 + newOctave * 12 + newSemi;
      if (startNote > 0 && currentScale.highestNote(startNote, 16) < 128) {
         baseNote = newSemi;
         octaveOffset = newOctave;
         applyScale();
      }
   }

   private InternalHardwareLightState computeGridLedState(final int index) {
      if (isBaseNote[index]) {
         return playing[index] ? BASENOTE_COLOR_ON : BASENOTE_COLOR;
      }
      return RgbLed.of(trackColor + (playing[index] ? 2 : 0)); // Fast color look up with value of
   }

   @Override
   void applyScale() {
      for (int i = 0; i < 128; i++) {
         noteToPad[i] = -1;
      }

      final int startNote = 60 + octaveOffset * 12 + baseNote;
      final int[] intervals = currentScale.getIntervals();
      for (int i = 0; i < 16; i++) {
         final int index = i % intervals.length;
         final int oct = i / intervals.length;
         final int note = Math.min(127, Math.max(0, startNote + intervals[index] + 12 * oct));
         noteTable[i + PadButton.PAD_NOTE_OFFSET] = note;
         noteToPad[note] = i;
         isBaseNote[i] = index == 0;
      }
      getDriver().getNoteInput().setKeyTranslationTable(noteTable);
   }

   @Override
   protected String getModeDescription() {
      return "Keyboard Mode";
   }

   public int getBaseNote() {
      return 60 + octaveOffset * 12 + baseNote;
   }

}
