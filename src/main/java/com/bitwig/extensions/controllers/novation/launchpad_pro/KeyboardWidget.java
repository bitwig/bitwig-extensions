package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;
import com.bitwig.extensions.controllers.akai.apc40_mkii.Led;

final class KeyboardWidget
{
   private static final int[] CHROMATIC_NOTE_INDEXES = new int[]{
      0,  2, 4,  5, 7, 9, 11, 12,
      -1, 1, 3, -1, 6, 8, 10, -1,
   };

   private final Color UP_DOWN_ON_COLOR = new Color(0.f, 0.f, 1.f);
   private final Color UP_DOWN_OFF_COLOR = new Color(0.f, 0.f, 0.2f);
   private final Color LEFT_RIGHT_ON_COLOR = new Color(0.f, 1.f, 0.f);
   private final Color LEFT_RIGHT_OFF_COLOR = new Color(0.f, 0.2f, 0.f);

   private final static Color KEYBOARD_ON_COLOR = Color.fromRgb255(11, 100, 63);
   private final static Color KEYBOARD_OFF_COLOR = Color.scale(KEYBOARD_ON_COLOR, 0.2f);
   private final static Color ROOT_KEY_COLOR = Color.fromRgb255(11, 100, 63);
   private final static Color USED_WHITE_KEY_COLOR = Color.fromRgb255(255, 240, 240);
   private final static Color UNUSED_KEY_COLOR = Color.fromRgb255(50, 50, 55);
   private final static Color USED_BLACK_KEY_COLOR = Color.fromRgb255(120, 85, 42);
   private final static Color PLAYING_KEY_COLOR = Color.fromRgb255(0, 255, 0);
   private final static Color INVALID_KEY_COLOR = Color.RED_LOW;

   private final static int MAX_OCTAVE = 10;

   static private boolean isBlackKey(int pitch)
   {
      pitch = pitch % 12;
      return pitch == 1 || pitch == 3 || pitch == 6 || pitch == 8 || pitch == 10;
   }

   KeyboardWidget(final LaunchpadProControllerExtension driver, final int x0, final int y0, final int w0, final int h0)
   {
      mDriver = driver;
      mX0 = x0;
      mY0 = y0;
      mWidth = w0;
      mHeight = h0;
   }

   void activate()
   {
      mDriver.getCursorTrack().playingNotes().subscribe();
   }

   void deactivate()
   {
      mDriver.getCursorTrack().playingNotes().unsubscribe();
   }

   void paint(final ColorValue trackColor)
   {
      switch (mDriver.getKeyboardLayout())
      {
         case GUITAR:
            paintGuitar(trackColor);
            break;

         case LINE_3:
            paintLine(3, trackColor);
            break;

         case LINE_7:
            paintLine(7, trackColor);
            break;

         case PIANO:
            paintPiano(trackColor);
            break;
      }
   }

   boolean canOctaveUp()
   {
      return mOctave < MAX_OCTAVE;
   }

   boolean canOctaveDown()
   {
      return mOctave > 0;
   }

   void octaveDown()
   {
      if (mOctave > 0)
         --mOctave;
   }

   void octaveUp()
   {
      if (mOctave < MAX_OCTAVE)
         ++mOctave;
   }

   private void paintGuitar(final ColorValue trackColor)
   {
      final PlayingNoteArrayValue playingNotes = mDriver.getCursorTrack().playingNotes();
      final MusicalScale scale = mDriver.getMusicalScale();

      for (int i = mX0; i < mX0 + mWidth; ++i)
         for (int j = mY0; j < mY0 + mHeight; ++j)
         {
            final Button bt = mDriver.getPadButton(i, j);
            final int midiNote = calculateGuitarKey(i, j);
            final int midiNoteBase = midiNote % 12;

            if (midiNote < 0 || midiNote > 127)
               bt.setColor(Color.OFF);
            else if (playingNotes.isNotePlaying(midiNote))
               bt.setColor(PLAYING_KEY_COLOR);
            else if (mDriver.shouldHihlightRootKey() && midiNoteBase == mDriver.getMusicalKey())
               bt.setColor(trackColor);
            else if (!mDriver.shouldHighlightScale() || scale.isMidiNoteInScale(mDriver.getMusicalKey(), midiNoteBase))
               bt.setColor(isBlackKey(midiNote) ? USED_BLACK_KEY_COLOR : USED_WHITE_KEY_COLOR);
            else
               bt.setColor(Color.OFF);
         }
   }

