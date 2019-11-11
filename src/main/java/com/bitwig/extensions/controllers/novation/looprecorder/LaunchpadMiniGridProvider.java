package com.bitwig.extensions.controllers.novation.looprecorder;

import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extension.controller.api.MidiOut;

public class LaunchpadMiniGridProvider extends GridProvider
{

   /**
    * Internal coords:
    * 0, 0: Top left
    * 7, 7: Bottom right
    */
   final private SimpleLed[][] mGridLeds = new SimpleLed[8][8];
   final private SimpleLed[] mTopLeds = new SimpleLed[8];
   final private SimpleLed[] mRightLeds = new SimpleLed[8];

   public LaunchpadMiniGridProvider(LoopRecorderExtension loopRecorder)
   {
      super(loopRecorder);

      // Matrix
      for (int x = 0; x < 8; ++x)
         for (int y = 0; y < 8; ++y)
            mGridLeds[x][y] = new SimpleLed(NOTE_ON_STATUS, matrixCoordsToNote(x, y));

      // Right
      for (int y = 0; y < 8; ++y)
         mRightLeds[y] = new SimpleLed(NOTE_ON_STATUS, matrixCoordsToNote(8, y));

      // Top
      for (int x = 0; x < 8; ++x)
         mTopLeds[x] = new SimpleLed(CC_STATUS, topIndexToCC(x));
   }

   @Override
   public void init()
   {
      mLoopRecorder.getMidiOutPort(0).sendMidi(176, 0, 0);
      mLoopRecorder.getMidiOutPort(0).sendMidi(176, 0, 40);
   }

   /**
    * Converts (x, y) coordinates into a note which the Launchpad Mini
    * associate to the corresponding pad/button.
    * <p>
    * x: 0..8
    * y: 0..7
    * <p>
    * (0, 0) -> first pad bottom left
    * (8, 0) -> button in the right, labeled as "H"
    * (8, 1) -> button in the right, labeled as "G"
    * <p>
    * And so on...
    */
   static private final int matrixCoordsToNote(int x, int y)
   {
      assert x >= 0;
      assert x <= 8;
      assert y >= 0;
      assert y <= 7;

      return x + 16 * y;
   }

   static private final int topIndexToCC(int x)
   {
      assert x >= 0;
      assert x < 8;

      return 104 + x;
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


      if (msg == MSG_CC)
      {
         final int x = data1 - 104;
         mLoopRecorder.onTopButton(x, data2 > 0);
         return true;
      }

      if (msg == MSG_NOTE_ON)
      {
         final int x = data1 & 0xf;
         final int y = data1 >> 4;
         if (x == 8)
            mLoopRecorder.onRightButton(y, data2 > 0);
         else
            mLoopRecorder.onGridButton(x, y, data2 > 0);
      }

      return false;
   }

   @Override
   public boolean handleSysexIn(String sysex)
   {
      return false;
   }

   @Override
   public void flush(final MidiOut out)
   {
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
      mMidiOut.sendMidi(176, 0, 0);
   }
}
