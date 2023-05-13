package com.bitwig.extensions.controllers.mackie.display;

public enum RingDisplayType {
   PAN_FILL(17, 10), //
   FILL_LR(33, 10), //
   SINGLE(1, 10),  //
   CENTER_FILL(49, 10), //
   FILL_LR_0(32, 11); //
   private final int offset;
   private final int range;

   RingDisplayType(final int offset, final int range) {
      this.offset = offset;
      this.range = range;
   }

   public int getOffset() {
      return offset;
   }

   public int getRange() {
      return range;
   }
}
