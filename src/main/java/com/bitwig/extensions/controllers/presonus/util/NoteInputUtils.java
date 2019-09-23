package com.bitwig.extensions.controllers.presonus.util;

public class NoteInputUtils
{
   public static Integer[] NO_NOTES;
   public static Integer[] ALL_NOTES;

   static
   {
      NO_NOTES = new Integer[128];
      ALL_NOTES = new Integer[128];

      for(int i=0; i<128; i++)
      {
         NO_NOTES[i] = -1;
         ALL_NOTES[i] = i;
      }
   }
}
