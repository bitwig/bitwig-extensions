package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;

final class DrumMode extends Mode
{
   private static final LedState DRUM1_LED = new LedState(Color.fromRgb255(255, 170, 0));
   private static final LedState DRUM2_LED = new LedState(Color.fromRgb255(0, 170, 127));
   private static final LedState DRUM3_LED = new LedState(Color.fromRgb255(255, 0, 255));
   private static final LedState DRUM4_LED = new LedState(Color.fromRgb255(255, 0, 0));

   public DrumMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "drum");

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 8; ++y)
         {
            final int X = x;
            final int Y = y;
            final Button button = driver.getPadButton(x, y);
            bindLightState(() -> computeGridLedState(X, Y), button);
         }
      }
   }

   @Override
   protected void doActivate()
   {
      final CursorTrack cursorTrack = mDriver.mCursorTrack;
      cursorTrack.subscribe();
      cursorTrack.playingNotes().subscribe();
   }

   @Override
   protected void doDeactivate()
   {
      final CursorTrack cursorTrack = mDriver.mCursorTrack;
      cursorTrack.unsubscribe();
      cursorTrack.playingNotes().unsubscribe();
   }

   private LedState computeGridLedState(final int x, final int y)
   {
      final PlayingNoteArrayValue playingNotes = mDriver.mCursorTrack.playingNotes();
      final int pitch = calculatePitch(x, y);

      if (playingNotes.isNotePlaying(pitch))
         return LedState.STEP_PLAY;

      switch (x / 4 + 2 * (y / 4))
      {
         case 0 ->
         {
            return DRUM1_LED;
         }
         case 1 ->
         {
            return DRUM2_LED;
         }
         case 2 ->
         {
            return DRUM3_LED;
         }
         case 3 ->
         {
            return DRUM4_LED;
         }
      }

      throw new IllegalStateException();
   }

   @Override
   protected String getModeDescription()
   {
      return "Play: 64 Drum Pads";
   }

   private int calculatePitch(final int x, final int y)
   {
      final int subX = x % 4;
      final int subY = y % 4;
      final int group = (x < 4 ? 0 : 2) + (y < 4 ? 0 : 1);

      return subX + 4 * subY + 16 * group + 36;
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 8; ++y)
         {
            final int tableIndex = 11 + x + y * 10;
            final int note = calculatePitch(x, y);

            assert note >= 0;
            assert note < 128;

            table[tableIndex] = note;
         }
      }
   }
}
