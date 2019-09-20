package com.bitwig.extensions.controllers.presonus;

public interface MidiReceiver
{
   void onMidi(final int status, final int data1, final int data2);
}
