package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteLatch;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;
import com.bitwig.extension.controller.api.SettableIntegerValue;

public final class KeyboardMode extends Mode
{
   public KeyboardMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "keyboard");

      final CursorTrack cursorTrack = driver.getCursorTrack();
      final PlayingNoteArrayValue playingNotes = cursorTrack.playingNotes();
      mKeyboardLayer = new KeyboardLayer(driver, "keyboard", 0, 0, 8, 8, () -> new Color(cursorTrack.color()),
         playingNotes::isNotePlaying, null);

      bindPressed(driver.getRightButton(), cursorTrack.selectNextAction());
      bindPressed(driver.getLeftButton(), cursorTrack.selectPreviousAction());
      bindPressed(driver.getUpButton(), () -> {
         mKeyboardLayer.octaveUp();
         mDriver.updateKeyTranslationTable();
      });
      bindPressed(driver.getDownButton(), () -> {
         mKeyboardLayer.octaveDown();
         mDriver.updateKeyTranslationTable();
      });

      bindLightState(LedState.PLAY_MODE, driver.getNoteButton());
      bindLightState(() -> cursorTrack.hasNext().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getRightButton());
      bindLightState(() -> cursorTrack.hasPrevious().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getLeftButton());
      bindLightState(() -> mKeyboardLayer.canOctaveDown() ? LedState.PITCH : LedState.PITCH_LOW, driver.getDownButton());
      bindLightState(() -> mKeyboardLayer.canOctaveUp() ? LedState.PITCH : LedState.PITCH_LOW, driver.getUpButton());

      final NoteInput noteInput = driver.getNoteInput();
      final NoteLatch noteLatch = noteInput.noteLatch();
      final Arpeggiator arpeggiator = noteInput.arpeggiator();
      final SettableIntegerValue octaves = arpeggiator.octaves();

      mConfigLayer = new NoteLatchAndArpeggiatorConfigLayer(driver, "keyboard-config");

      bindPressed(driver.getShiftButton(), mConfigLayer.getToggleAction());
   }

   @Override
   protected String getModeDescription()
   {
      switch (mDriver.getKeyboardLayout())
      {
         case GUITAR:
            return "Play: Guitar Layout";
         case LINE_3:
            return "Play: Line/3 Layout";
         case LINE_7:
            return "Play: Line/7 Layout";
         case PIANO:
            return "Play: Piano Layout";
      }
      return "Play ???";
   }

   @Override
   public void doActivate()
   {
      mKeyboardLayer.activate();

      final CursorTrack cursorTrack = mDriver.getCursorTrack();
      cursorTrack.subscribe();
      cursorTrack.playingNotes().subscribe();
      cursorTrack.color().subscribe();
      cursorTrack.hasNext().subscribe();
      cursorTrack.hasPrevious().subscribe();
   }

   @Override
   protected void doDeactivate()
   {
      mConfigLayer.deactivate();
      mKeyboardLayer.deactivate();
      mDriver.getNoteInput().setKeyTranslationTable(LaunchpadProControllerExtension.FILTER_ALL_NOTE_MAP);

      final CursorTrack cursorTrack = mDriver.getCursorTrack();
      cursorTrack.playingNotes().unsubscribe();
      cursorTrack.color().unsubscribe();
      cursorTrack.hasPrevious().unsubscribe();
      cursorTrack.hasNext().unsubscribe();
      cursorTrack.unsubscribe();
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      mKeyboardLayer.updateKeyTranslationTable(table);
      if (mConfigLayer.isActive())
         mConfigLayer.updateKeyTranslationTable(table);
   }

   public void invalidate()
   {
      if (!isActive())
         return;

      mDriver.updateKeyTranslationTable();
   }

   private final KeyboardLayer mKeyboardLayer;
   private final NoteLatchAndArpeggiatorConfigLayer mConfigLayer;
}
