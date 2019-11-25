package com.bitwig.extensions.controllers.novation.launchpad_pro;

public class ScaleAndKeyChooserMode extends Mode
{
   private static final int[] CHROMATIC_NOTE_INDEXES = new int[] { 0, 2, 4, 5, 7, 9, 11, 12, -1, 1, 3, -1, 6, 8, 10,
         -1, };

   private final static Color ROOT_KEY_COLOR = Color.fromRgb255(11, 100, 63);

   private final static Color USED_KEY_COLOR = Color.fromRgb255(255, 240, 240);

   private final static Color UNUSED_KEY_COLOR = Color.fromRgb255(40, 40, 40);

   private final static Color SCALE_ON_COLOR = Color.fromRgb255(50, 167, 202);

   private final static Color SCALE_OFF_COLOR = Color.scale(SCALE_ON_COLOR, 0.2f);

   ScaleAndKeyChooserMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "scale-key-chooser");
   }

   @Override
   protected String getModeDescription()
   {
      return "Scale and Key Chooser";
   }

   @Override
   protected void doActivate()
   {
   }

   @Override
   public void paint()
   {
      paintKeyboard();
      paintScales();

      // light off the scene buttons
      for (int i = 0; i < 8; ++i)
         mDriver.getRightLed(i).clear();
   }

   private void paintKeyboard()
   {
      final MusicalScale scale = mDriver.getMusicalScale();

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 2; ++y)
         {
            final int noteIndex = CHROMATIC_NOTE_INDEXES[x + 8 * y];
            if (noteIndex == -1)
            {
               /* dead led */
               mDriver.getPadLed(x, 6 + y).clear();
            }
            else if ((noteIndex % 12) == mDriver.getMusicalKey())
            {
               /* root key */
               mDriver.getPadLed(x, 6 + y).setColor(ROOT_KEY_COLOR);
            }
            else if (scale.isMidiNoteInScale(mDriver.getMusicalKey(), noteIndex))
            {
               /* note in scale */
               mDriver.getPadLed(x, 6 + y).setColor(USED_KEY_COLOR);
            }
            else
            {
               /* note not in scale */
               mDriver.getPadLed(x, 6 + y).setColor(UNUSED_KEY_COLOR);
            }
         }
      }
   }

   private void paintScales()
   {
      final int librarySize = MusicalScaleLibrary.getInstance().getMusicalScalesCount();
      final int indexInLibrary = mDriver.getMusicalScale().getIndexInLibrary();

      for (int i = 0; i < 8; ++i)
      {
         for (int j = 0; j < 6; ++j)
         {
            final Led led = mDriver.getPadLed(i, 5 - j);
            final int index = i + 8 * j;
            if (index >= librarySize)
               led.clear();
            else if (indexInLibrary == index)
               led.setColor(SCALE_ON_COLOR);
            else
               led.setColor(SCALE_OFF_COLOR);
         }
      }
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      if (y < 6)
         selectScale(x + (5 - y) * 8);
      else
         selectKey(CHROMATIC_NOTE_INDEXES[x + 8 * (y - 6)]);
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
