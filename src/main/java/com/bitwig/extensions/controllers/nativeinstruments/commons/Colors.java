package com.bitwig.extensions.controllers.nativeinstruments.commons;

public enum Colors {
   OFF(0), //
   RED(1),  //
   ORANGE(2),  //
   LIGHT_ORANGE(3),  //
   WARM_YELLOW(4), //
   YELLOW(5),  //
   LIME(6),  //
   GREEN(7),  //
   MINT(8), //
   CYAN(9),  //
   TURQUIOSE(10),
   BLUE(11),  //
   PLUM(12), //
   VIOLET(13),  //
   PURPLE(14),  //
   MAGENTA(15),  //
   FUCHSIA(16),  //
   WHITE(17);

   private int index;

   Colors(final int index) {
      this.index = index;
   }

   public int getIndexValue(final ColorBrightness brightness) {
      if (index == 0) {
         return 0;
      }
      return index * 4 + brightness.getAdjust();
   }
}
