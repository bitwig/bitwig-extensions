package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteLatch;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;
import com.bitwig.extension.controller.api.SettableIntegerValue;

final class KeyboardMode extends Mode
{
   public KeyboardMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "keyboard");

      final CursorTrack cursorTrack = driver.mCursorTrack;
      final PlayingNoteArrayValue playingNotes = cursorTrack.playingNotes();
      mKeyboardLayer = new KeyboardLayer(driver, "keyboard", 0, 0, 8, 8, () -> new Color(cursorTrack.color()),
         playingNotes::isNotePlaying, null);

      bindPressed(driver.mRightButton, cursorTrack.selectNextAction());
      bindPressed(driver.mLeftButton, cursorTrack.selectPreviousAction());
      bindPressed(driver.mUpButton, () -> {
         mKeyboardLayer.octaveUp();
         mDriver.updateKeyTranslationTable();
      });
      bindPressed(driver.mDownButton, () -> {
         mKeyboardLayer.octaveDown();
         mDriver.updateKeyTranslationTable();
      });

      bindLightState(LedState.PLAY_MODE, driver.mNoteButton);
      bindLightState(() -> cursorTrack.hasNext().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.mRightButton);
      bindLightState(() -> cursorTrack.hasPrevious().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.mLeftButton);
      bindLightState(() -> mKeyboardLayer.canOctaveDown() ? LedState.PITCH : LedState.PITCH_LOW, driver.mDownButton);
      bindLightState(() -> mKeyboardLayer.canOctaveUp() ? LedState.PITCH : LedState.PITCH_LOW, driver.mUpButton);

      final NoteInput noteInput = driver.mNoteInput;
      final NoteLatch noteLatch = noteInput.noteLatch();
      final Arpeggiator arpeggiator = noteInput.arpeggiator();
      final SettableIntegerValue octaves = arpeggiator.octaves();

      mConfigLayer = new NoteLatchAndArpeggiatorConfigLayer(driver, "keyboard-config");

      bindPressed(driver.mShiftButton, mConfigLayer.getToggleAction());
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

      final CursorTrack cursorTrack = mDriver.mCursorTrack;
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
      mDriver.mNoteInput.setKeyTranslationTable(LaunchpadProControllerExtension.FILTER_ALL_NOTE_MAP);

      final CursorTrack cursorTrack = mDriver.mCursorTrack;
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
