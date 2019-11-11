package com.bitwig.extensions.controllers.arturia.keylab.mk2;

public enum DAWMode
{
   MCU(0x0),
   HUI(0x41),
   Live(0x02),
   Logic(0x03),
   PT(0x44),
   Cubase(0x05),
   StudioOne(0x06),
   Reaper(0x08),
   MMC(0x0A);

   DAWMode(final int ID)
   {
      mID = ID;
   }

   public int getID()
   {
      return mID;
   }

   private final int mID;
}
