package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

public enum CcAssignment {
   ENCODER_PRESS(0x8), //
   ENCODER_TOUCH(0x9), //
   STRIP_TOUCH(0x2),

   PLAY(57), //
   STOP(59), //
   RECORD(58), //
   RESTART(53), //
   ERASE(54), //
   TAP(55), //
   FOLLOW(56), //

   GROUP(34), //
   AUTO(35), //
   LOCK(36), //
   NOTE_REPEAT(37), //

   SCENE(85), //
   PATTERN(86), //
   VARIATION(88), //
   DUPLICATE(89), //
   SELECT(90), //
   SOLO(91), //
   MUTE(92), //

   FIXED_VEL(80), //
   PAD_MODE(81), //
   KEYBOARD(82), //
   CHORD(84), //
   STEP(83), //

   VOLUME(44), //
   SWING(46), //
   TEMPO(48), //
   PLUGIN(45), //
   SAMPLING(46), //

   MASCHINE(38), //
   FAVORITE(39), //
   SEARCH(40), //

   PITCH(49), //
   MOD(50), //
   PERFORM(51), //
   NOTES(52);//


   private int ccNr;
   private int channel;

   CcAssignment(final int ccNr, final int channel) {
      this.ccNr = ccNr;
      this.channel = channel;
   }

   CcAssignment(final int ccNr) {
      this.ccNr = ccNr;
      this.channel = 0;
   }

   public int getCcNr() {
      return ccNr;
   }

   public void setCcNr(final int ccNr) {
      this.ccNr = ccNr;
   }

   public int getChannel() {
      return channel;
   }

   public void setChannel(final int channel) {
      this.channel = channel;
   }


}
