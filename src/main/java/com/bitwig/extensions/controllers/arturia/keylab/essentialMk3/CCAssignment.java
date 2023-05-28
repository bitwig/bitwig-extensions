package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

public enum CCAssignment {
   SAVE(40, 0x0C),
   PUNCH(41, 0x0D),//
   UNDO(42, 0x0E),//
   REDO(43, 0x0F),//
   LOOP(24, 0x10),//
   RWD(25, 0x11),//
   FFWD(26, 0x12),//
   METRO(27, 0x13),//
   STOP(20, 0x14),//
   PLAY(21, 0x15),//
   REC(22, 0x16),//
   TAP(23, 0x17),//
   PART(119, 0x07),//
   CONTEXT1(44, 0x18),//
   CONTEXT2(45, 0x19),//
   CONTEXT3(46, 0x1A),//
   CONTEXT4(47, 0x1B),//
   PAD1_A(36, 0x1C, true),//
   PAD1_B(44, 0x24, true),//
   ;

   private final int ccId;
   private final int itemId;
   private final boolean isMultiBase;

   CCAssignment(final int ccId, final int itemId, final boolean isMultiBase) {
      this.ccId = ccId;
      this.itemId = itemId;
      this.isMultiBase = isMultiBase;
   }

   CCAssignment(final int ccId, final int itemId) {
      this(ccId, itemId, false);
   }

   public int getItemId() {
      return itemId;
   }

   public int getCcId() {
      return ccId;
   }

   public boolean isMultiBase() {
      return isMultiBase;
   }
}
