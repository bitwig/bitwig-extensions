package com.bitwig.extensions.controllers.akai.apcmk2;

import java.util.Arrays;

public enum OrientationFollowType {
   AUTOMATIC("Automatic"), //
   FIXED_VERTICAL("Mix Panel Layout"), //
   FIXED_HORIZONTAL("Arrange Panel Layout");

   private final String label;

   OrientationFollowType(String label) {
      this.label = label;
   }

   public String getLabel() {
      return label;
   }

   public static OrientationFollowType toType(String value) {
      return Arrays.stream(OrientationFollowType.values())
         .filter(type -> type.label.equals(value))
         .findFirst()
         .orElse(OrientationFollowType.FIXED_VERTICAL);
   }
}
