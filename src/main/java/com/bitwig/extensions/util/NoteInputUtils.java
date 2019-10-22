package com.bitwig.extensions.util;

public class NoteInputUtils
{
   public static Integer[] NO_NOTES;
   public static Integer[] ALL_NOTES;
   public static Integer[] NORMAL_VELOCITY;
   public static Integer[] FULL_VELOCITY;

   static
   {
      NO_NOTES = new Integer[128];
      ALL_NOTES = new Integer[128];
      NORMAL_VELOCITY = new Integer[128];
      FULL_VELOCITY = new Integer[128];

      for(int i=0; i<128; i++)
      {
         NO_NOTES[i] = -1;
         ALL_NOTES[i] = i;
         NORMAL_VELOCITY[i] = i;
         FULL_VELOCITY[i] = i > 0 ? 127 : 0;
      }
   }
}
