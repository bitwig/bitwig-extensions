package com.bitwig.extensions.controllers.novation.launchkey_mk3;

public enum LightState {
   NORMAL(0),
   FLASHING(1),
   PULSING(2);

   private final int channel;

   LightState(final int channel) {
      this.channel = channel;
   }

   public int getChannel() {
      return channel;
   }

}
