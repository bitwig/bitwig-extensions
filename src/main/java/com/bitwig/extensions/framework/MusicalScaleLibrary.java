package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class MusicalScaleLibrary
{
   private MusicalScaleLibrary()
   {
      /* Classic */
      addScale(new MusicalScale("Chromatic", new int[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }));
      addScale(new MusicalScale("Ionan (Major)", new int[]{ 0, 2, 4, 5, 7, 9, 11 }));
      addScale(new MusicalScale("Dorian", new int[]{ 0, 2, 3, 5, 7, 9, 10 }));
      addScale(new MusicalScale("Phrygian", new int[]{ 0, 1, 3, 5, 7, 8, 10 }));
      addScale(new MusicalScale("Lydian", new int[]{ 0, 2, 4, 6, 7, 9, 11 }));
      addScale(new MusicalScale("Mixolydian", new int[]{ 0, 2, 4, 5, 7, 9, 10 }));
      addScale(new MusicalScale("Aeolian (Minor)", new int[]{ 0, 2, 3, 5, 7, 8, 10 }));
      addScale(new MusicalScale("Locrian", new int[]{ 0, 1, 3, 5, 6, 8, 10 }));

      /* 5 keys */
      addScale(new MusicalScale("Hirajoshi", new int[]{ 0, 2, 3, 7, 8 }));
      addScale(new MusicalScale("Iwato", new int[]{ 0, 1, 5, 6, 10 })); // related to Hirajoshi (after)
      addScale(new MusicalScale("Kumoi", new int[]{ 0, 2, 3, 7, 9 }));
      addScale(new MusicalScale("In Sen", new int[]{ 0, 1, 5, 7, 10 })); // related to Kumoi (after)
      addScale(new MusicalScale("Yo scale", new int[]{ 0, 2, 5, 7, 9 }));
      addScale(new MusicalScale("Minor Pentatonic", new int[]{ 0, 3, 5, 7, 10 })); // related to Yo Scale (after)
      addScale(new MusicalScale("Major Pentatonic", new int[]{ 0, 2, 4, 7, 9 })); // related to Minor Pentatonic (after)

      /* 6 keys */
      addScale(new MusicalScale("Whole Tone", new int[]{ 0, 2, 4, 6, 8, 10 }));
      addScale(new MusicalScale("Blues", new int[]{ 0, 3, 5, 6, 7, 10 }));
      addScale(new MusicalScale("Major Blues", new int[]{ 0, 2, 3, 4, 7, 9 }));

      /* scales with augmented second */
      addScale(new MusicalScale("Todi", new int[]{ 0, 1, 3, 5, 6, 7, 11 }));
      addScale(new MusicalScale("Phrygian Dominant", new int[]{ 0, 1, 4, 5, 7, 8, 10 }));
      addScale(new MusicalScale("Double Harmonic", new int[]{ 0, 1, 4, 5, 7, 8, 11 }));
      addScale(new MusicalScale("Marva", new int[]{ 0, 1, 4, 6, 7, 9, 11 }));
      addScale(new MusicalScale("Harmonic Minor", new int[]{ 0, 2, 3, 5, 7, 8, 11 }));

      /* Others */
      addScale(new MusicalScale("Melodic Minor (ascending)", new int[]{ 0, 2, 3, 5, 7, 9, 11 }));
      addScale(new MusicalScale("Hungarian Minor", new int[]{ 0, 2, 3, 6, 7, 8, 11 }));
      addScale(new MusicalScale("Ukranian Dorian", new int[]{ 0, 2, 3, 6, 7, 9, 10 }));
      addScale(new MusicalScale("Super Locrian", new int[]{ 0, 1, 3, 4, 6, 8, 10 }));
      addScale(new MusicalScale("Whole Half", new int[]{ 0, 2, 3, 5, 6, 8, 9, 11 }));
      addScale(new MusicalScale("Half-Whole Diminished", new int[]{ 0, 1, 3, 4, 6, 7, 9, 10 }));
      addScale(new MusicalScale("BeBop Major", new int[]{ 0, 2, 4, 5, 7, 8, 9, 11 }));
      addScale(new MusicalScale("BeBop Dorian", new int[]{ 0, 2, 3, 4, 5, 7, 9, 10 }));
      addScale(new MusicalScale("BeBop Mixolydian", new int[]{ 0, 2, 4, 5, 7, 9, 10, 11 }));
      addScale(new MusicalScale("BeBop Minor", new int[]{ 0, 2, 3, 5, 7, 8, 10, 11 }));

      final int numScales = mMusicalScales.size();
      mScalesName = new String[numScales];
      for (int i = 0; i < numScales; ++i)
         mScalesName[i] = mMusicalScales.get(i).getName();
   }

   private void addScale(final MusicalScale musicalScale)
   {
      assert !mMusicalScaleHashMap.containsKey(musicalScale.getName());
      assert !mMusicalScales.contains(musicalScale);

      /* Check that no other scale has the same notes */
      for (final MusicalScale scale : mMusicalScales)
         assert !Arrays.equals(scale.getNotes(), musicalScale.getNotes());

      musicalScale.setIndexInLibrary(mMusicalScales.size());
      mMusicalScales.add(musicalScale);
      mMusicalScaleHashMap.put(musicalScale.getName(), musicalScale);
   }

   static public MusicalScaleLibrary getInstance()
   {
      return mInstance;
   }

   public final int getMusicalScalesCount()
   {
      return mMusicalScales.size();
   }

   public final MusicalScale getMusicalScale(final int index)
   {
      if (index < 0 || index >= mMusicalScales.size())
         return null;
      return mMusicalScales.get(index);
   }

   public MusicalScale getMusicalScale(final String scaleName)
   {
      return mMusicalScaleHashMap.get(scaleName);
   }

   public String[] getScalesName()
   {
      return mScalesName;
   }

   private static final MusicalScaleLibrary mInstance = new MusicalScaleLibrary();
   private final List<MusicalScale> mMusicalScales = new ArrayList<>();
   private final HashMap<String, MusicalScale> mMusicalScaleHashMap = new HashMap<>();
   private final String[] mScalesName;
}
