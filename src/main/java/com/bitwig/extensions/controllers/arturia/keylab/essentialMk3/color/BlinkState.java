package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color;

public enum BlinkState {
   BLINK1((byte) 0),
   BLINK2((byte) 1),
   BLINK3((byte) 2),
   PULSING((byte) 3),
   NONE((byte) 32);
   private final byte code;

   BlinkState(final byte code) {
      this.code = code;
   }

   public byte getCode() {
      return code;
   }
}
