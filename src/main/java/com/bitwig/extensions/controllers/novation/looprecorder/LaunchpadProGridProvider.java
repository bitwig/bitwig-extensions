package com.bitwig.extensions.controllers.novation.looprecorder;

import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extension.controller.api.MidiOut;

/**
 * Check the reference manual:
 * https://d2xhy469pqj8rc.cloudfront.net/sites/default/files/novation/downloads/10598/launchpad-pro-programmers
 * -reference-guide_0.pdf
 */
public class LaunchpadProGridProvider extends GridProvider
{

   static final byte[] PROGRAMMER_MODE_SYSEX =
      new byte[] {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x10, 0x2C, 0x03, (byte) 0xF7};
   /**
    * Internal coords:
    * 0, 0: Top left
    * 7, 7: Bottom right
    */
   final private SimpleLed[][] mGridLeds = new SimpleLed[8][8];
   final private SimpleLed[] mTopLeds = new SimpleLed[8];
   final private SimpleLed[] mRightLeds = new SimpleLed[8];
   private boolean mReady = false;

   public LaunchpadProGridProvider(LoopRecorderExtension loopRecorder)
   {
      super(loopRecorder);

      // Matrix
      for (int x = 0; x < 8; ++x)
         for (int y = 0; y < 8; ++y)
            mGridLeds[x][y] = new SimpleLed(NOTE_ON_STATUS, 11 + x + 10 * y);

      // Right
      for (int y = 0; y < 8; ++y)
         mRightLeds[y] = new SimpleLed(NOTE_ON_STATUS, 19 + 10 * y);

      // Top
      for (int x = 0; x < 8; ++x)
         mTopLeds[x] = new SimpleLed(NOTE_ON_STATUS, 91 + x);
   }

   @Override
   public void init()
   {
      // Programmer Mode
      mLoopRecorder.getMidiOutPort(0).sendSysex(PROGRAMMER_MODE_SYSEX);
   }

   @Override
   public SimpleLed getTopLed(int index)
   {
      return mTopLeds[index];
   }

   @Override
   public SimpleLed getRightLed(int index)
   {
      return mRightLeds[index];
   }

   @Override
   public SimpleLed getMatrixLed(int x, int y)
   {
      return mGridLeds[x][y];
   }

   @Override
   public boolean handleMidiIn(int status, int data1, int data2)
   {

      int msg = status >> 4;

      if (msg == MSG_NOTE_ON)
      {
         final int x = (data1 % 10) - 1;
         final int y = 8 - data1 / 10;
         mLoopRecorder.onGridButton(x, y, data2 > 0);
         return true;
      }

      if (msg == MSG_CC)
      {
         if (data1 > 90)
         {
            final int x = (data1 % 10) - 1;
            mLoopRecorder.onTopButton(x, data2 > 0);
            return true;
         }

         if (data1 % 10 == 9)
         {
            final int y = 8 - data1 / 10;
            mLoopRecorder.onRightButton(y, data2 > 0);
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean handleSysexIn(String sysex)
   {
      mReady = true;
      return true;
   }

   @Override
   public void flush(MidiOut out)
   {
      if (!mReady)
         return;

      // Matrix
      for (int x = 0; x < 8; ++x)
         for (int y = 0; y < 8; ++y)
            mGridLeds[x][y].flush(out, 0);

      // Right
      for (int y = 0; y < 8; ++y)
         mRightLeds[y].flush(out, 0);

      // Top
      for (int x = 0; x < 8; ++x)
         mTopLeds[x].flush(out, 0);
   }

   @Override
   public void exit(MidiOut mMidiOut)
   {
      // TODO
   }
}
