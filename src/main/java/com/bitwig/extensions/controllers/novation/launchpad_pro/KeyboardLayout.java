package com.bitwig.extensions.controllers.novation.launchpad_pro;

enum KeyboardLayout
{
   GUITAR, LINE_3, LINE_7, PIANO;

   static KeyboardLayout fromString(final String name)
   {
      switch (name)
      {
         case "Guitar":
            return GUITAR;

         case "Piano":
            return PIANO;

         case "Line 3":
            return LINE_3;

         case "Line 7":
            return LINE_7;

         default:
            return GUITAR;
      }
   }

   @Override
   public String toString()
   {
      switch (this)
      {
         case GUITAR:
            return "Guitar";
         case LINE_3:
            return "Line 3";
         case LINE_7:
            return "Line 7";
         case PIANO:
            return "Piano";
         default:
            return "Guitar";
      }
   }

   static final String[] OPTIONS = new String[]{"Guitar", "Piano", "Line 3", "Line 7"};
}
