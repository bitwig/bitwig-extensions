package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

public enum KeylabIcon {
   NONE(0),
   ARTURIA(1),
   BACK(2),
   FULL_OCTAVE(7),
   COMPUTER(8),
   SMALL_POT(9),
   SMALL_DELETE(10),
   DRUMS(11),
   SMALL_DRUMS(12),
   FW(15),
   KEY_TYPE1(16),
   KEY_TYPE2(17),
   KEY_TYPE3(18),
   KEY_TYPE4(19),
   KEY_TYPE1_FULL(20),
   KEY_TYPE2_FULL(21),
   KEY_TYPE3_FULL(22),
   KEY_TYPE4_FULL(23),
   MIDI_DIN(30),
   ARROW_LEFT(35),
   ARROW_RIGHT(36),
   ARROW_LEFT_FULL(37),
   ARROW_RIGHT_FULL(38),
   PENCIL(39),
   REPLACING(42),
   RESET(43),
   RESET_SMALL(44),
   SEQUENCE(45),
   SFX(47),
   SFX_SMALL(48),
   PLUS(51),
   UPDATE(52),
   USER(53),
   USER_SMALL(54),
   BRACKET_BOTTOM(57),
   BRACKET_TOP(58),
   BRACKET_LEFT(59),
   BRACKET_RIGHT(60),
   SELECTED(61),
   BANK(62),
   MIXER(63),
   SEARCH(64),
   BIG_OCTAVE(65),
   STRING(49),
   BITWIG(69),
   LAST_ENTRY(72);

   private final int key;


   KeylabIcon(final int key) {
      this.key = key;
   }

   public int getKey() {
      return key;
   }

   public KeylabIcon next() {
      final KeylabIcon[] values = KeylabIcon.values();
      int index = -1;
      for (int i = 0; i < values.length; i++) {
         if (this == values[i]) {
            index = i;
            break;
         }
      }
      if (index != -1) {
         if (index + 1 < values.length) {
            return values[index + 1];
         } else {
            return values[0];
         }
      }
      return this;
   }
}
