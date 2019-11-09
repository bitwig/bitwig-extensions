package com.bitwig.extensions.controllers.novation.common;

public enum SimpleLedColor
{
   RedLow(13), Red(15),
   AmberLow(29), Amber(63),
   Yellow(62), YellowLow(45),
   GreenLow(28), Green(60),

   RedFlash(11), AmberFlash(59), YellowFlash(58), GreenFlash(56),

   Off(12);

   public int value()
   {
      return mColorValue;
   }

   private final int mColorValue;

   SimpleLedColor(int colorValue)
   {
      mColorValue = colorValue;
   }
}
