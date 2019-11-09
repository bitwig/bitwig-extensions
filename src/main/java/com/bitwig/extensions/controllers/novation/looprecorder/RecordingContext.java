package com.bitwig.extensions.controllers.novation.looprecorder;

public class RecordingContext
{
   int mRecStartBeats = 0;
   int mRecLengthBars = 2;
   int mRecLengthBeats = mRecLengthBars * 4;
   int mRecStopBeats = mRecStartBeats + mRecLengthBeats;
   boolean mIsRecording = false;
   boolean mContinueRecording = true;
   int mRecordingSceneIndex = 0;

   public void setRecordLengthInBars(int length)
   {
      mRecLengthBars = length;
      mRecLengthBeats = length * 4;
   }

   public void setRecording()
   {
      mIsRecording = true;
      mContinueRecording = true;
      mRecordingSceneIndex = 0;
   }
}
