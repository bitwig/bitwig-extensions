package com.bitwig.extensions.controllers.nativeinstruments.commons;

public enum ColorBrightness {
   DARKENED(0),  //
   BRIGHT(2), //
   DIMMED(1),  //
   SUPERBRIGHT(3); //

   private int adjust;

   ColorBrightness(final int adjust) {
      this.adjust = adjust;
   }

   public int getAdjust() {
      return adjust;
   }
}
