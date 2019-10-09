package com.bitwig.extensions.controllers.novation.looprecorder;

import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class LoopRecorderExtension extends ControllerExtension
{
   static final int ledOff = 12;
   static final int ledRedLow = 13;
   static final int ledRed = 7;
   static final int ledAmberLow = 29;
   static final int ledAmber = 63;
   static final int ledYellow = 62;
   static final int ledGreenLow = 28;
   static final int ledGreen = 60;

   static final int ledRedFlash = 11;
   static final int ledAmberFlash = 59;
   static final int ledYellowFlash = 58;
   static final int ledGreenFlash = 56;

   private static final int NUM_TRACKS = 4; // Group track + 4 childs
   private static final int NUM_SCENES = 8;

   /* List of supported launchpad variants */
   private static final String LAUNCHPAD_MINI = "Launchpad S/Mini";
   private static final String LAUNCHPAD_PRO = "Launchpad Pro";

   /* Abstraction to the launchpad variants */
   private GridProvider mGrid = null;

   /* Host objects */
   private ControllerHost mHost = null;
   private Application mApplication = null;
   private Transport mTransport = null;
   private Preferences mHostPreferences = null;
   private SettableEnumValue mControllerModelSetting = null;

   /* Track and Scene banks banks */
   private TrackBank mGroupsTrackBank = null;
   private TrackBank[] mRecTrackBanks = new TrackBank[2];
   private TrackBank[] mPlayTrackBanks = new TrackBank[2];

   /* Midi stuff */
   private MidiIn mMidiIn = null;
   private MidiOut mMidiOut = null;

   /* State variables */
   private RecordingContext[] mRecordingContexts = new RecordingContext[] {
      new RecordingContext(), new RecordingContext()
   };

   /* Tempo nudging */
   private double mTempo;
   private boolean mIsNudgingTempo = false;

   protected LoopRecorderExtension(
      final LoopRecorderExtensionDefinition loopRecorderExtensionDefinition, final ControllerHost host)
   {
      super(loopRecorderExtensionDefinition, host);

      mGrid = new LaunchpadMiniGridProvider(this);
   }

   @Override
   public void init()
   {
      mHost = getHost();

      mMidiIn = mHost.getMidiInPort(0);
      mMidiOut = mHost.getMidiOutPort(0);

      mMidiIn.setMidiCallback(this::onMidiIn);
      mMidiIn.setSysexCallback(this::onSysexIn);

      mApplication = mHost.createApplication();

      mHostPreferences = mHost.getPreferences();
      mControllerModelSetting = mHostPreferences.getEnumSetting("Model",
         "Controller",
         new String[] {LAUNCHPAD_MINI, LAUNCHPAD_PRO},
         LAUNCHPAD_MINI);
      mControllerModelSetting.markInterested();
      mControllerModelSetting.addValueObserver(this::controllerModelChanged);
      controllerModelChanged(mControllerModelSetting.get());

      mTransport = mHost.createTransport();

      mTransport.isMetronomeEnabled().markInterested();
      mTransport.tempo().markInterested();
      mTransport.isPlaying().markInterested();
      mTransport.getPosition().markInterested();

      mTransport.getPosition().addValueObserver(this::checkPosition);

      mGroupsTrackBank = mHost.createTrackBank(4, 0, 8, false);

      mRecTrackBanks[0] = initTrackBank(0);
      mRecTrackBanks[1] = initTrackBank(1);
      mPlayTrackBanks[0] = initTrackBank(2);
      mPlayTrackBanks[1] = initTrackBank(3);

      mHost.requestFlush();

      mGrid.init();

      mHost.showPopupNotification("LoopRecorder Initialized");
   }

   private void controllerModelChanged(String s)
   {
      if (mGrid != null)
      {
         mGrid.exit(mMidiOut);
         mGrid = null;
      }

      switch (s)
      {
         case LAUNCHPAD_MINI:
            mGrid = new LaunchpadMiniGridProvider(this);
            break;

         case LAUNCHPAD_PRO:
            mGrid = new LaunchpadProGridProvider(this);
            break;

         default:
            mGrid = new LaunchpadMiniGridProvider(this);
            mControllerModelSetting.set(LAUNCHPAD_MINI);
            break;
      }

      mGrid.init();
   }

   private TrackBank initTrackBank(final int pos)
   {
      Track parentTrack = mGroupsTrackBank.getItemAt(pos);
      final TrackBank trackBank = parentTrack.createTrackBank(NUM_TRACKS, 0, NUM_SCENES, false);

      trackBank.scrollPosition().markInterested();
      trackBank.scrollPosition().set(1 + pos * 5);

      final SceneBank sceneBank = trackBank.sceneBank();
      for (int i = 0; i < NUM_SCENES; ++i)
      {
         final Scene scene = sceneBank.getScene(i);
         scene.exists().markInterested();
      }

      for (int i = 0; i < NUM_TRACKS; ++i)
      {
         Track track = trackBank.getItemAt(i);
         track.exists().markInterested();
         track.isQueuedForStop().markInterested();
         track.isStopped().markInterested();
         track.position().markInterested();

         ClipLauncherSlotBank clipLauncherSlotBank = track.clipLauncherSlotBank();
         clipLauncherSlotBank.setIndication(true);
         for (int j = 0; j < NUM_SCENES; ++j)
         {
            ClipLauncherSlot slot = clipLauncherSlotBank.getItemAt(j);
            slot.hasContent().markInterested();
            slot.exists().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.isStopQueued().markInterested();
         }
      }

      return trackBank;
   }

   @Override
   public void exit()
   {
      mGrid.exit(mMidiOut);
      mHost.showPopupNotification("LoopRecorder Exited");
   }

   @Override
   public void flush()
   {
      paintBarFeedback();
      paintSideButtons();
      paintPlayButtons();
      paintRecordButtons();

      mGrid.flush(mMidiOut);
   }

   private void paintBarFeedback()
   {
      final boolean isPlaying = mTransport.isPlaying().get();
      final double beatTime = mTransport.getPosition().get();
      final int currentBar = ((int) Math.floor(beatTime / 4.0)) & 0xF;

      for (int i = 0; i < 8; ++i)
      {
         int color = ledYellow;

         if (isPlaying && currentBar == i)
            color = ledGreen;
         if (isPlaying && currentBar == 8 + i)
            color = ledRed;

         getTopLed(i).setColor(color);
      }
   }

   private void paintSideButtons()
   {
      for (int i = 0; i < 5; ++i)
         getRightLed(i).setColor(mRecordingContexts[1].mRecLengthBars == 1 << i ? ledRed : ledGreen);
      getTempoNudgeDecLed().setColor(ledAmber);
      getTempoNudgeIncLed().setColor(ledAmber);
      getRecPrepareLed().setColor(ledRed);
   }

   private void paintPlayButtons()
   {
      for (int i = 0; i < 8; ++i)
      {
         for (int group = 0; group < 2; ++group)
         {
            boolean isPlaying = isPlayingScene(group, i);
            boolean hasContent = hasPlayContent(group, i);
            getMatrixLed(4 + group * 2, i).setColor(isPlaying ? ledGreenFlash : hasContent ? ledGreen : ledGreenLow);
         }
      }

      // Delete play clips
      getMatrixLed(5, 1).setColor(ledYellow);
      getMatrixLed(7, 1).setColor(ledYellow);

      // Stop play
      getMatrixLed(5, 7).setColor(ledAmber);
      getMatrixLed(7, 7).setColor(ledAmber);
   }

   private void paintRecordButtons()
   {
      for (int i = 0; i < 8; ++i)
      {
         for (int group = 0; group < 2; ++group)
         {
            boolean isRecording = isRecordingScene(group, i);
            boolean hasContent = hasRecordContent(group, i);
            getMatrixLed(group * 2, i).setColor(isRecording ? ledRedFlash : hasContent ? ledRed : ledOff);
         }
      }

      // Rec Save
      getMatrixLed(1, 0).setColor(ledGreen);
      getMatrixLed(3, 0).setColor(ledGreen);

      // Rec Delete
      getMatrixLed(1, 1).setColor(ledYellow);
      getMatrixLed(3, 1).setColor(ledYellow);

      // Rec Start
      getMatrixLed(1, 6).setColor(ledAmber);
      getMatrixLed(3, 6).setColor(ledAmber);

      // Rec Stop
      getMatrixLed(1, 7).setColor(ledAmber);
      getMatrixLed(3, 7).setColor(ledAmber);
   }

   private final SimpleLed getTopLed(int x)
   {
      return mGrid.getTopLed(x);
   }

   private final SimpleLed getRightLed(int y)
   {
      return mGrid.getRightLed(y);
   }

   private final SimpleLed getTempoNudgeDecLed()
   {
      return getRightLed(6);
   }

   private final SimpleLed getTempoNudgeIncLed()
   {
      return getRightLed(5);
   }

   private final SimpleLed getRecPrepareLed()
   {
      return mGrid.getRightLed(7);
   }

   private boolean isPlayingScene(final int group, final int sceneIndex)
   {
      return mPlayTrackBanks[group].getItemAt(0).clipLauncherSlotBank().getItemAt(sceneIndex).isPlaying().get();
   }

   private boolean hasPlayContent(final int group, final int sceneIndex)
   {
      return mPlayTrackBanks[group].getItemAt(0).clipLauncherSlotBank().getItemAt(sceneIndex).hasContent().get();
   }

   private final SimpleLed getMatrixLed(int x, int y)
   {
      return mGrid.getMatrixLed(x, y);
   }

   private boolean isRecordingScene(final int group, final int sceneIndex)
   {
      return mRecTrackBanks[group].getItemAt(0).clipLauncherSlotBank().getItemAt(sceneIndex).isRecording().get();
   }

   private boolean hasRecordContent(final int group, final int sceneIndex)
   {
      return mRecTrackBanks[group].getItemAt(0).clipLauncherSlotBank().getItemAt(sceneIndex).hasContent().get();
   }

   private void onSysexIn(final String sysex)
   {
      //mHost.println("onSysexIn: '" + sysex + "', " + sysex.length());
      mGrid.handleSysexIn(sysex);
   }

   private void onMidiIn(final int status, final int data1, final int data2)
   {
      if (false)
      {
         int channel = status & 0xF;
         int msg = status >> 4;

         mHost.println("MIDI IN, msg: " + msg + " channel: " + channel + ", data1: " + data1 + ", data2: " + data2);
      }

      mGrid.handleMidiIn(status, data1, data2);
   }

   public void onTopButton(final int x, final boolean isPressed)
   {
      /* Nothing to do here */
   }

   public void onRightButton(final int y, final boolean isPressed)
   {
      if (0 <= y && y <= 4 && isPressed)
         setRecordLength(1 << y);
      else if (y == 7 && isPressed)
         prepeareRecordGroup();
      else if (!isPressed && (y == 5 || y == 6))
         tempoNudgeOff();
      else if (y == 5)
         tempoNudgeInc();
      else if (y == 6)
         tempoNudgeDec();
   }

   private void setRecordLength(int numBars)
   {
      mRecordingContexts[1].setRecordLengthInBars(numBars);
   }

   private void prepeareRecordGroup()
   {
      for (int group = 0; group < 2; ++group)
      {
         for (int i = 0; i < NUM_TRACKS; ++i)
         {
            final Track track = mRecTrackBanks[group].getItemAt(i);
            track.getAutoMonitor().set(false);
            track.getMonitor().set(false);
            track.getArm().set(true);
         }
      }
   }

   private void tempoNudgeOff()
   {
      if (mIsNudgingTempo)
      {
         mTransport.tempo().set(mTempo);
         mIsNudgingTempo = false;
      }

   }

   private void tempoNudgeInc()
   {
      mIsNudgingTempo = true;
      mTempo = mTransport.tempo().get();
      mTransport.tempo().set(mTempo + 0.001);
   }

   private void tempoNudgeDec()
   {
      mIsNudgingTempo = true;
      mTempo = mTransport.tempo().get();
      mTransport.tempo().set(mTempo - 0.001);
   }

   public void onGridButton(final int x, final int y, final boolean isPressed)
   {
      if (!isPressed)
         return;

      if (x == 0)
         copyScene(0, y);
      else if (x == 2)
         copyScene(1, y);
      else if (x == 4)
         playScene(0, y);
      else if (x == 6)
         playScene(1, y);
      else if (x == 1 && y == 0)
         copyEveryScenes(0);
      else if (x == 1 && y == 1)
         deleteRecordClips(0);
      else if (x == 1 && y == 6)
         startToRecord(0);
      else if (x == 1 && y == 7)
         stopRecording(0);
      else if (x == 3 && y == 0)
         copyEveryScenes(1);
      else if (x == 3 && y == 1)
         deleteRecordClips(1);
      else if (x == 3 && y == 6)
         startToRecord(1);
      else if (x == 3 && y == 7)
         stopRecording(1);
      else if (x == 5 && y == 1)
         deletePlayClips(0);
      else if (x == 5 && y == 7)
         stopPlaying(0);
      else if (x == 7 && y == 1)
         deletePlayClips(1);
      else if (x == 7 && y == 7)
         stopPlaying(1);
   }

   private void copyScene(int group, int y)
   {
      Scene source = mRecTrackBanks[group].sceneBank().getScene(y);
      Scene dest = mPlayTrackBanks[group].sceneBank().getScene(y);
      dest.copyFrom(source);
   }

   private void playScene(int group, int sceneIndex)
   {
      mPlayTrackBanks[group].sceneBank().getScene(sceneIndex).launch();
   }

   private void copyEveryScenes(int group)
   {
      deletePlayClips(group);

      for (int i = 0; i < NUM_SCENES; ++i)
         copyScene(group, i);
   }

   private void deleteRecordClips(int group)
   {
      stopRecording(group);

      deleteClipsInTrackBank(mRecTrackBanks[group]);
   }

   private void startToRecord(int group)
   {
      deleteRecordClips(group);
      mRecordingContexts[group].setRecording();
      setNextRecMark(group, (int) mTransport.getPosition().get());
      mRecTrackBanks[group].sceneBank().getScene(0).launch();

      mHost.showPopupNotification("REC" + group + " START: " + mRecordingContexts[group].mRecStartBeats + " WILL STOP: "
         + mRecordingContexts[group].mRecStopBeats);
   }

   private void stopRecording(int group)
   {
      mRecordingContexts[group].mContinueRecording = false;
   }

   private void deleteClipsInTrackBank(TrackBank trackBank)
   {
      for (int i = 0; i < NUM_TRACKS; ++i)
      {
         final Track channel = trackBank.getChannel(i);
         if (channel == null || !channel.exists().get())
            continue;

         final ClipLauncherSlotBank clipLauncher = channel.clipLauncherSlotBank();
         if (clipLauncher == null)
            continue;

         for (int j = 0; j < NUM_SCENES; ++j)
            clipLauncher.deleteClip(j);
      }
   }

   private void deletePlayClips(int group)
   {
      deleteClipsInTrackBank(mPlayTrackBanks[group]);
   }

   private void stopPlaying(int group)
   {
      mGroupsTrackBank.getItemAt(2 + group).stop();
   }

   public void checkPosition(double position)
   {
      checkPosition(0, position);
      checkPosition(1, position);
   }

   public void checkPosition(int group, double position)
   {
      if (mRecordingContexts[group].mIsRecording)
      {
         int beats = (int) position;
         if (beats >= mRecordingContexts[group].mRecStopBeats)
         {
            if (mRecordingContexts[group].mContinueRecording)
            {
               mRecordingContexts[group].mRecordingSceneIndex =
                  (mRecordingContexts[group].mRecordingSceneIndex + 1) % NUM_SCENES;

               clearRecScene(group, mRecordingContexts[group].mRecordingSceneIndex);
               mRecTrackBanks[group].sceneBank().getScene(mRecordingContexts[group].mRecordingSceneIndex).launch();

               setNextRecMark(group, beats);

               mHost.showPopupNotification("NEXT SCENE REC: " + beats);
            }
            else
            {
               setNextRecMark(group, beats);
               mGroupsTrackBank.getItemAt(group).stop();

               mRecordingContexts[group].mIsRecording = false;
               mHost.showPopupNotification("STOPPED REC: " + beats);
            }
         }
      }
   }

   private void clearRecScene(int group, int index)
   {
      for (int i = 0; i < NUM_TRACKS + 1; ++i)
      {
         Track channel = mRecTrackBanks[group].getChannel(i);
         if (channel == null)
            continue;

         channel.clipLauncherSlotBank().deleteClip(index);
      }
   }

   public void setNextRecMark(int group, int beats)
   {
      mRecordingContexts[group].mRecStartBeats = beats;
      mRecordingContexts[group].mRecStopBeats =
         mRecordingContexts[group].mRecStartBeats + mRecordingContexts[group].mRecLengthBeats;
   }
}
