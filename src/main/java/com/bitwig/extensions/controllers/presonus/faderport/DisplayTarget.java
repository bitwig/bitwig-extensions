package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extensions.framework.targets.Target;

public interface DisplayTarget extends Target
{
   int getBarValue();

   default String getText(int line)
   {
      return "";
   }

   default int getTextAlignment(int line)
   {
      return 0;
   }

   default boolean isTextInverted(int line)
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
