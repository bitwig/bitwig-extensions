package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.function.Function;
import java.util.function.Supplier;

import com.bitwig.extensions.framework.MusicalScale;

final class KeyboardLayer extends LaunchpadLayer
{
   private static final int[] CHROMATIC_NOTE_INDEXES = new int[] {
      0, 2, 4, 5, 7, 9, 11, 12, -1, 1, 3, -1, 6, 8, 10, -1,
   };

   private final static Color USED_WHITE_KEY_COLOR = Color.fromRgb255(255, 240, 240);
   private final static Color UNUSED_KEY_COLOR = Color.fromRgb255(50, 50, 55);
   private final static Color USED_BLACK_KEY_COLOR = Color.fromRgb255(120, 85, 42);

   private final static int MAX_OCTAVE = 10;

   static private boolean isBlackKey(int pitch)
   {
      pitch = pitch % 12;
      return pitch == 1 || pitch == 3 || pitch == 6 || pitch == 8 || pitch == 10;
   }

   @FunctionalInterface
   interface KeyPressedCallback
   {
      void onKeyPressed(int key, double velocity);
   }

   KeyboardLayer(
      final LaunchpadProControllerExtension driver,
      final String name,
      final int x0,
      final int y0,
      final int w0,
      final int h0,
      final Supplier<Color> trackColorSupplier,
      final Function<Integer /* Key */, Boolean> isPlaying,
      final KeyPressedCallback onKeyPlayed)
   {
      super(driver, name);

      mX0 = x0;
      mY0 = y0;
      mWidth = w0;
      mHeight = h0;
      mTrackColorSupplier = trackColorSupplier;
      mIsPlaying = isPlaying;

      for (int x = x0; x < x0 + w0; ++x)
      {
         for (int y = y0; y < y0 + h0; ++y)
         {
            final int X = x;
            final int Y = y;

            final Button padButton = driver.getPadButton(x, y);
            bindLightState(() -> computeLedState(X, Y), padButton);
            bindPressed(padButton, v -> {
               if (onKeyPlayed != null)
               {
                  final int key = calculateKeyForPosition(X, Y);
                  if (key >= 0 && key < 127)
                     onKeyPlayed.onKeyPressed(key, v);
               }
            });
         }
      }
   }

   private LedState computeLedState(final int x, final int y)
   {
      final Color trackColor = mTrackColorSupplier.get();

      return switch (mDriver.getKeyboardLayout())
         {
            case GUITAR -> computeGuitarLedState(x, y, trackColor);
            case LINE_3 -> computeLineLedState(3, x, y, trackColor);
            case LINE_7 -> computeLineLedState(7, x, y, trackColor);
            case PIANO -> computePianoLedState(x, y, trackColor);
         };

   }

   @Override
   protected void onActivate()
   {
      mDriver.mSafePitchesSetting.subscribe();
      mDriver.mHighlightScaleSetting.subscribe();
      mDriver.mHighlightRootKeySetting.subscribe();
      mDriver.mCursorTrack.playingNotes().subscribe();
   }

   @Override
   protected void onDeactivate()
   {
      mDriver.mSafePitchesSetting.unsubscribe();
      mDriver.mHighlightScaleSetting.unsubscribe();
      mDriver.mHighlightRootKeySetting.unsubscribe();
      mDriver.mCursorTrack.playingNotes().unsubscribe();
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

   private LedState computeGuitarLedState(final int x, final int y, final Color trackColor)
   {
      final MusicalScale scale = mDriver.getMusicalScale();

      final int midiNote = calculateGuitarKey(x, y);
      final int midiNoteBase = midiNote % 12;

      if (midiNote < 0 || midiNote > 127)
         return LedState.OFF;
      if (mIsPlaying.apply(midiNote))
         return LedState.STEP_PLAY;
      if (mDriver.mHighlightRootKeySetting.get() && midiNoteBase == mDriver.getMusicalKey())
         return new LedState(trackColor);
      else if (!mDriver.mHighlightScaleSetting.get() || scale.isMidiNoteInScale(mDriver.getMusicalKey(), midiNoteBase))
         return new LedState(isBlackKey(midiNote) ? USED_BLACK_KEY_COLOR : USED_WHITE_KEY_COLOR);
      return LedState.OFF;
   }

   private LedState computePianoLedState(final int x, final int y, final Color trackColor)
   {
      final MusicalScale scale = mDriver.getMusicalScale();

      final int noteIndex = CHROMATIC_NOTE_INDEXES[x + 8 * (y % 2)];
      final int pitch = calculateKeyboardKey(x, y);

      if (noteIndex == -1)
         return LedState.OFF;
      if (mIsPlaying.apply(pitch))
         return LedState.STEP_PLAY;
      if ((noteIndex % 12) == mDriver.getMusicalKey())
         return new LedState(trackColor);
      else if (scale.isMidiNoteInScale(mDriver.getMusicalKey(), noteIndex))
      {
         /* note in scale */
         return new LedState(isBlackKey(pitch) ? USED_BLACK_KEY_COLOR : USED_WHITE_KEY_COLOR);
      }

      /* note not in scale */
      return new LedState(UNUSED_KEY_COLOR);
   }

   private LedState computeLineLedState(final int X, final int x, final int y, final Color trackColor)
   {
      final MusicalScale musicalScale = mDriver.getMusicalScale();
      final int scaleSize = musicalScale.getNotesCount();

      final int noteIndex = x + X * y;
      final int midiNode = musicalScale.computeNote(mDriver.getMusicalKey(), mOctave, noteIndex);

      if (midiNode < 0 || midiNode > 127)
         return LedState.OFF;
      if (mIsPlaying.apply(midiNode))
         return LedState.STEP_PLAY;
      if (noteIndex % scaleSize == 0)
         return new LedState(trackColor);
      else
         return new LedState(USED_WHITE_KEY_COLOR);
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

         final int key = calculateKeyForPosition(x, y);
         table[i] = (key >= 0 && key < 128) ? key : -1;
      }
   }

   private int calculateKeyForPosition(final int x, final int y)
   {
      if (x < mX0 || mX0 + mWidth <= x || y < mY0 || mY0 + mHeight <= y)
         return -1;

      return switch (mDriver.getKeyboardLayout())
         {
            case GUITAR -> calculateGuitarKey(x, y);
            case LINE_3 -> calculateLineXKey(3, x, y);
            case LINE_7 -> calculateLineXKey(7, x, y);
            case PIANO -> calculateKeyboardKey(x, y);
            default -> -1;
         };
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

      if (mDriver.mSafePitchesSetting.get() && !musicalScale.isMidiNoteInScale(musicalKey, key))
         return -1;
      return key;
   }

   private final int mX0;
   private final int mY0;
   private final int mWidth;
   private final int mHeight;
   private int mOctave = 3;
   private final Supplier<Color> mTrackColorSupplier;
   private final Function<Integer, Boolean> mIsPlaying;
}
