package com.bitwig.extensions.util;

public class ValueUtils
{
   public static int doubleToUnsigned7(final double x)
   {
      return Math.max(0, Math.min((int)(x * 127.0), 127));
   }
}
