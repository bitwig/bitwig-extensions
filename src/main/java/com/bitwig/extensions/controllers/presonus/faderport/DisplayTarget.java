package com.bitwig.extensions.controllers.presonus.faderport;

public interface DisplayTarget
{
   int getBarValue();

   default String getText(final int line)
   {
      return "";
   }

   default int getTextAlignment(final int line)
   {
      return 0;
   }

   default boolean isTextInverted(final int line)
   {
      return false;
   }

   default ValueBarMode getValueBarMode()
   {
      return ValueBarMode.Normal;
   }

   default DisplayMode getMode()
   {
      return DisplayMode.Default;
   }
}
