package com.bitwig.extensions.controllers.novation.launchkey_mk3;

public enum LaunchColor {
   BLACK(0, 0, 0), //
   WHIT(3, 0, 0), //
   GREY_LO(71), //
   RED(5),
   MAGENTA(57),
   ORANGE(9),
   ORANGE2(10),
   YELLOW(13),
   AQUA(41);
   /*
   GREY_MD(117, 1, 2), //
   GREY_HALF(118, 1, 2), //
   GREY_HI(2), //
   WHITE(3, 1, 3), //
   ROSE(4, 2, 5), //
   RED_HI(5), //
   RED(6), //
   RED_LO(7), //
   RED_AMBER(8), //
   AMBER_HI(9), //
   AMBER(10), //
   AMBER_LO(11), //
   AMBER_YELLOW(12), //
   YELLOW_HI(13), //
   YELLOW(14), //
   YELLOW_LO(15), //
   YELLOW_LIME(16), //
   LIME_HI(17), //
   LIME(18), //
   LIME_LO(19), //
   LIME_GREEN(20), //
   GREEN_HI(21), //
   GREEN(22), //
   GREEN_LO(23), //
   GREEN_SPRING(24), //
   SPRING_HI(25), //
   SPRING(26), //
   SPRING_LO(27), //
   SPRING_TURQUOISE(28), //
   TURQUOISE_LO(29), //
   TURQUOISE(30), //
   TURQUOISE_HI(31), //
   TURQUOISE_CYAN(32), //
   CYAN_HI(33), //
   CYAN(34), //
   CYAN_LO(35), //
   CYAN_SKY(36), //
   SKY_HI(37), //
   SKY(38), //
   SKY_LO(39), //
   SKY_OCEAN(40), //
   OCEAN_HI(41), //
   OCEAN(42), //
   OCEAN_LO(43), //
   OCEAN_BLUE(44), //
   BLUE_HI(45), //
   BLUE(46), //
   BLUE_LO(47), //
   BLUE_ORCHID(48), //
   PURPLE_HI(49), //
   PURPLE(50), //
   PURPLE_LO(51), //
   ORCHID_MAGENTA(52), //
   MAGENTA_HI(53), //
   MAGENTA(54), //
   MAGENTA_LO(55), //
   MAGENTA_PINK(56), //
   PINK_HI(57), //
   PINK(58), //
   PINK_LO(59), //
   ORANGE(60, 11, 9)
   */


   private final int index;
   private final int lowIndex;
   private final int hiIndex;

   LaunchColor(final int index) {
      this.index = index;
      lowIndex = index + 1;
      hiIndex = index - 1;
   }

   LaunchColor(final int index, final int lo, final int hi) {
      this.index = index;
      lowIndex = lo;
      hiIndex = hi;
   }

   public int getIndex() {
      return index;
   }

   public int getLowIndex() {
      return lowIndex;
   }

   public int getHiIndex() {
      return hiIndex;
   }
}
