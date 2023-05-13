package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.ModeType;

public enum PadMode implements ModeType {
   SESSION(2),
   DRUM(1),
   SCALE_CHORD(3),
   USER_CHORD(4),
   CUSTOM1(5),
   CUSTOM2(6),
   CUSTOM3(7),
   CUSTOM4(8);

   private final int id;

   PadMode(final int id) {
      this.id = id;
   }

   @Override
   public int getId() {
      return id;
   }

}
