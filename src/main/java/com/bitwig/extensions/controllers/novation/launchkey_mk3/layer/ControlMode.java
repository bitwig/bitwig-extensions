package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.ModeType;

public enum ControlMode implements ModeType {
   DEVICE(2),
   VOLUME(1),
   SEND_A(4),
   SEND_B(5),
   PAN(3);

   private final int id;

   ControlMode(final int id) {
      this.id = id;
   }

   @Override
   public int getId() {
      return id;
   }

}
