package com.bitwig.extensions.controllers.novation.launchpad_pro;

enum KeyboardLayout
{
   GUITAR, LINE_3, LINE_7, PIANO;

   static KeyboardLayout fromString(final String name)
   {
      return switch (name)
         {
            case "Guitar" -> GUITAR;
            case "Piano" -> PIANO;
            case "Line 3" -> LINE_3;
            case "Line 7" -> LINE_7;
            default -> throw new IllegalStateException();
         };
   }

   @Override
   public String toString()
   {
      return switch (this)
         {
            case GUITAR -> "Guitar";
            case LINE_3 -> "Line 3";
            case LINE_7 -> "Line 7";
            case PIANO -> "Piano";
            default -> throw new IllegalStateException();
         };
   }

   static final String[] OPTIONS = new String[]{"Guitar", "Piano", "Line 3", "Line 7"};
}