   private void paintPiano(final ColorValue trackColor)
   {
      final PlayingNoteArrayValue playingNotes = mDriver.getCursorTrack().playingNotes();
      final MusicalScale scale = mDriver.getMusicalScale();

      for (int x = mX0; x < mX0 + mWidth; ++x)
      {
         for (int y = mY0; y < mY0 + mHeight; ++y)
         {
            final int noteIndex = CHROMATIC_NOTE_INDEXES[x + 8 * (y % 2)];
            final int pitch = calculateKeyboardKey(x, y);

            if (noteIndex == -1)
            {
               /* dead led */
               mDriver.getPadButton(x, y).clear();
            }
            else if (playingNotes.isNotePlaying(pitch))
            {
               /* playing note */
               mDriver.getPadButton(x, y).setColor(PLAYING_KEY_COLOR);
            }
            else if ((noteIndex % 12) == mDriver.getMusicalKey())
            {
               /* root key */
               mDriver.getPadButton(x, y).setColor(trackColor);
            }
            else if (scale.isMidiNoteInScale(mDriver.getMusicalKey(), noteIndex))
            {
               /* note in scale */
               mDriver.getPadButton(x, y).setColor(isBlackKey(pitch) ? USED_BLACK_KEY_COLOR : USED_WHITE_KEY_COLOR);
            }
            else
            {
               /* note not in scale */
               mDriver.getPadButton(x, y).setColor(UNUSED_KEY_COLOR);
            }
         }
      }
   }

   private void paintLine(final int X, final ColorValue trackColor)
   {
      final MusicalScale musicalScale = mDriver.getMusicalScale();
      final int scaleSize = musicalScale.getNotesCount();
      final PlayingNoteArrayValue playingNotes = mDriver.getCursorTrack().playingNotes();

      for (int i = mX0; i < mX0 + mWidth; ++i)
         for (int j = mY0; j < mY0 + mHeight; ++j)
         {
            final Button button = mDriver.getPadButton(i, j);
            final int noteIndex = i + X * j;
            final int midiNode = musicalScale.computeNote(mDriver.getMusicalKey(), mOctave, noteIndex);
            if (midiNode < 0 || midiNode > 127)
               button.clear();
            else if (playingNotes.isNotePlaying(midiNode))
               button.setColor(PLAYING_KEY_COLOR);
            else if (noteIndex % scaleSize == 0)
               button.setColor(trackColor);
            else
               button.setColor(USED_WHITE_KEY_COLOR);
         }
   }

   void updateKeyTranslationTable(final Integer[] table)
   {
      assert table.length == 128;

      for (int i = 0; i < 128; ++i)
      {
         if (i < 11 || (i % 10 == 0) || (i % 10 == 9) || i > 89)
         {
            table[i] = -1;
            continue;
         }

         final int x = (i % 10) - 1;
         final int y = (i / 10) - 1;

         table[i] = calculateKeyForPosition(x, y);
      }
   }

   int calculateKeyForPosition(final int x, final int y)
   {
      if (x < mX0 || mX0 + mWidth <= x || y < mY0 || mY0 + mHeight <= y)
         return -1;

      switch (mDriver.getKeyboardLayout())
      {
         case GUITAR:
            return calculateGuitarKey(x, y);
         case LINE_3:
            return calculateLineXKey(3, x, y);
         case LINE_7:
            return calculateLineXKey(7, x, y);
         case PIANO:
            return calculateKeyboardKey(x, y);
         default:
            return -1;
      }
   }

   private int calculateKeyboardKey(final int x, final int y)
   {
      final int noteIndex = CHROMATIC_NOTE_INDEXES[x + 8 * (y % 2)];
      if (noteIndex == -1)
         return -1;

      int pitch = noteIndex + 12 * (y / 2) + 12 * mOctave;

      if (pitch > 127)
         pitch = -1;

      assert pitch >= -1;
      assert pitch < 128;

      return pitch;
   }

   private int calculateLineXKey(final int X, final int x, final int y)
   {
      final MusicalScale musicalScale = mDriver.getMusicalScale();
      final int noteIndex = x + X * y;
      return musicalScale.computeNote(mDriver.getMusicalKey(), mOctave, noteIndex);
   }

   private int calculateGuitarKey(final int x, final int y)
   {
      final int note = mOctave * 12 + x + 5 * y;

      return filterKey(note);
   }

   private int filterKey(final int key)
   {
      final int musicalKey = mDriver.getMusicalKey();
      final MusicalScale musicalScale = mDriver.getMusicalScale();

      if (mDriver.wantsSafePitches() && !musicalScale.isMidiNoteInScale(musicalKey, key))
         return -1;
      return key;
   }

   private final int mX0;
   private final int mY0;
   private final int mWidth;
   private final int mHeight;
   private final LaunchpadProControllerExtension mDriver;
   private int mOctave = 3;
}
