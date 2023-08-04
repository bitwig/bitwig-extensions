package com.bitwig.extensions.controllers.maudio.oxygenpro;

public enum OxygenCcAssignments {
   TRACK_1(0x20),
   SLIDER_1(0x0C),
   KNOB_1(0x16),

   REC_MODE(0x3A),
   SELECT_MODE(0x3B),
   MUTE_MODE(0x3C),
   SOLO_MODE(0x3D),
   OXY_MODE(0x39),
   SHIFT(0x69, 0xC),
   DAW(0x71),
   PRESET(0x70),
   BACK(0x68),
   ENCODER_PUSH(0x66),
   ENCODER(0x67),
   METRO(0x6A),
   BANK_LEFT(0x6E),
   BANK_RIGHT(0x6F),
   LOOP(0x72),
   FAST_RWD(0x73),
   FAST_FWD(0x74),
   STOP(0x75),
   PLAY(0x76),
   RECORD(0x77),
   VOLUME_MODE(0x53),
   PAN_MODE(0x55),
   DEVICE_MODE(0x56),
   SENDS_MODE(0x57),
   SCENE_LAUNCH1(0x6B),
   SCENE_LAUNCH2(0x6C);

   private int ccNr;
   private int channel;

   OxygenCcAssignments(int ccNr) {
      this(ccNr, 0);
   }

   OxygenCcAssignments(int ccNr, int channel) {
      this.ccNr = ccNr;
      this.channel = channel;
   }

   public int getCcNr() {
      return ccNr;
   }

   public int getChannel() {
      return channel;
   }
}
