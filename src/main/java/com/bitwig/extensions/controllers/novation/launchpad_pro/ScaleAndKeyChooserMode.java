package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;

public class ScaleAndKeyChooserMode extends Mode
{
   private static final int[] CHROMATIC_NOTE_INDEXES = new int[] { 0, 2, 4, 5, 7, 9, 11, 12, -1, 1, 3, -1, 6, 8, 10,
         -1, };

   private final static LedState ROOT_KEY_COLOR = new LedState(Color.fromRgb255(11, 100, 63));

   private final static LedState USED_KEY_COLOR = new LedState(Color.fromRgb255(255, 240, 240));

   private final static LedState UNUSED_KEY_COLOR = new LedState(Color.fromRgb255(40, 40, 40));

   private final static LedState SCALE_ON_COLOR = new LedState(Color.fromRgb255(50, 167, 202));

   private final static LedState SCALE_OFF_COLOR = new LedState(Color.scale(SCALE_ON_COLOR.getColor(), 0.2f));

   ScaleAndKeyChooserMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "scale-key-chooser");

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 2; ++y)
         {
            final int noteIndex = CHROMATIC_NOTE_INDEXES[x + 8 * y];
            final Button bt = driver.getPadButton(x, 6 + y);
            bindPressed(bt, () -> selectKey(noteIndex));
            bindLightState(() -> {
               if (noteIndex == -1)
                  return LedState.OFF;
               if ((noteIndex % 12) == mDriver.getMusicalKey())
                  return ROOT_KEY_COLOR;

               final MusicalScale scale = mDriver.getMusicalScale();
               if (scale.isMidiNoteInScale(mDriver.getMusicalKey(), noteIndex))
                  return USED_KEY_COLOR;
               return UNUSED_KEY_COLOR;
            }, bt);
         }
      }

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 6; ++y)
         {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, 5 - y);
            bindPressed(bt, () -> selectScale(X + Y * 8));
            bindLightState(() -> {
               final int librarySize = MusicalScaleLibrary.getInstance().getMusicalScalesCount();
               final int indexInLibrary = mDriver.getMusicalScale().getIndexInLibrary();

               final int index = X + 8 * Y;
               if (index >= librarySize)
                  return LedState.OFF;
               if (indexInLibrary == index)
                  return SCALE_ON_COLOR;
               return SCALE_OFF_COLOR;
            }, bt);
         }
      }
   }

   @Override
   protected String getModeDescription()
   {
      return "Scale and Key Chooser";
   }

   private void selectScale(final int i)
   {
      final MusicalScale musicalScale = MusicalScaleLibrary.getInstance().getMusicalScale(i);
      if (musicalScale == null)
         return;
      mDriver.setMusicalScale(musicalScale);

      playScale(0, ++mPlayScaleCount);
   }

   private void playScale(final int i, final int playScaleCount)
   {
      // Prevent multiple scales from playing at the same time.
      if (playScaleCount < mPlayScaleCount)
         return;

      final MusicalScale musicalScale = mDriver.getMusicalScale();
      if (i > musicalScale.getNotesCount())
         return;

      final int note = musicalScale.computeNote(mDriver.getMusicalKey(), 6, i);
      mDriver.getCursorTrack().playNote(note, 100);

      if (i + 1 <= musicalScale.getNotesCount())
      {
         final double duration = 60 / mDriver.getTransport().tempo().value().getRaw() / 2 * 1000;

         mDriver.getHost().scheduleTask(() -> playScale(i + 1, playScaleCount), (long)duration);
      }
   }

   private void selectKey(final int noteIndex)
   {
      if (noteIndex < 0)
         return;

      mDriver.setMusicalKey(noteIndex % 12);
      playScale(0, ++mPlayScaleCount);
   }

   private int mPlayScaleCount = 0;
}
