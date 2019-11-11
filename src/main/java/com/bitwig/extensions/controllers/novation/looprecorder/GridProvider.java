package com.bitwig.extensions.controllers.novation.looprecorder;

import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extension.controller.api.MidiOut;

public abstract class GridProvider
{

   static final int NOTE_ON_STATUS = 144;
   static final int NOTE_OFF_STATUS = 128;
   static final int CC_STATUS = 176;

   static final int MSG_NOTE_ON = 9;
   static final int MSG_NOTE_OFF = 8;
   static final int MSG_CC = 11;
   protected final LoopRecorderExtension mLoopRecorder;

   public GridProvider(final LoopRecorderExtension loopRecorder)
   {
      mLoopRecorder = loopRecorder;
   }

   /**
    * Sends initial data to the controller to put it in the init state.
    */
   public abstract void init();

   /**
    * 0 is for button labeled as "1"
    * 7 is for button labeled as "8"
    */
   public abstract SimpleLed getTopLed(final int index);

   /**
    * 0 is for button labeled as "A"
    * 7 is for button labeled as "H"
    */
   public abstract SimpleLed getRightLed(final int index);

   /**
    * Return the led corresponding of the intersection of the led
    * returned by getTopLed(x) and getRightLed(y).
    */
   public abstract SimpleLed getMatrixLed(final int x, final int y);

   /**
    * Returns true if it could handle the midi in.
    */
   public abstract boolean handleMidiIn(int status, int data1, int data2);

   public abstract boolean handleSysexIn(final String sysex);

   /**
    * Flushes the leds
    */
   public abstract void flush(final MidiOut out);

   public abstract void exit(MidiOut mMidiOut);
}
