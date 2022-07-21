package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

public enum ControlMode {
   DEVICE(2),
   VOLUME(1),
   SEND_A(4),
   SEND_B(5),
   PAN(3);

   private final int faderId;

   ControlMode(final int id) {
      faderId = id;
   }

   public int getFaderId() {
      return faderId;
   }
}
