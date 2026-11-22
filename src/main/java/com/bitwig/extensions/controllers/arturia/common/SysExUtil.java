package com.bitwig.extensions.controllers.arturia.common;

/** Shared SysEx text folding (literal copy from KeyLab mk3). */
public class SysExUtil
{
   private SysExUtil()
   {
   }

   public static String convert(final String text)
   {
      if (text == null)
      {
         return "";
      }
      final StringBuilder builder = new StringBuilder(text.length());
      for (int i = 0; i < text.length(); i++)
      {
         builder.append(convert(text.charAt(i)));
      }
      return builder.toString();
   }

   public static char convert(final char c)
   {
      if (c < 128)
      {
         return c;
      }
      switch (c)
      {
         case 'Á':
         case 'À':
         case 'Ä':
            return 'A';
         case 'É':
         case 'È':
            return 'E';
         case 'á':
         case 'à':
         case 'ä':
            return 'a';
         case 'Ö':
            return 'O';
         case 'Ü':
            return 'U';
         case 'è':
         case 'é':
            return 'e';
         case 'ö':
            return 'o';
         case 'ü':
            return 'u';
         case 'ß':
            return 's';
      }
      return '?';
   }
}
