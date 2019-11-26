package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.PlayingNoteArrayValue;

public final class DrumMode extends Mode
{
   private static final Color DRUM1_COLOR = Color.fromRgb255(255, 170, 0);
   private static final Color DRUM2_COLOR = Color.fromRgb255(0, 170, 127);
   private static final Color DRUM3_COLOR = Color.fromRgb255(255, 0, 255);
   private static final Color DRUM4_COLOR = Color.fromRgb255(255, 0, 0);
   private final static Color PLAYING_DRUM_COLOR = Color.fromRgb255(0, 255, 0);

   public DrumMode(final LaunchpadProControllerExtension launchpadProControllerExtension)
   {
      super(launchpadProControllerExtension, "drum");
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

   @Override
   public void paint()
   {
      super.paint();

      final PlayingNoteArrayValue playingNotes = mDriver.getCursorTrack().playingNotes();

      for (int i = 0; i < 8; ++i)
      {
         for (int j = 0; j < 8; ++j)
         {
            final Button bt = mDriver.getPadButton(i, j);

            final int pitch = calculatePitch(i, j);

            if (playingNotes.isNotePlaying(pitch))
               bt.setColor(PLAYING_DRUM_COLOR);
            else
               switch (i / 4 + 2 * (j / 4))
               {
                  case 0:
                     bt.setColor(DRUM1_COLOR);
                     break;
                  case 1:
                     bt.setColor(DRUM2_COLOR);
                     break;
                  case 2:
                     bt.setColor(DRUM3_COLOR);
                     break;
                  case 3:
                     bt.setColor(DRUM4_COLOR);
                     break;
               }
         }

         mDriver.getRightButton(i).clear();
      }
   }
}
