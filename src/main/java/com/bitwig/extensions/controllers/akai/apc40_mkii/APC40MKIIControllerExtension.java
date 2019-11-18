package com.bitwig.extensions.controllers.akai.apc40_mkii;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.UserControlBank;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class APC40MKIIControllerExtension extends ControllerExtension
{
   private static final int CHANNEL_STRIP_NUM_PARAMS = 4;

   private static final int CHANNEL_STRIP_NUM_SENDS = 4;

   private enum TopMode
   {
      PAN, SENDS, USER, CHANNEL_STRIP;
   }

   private static final int MSG_NOTE_ON = 9;

   private static final int MSG_NOTE_OFF = 8;

   private static final int MSG_CC = 11;

   private static final int CC_TRACK_VOLUME = 7;

   private static final int CC_MASTER_VOLUME = 14;

   private static final int CC_AB_CROSSFADE = 15;

   private static final int CC_DEV_CTL0 = 16;

   private static final int CC_TOP_CTL0 = 48;

   private static final int CC_TEMPO = 13;

   private static final int CC_CUE = 47;

   private static final int BT_TRACK_STOP = 52;

   private static final int BT_TRACK_SELECT = 51;

   private static final int BT_TRACK_MUTE = 50;

   private static final int BT_TRACK_SOLO = 49;

   private static final int BT_TRACK_AB = 66;

   private static final int BT_TRACK_ARM = 48;

   private static final int BT_MASTER_SELECT = 80;

   private static final int BT_MASTER_STOP = 81;

   private static final int BT_SESSION = 102;

   private static final int BT_RECORD = 93;

   private static final int BT_PLAY = 91;

   private static final int BT_PAD0 = 0;

   private static final int BT_SCENE0 = 82;

   private static final int BT_SHIFT = 98;

   private static final int BT_BANK = 103;

   private static final int BT_PAN = 87;

   private static final int BT_SENDS = 88;

   private static final int BT_USER = 89;

   private static final int BT_METRONOME = 90;

   private static final int BT_TAP_TEMPO = 99;

   private static final int BT_LAUNCHER_LEFT = 97;

   private static final int BT_LAUNCHER_RIGHT = 96;

   private static final int BT_LAUNCHER_UP = 94;

   private static final int BT_LAUNCHER_DOWN = 95;

   private static final int BT_NUDGE_MINUS = 100;

   private static final int BT_NUDGE_PLUS = 101;

   private static final int BT_BANK0 = 58;

   private static final int BT_PREV_DEVICE = 58;

   private static final int BT_NEXT_DEVICE = 59;

   private static final int BT_PREV_BANK = 60;

   private static final int BT_NEXT_BANK = 61;

   private static final int BT_DEVICE_ONOFF = 62;

   private static final int BT_DEVICE_LOCK = 63;

   private static final int BT_CLIP_DEVICE_VIEW = 64;

   private static final int BT_DETAIL_VIEW = 65;

   private static final long DOUBLE_MAX_TIME = 250;

   private static final double PHYSICAL_KNOB_WIDTH = 20;


   protected APC40MKIIControllerExtension(
      final ControllerExtensionDefinition controllerExtensionDefinition,
      final ControllerHost host)
   {
      super(controllerExtensionDefinition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mApplication = host.createApplication();

      final Preferences preferences = host.getPreferences();
      mMaxVolumeSetting = preferences.getEnumSetting("Maximum Volume", "Volume Faders", FADER_VOLUME_ENUM,
         FADER_VOLUME_ENUM[1]);
      mMaxVolumeSetting.markInterested();

      mPanAsChannelStripSetting = preferences.getBooleanSetting("Replace PAN by Channel Strip",
         "Channel Strip", false);
      mPanAsChannelStripSetting.markInterested();
      if (mPanAsChannelStripSetting.get())
         mTopMode = TopMode.CHANNEL_STRIP;

      mHorizontalScrollByPageSetting = preferences.getBooleanSetting("Scroll by page (Horizontal)",
         "Clip Launcher", false);
      mHorizontalScrollByPageSetting.markInterested();

      mVerticalScrollByPageSetting = preferences.getBooleanSetting("Scroll by page (Vertical)",
         "Clip Launcher", false);
      mVerticalScrollByPageSetting.markInterested();

      mControlSendEffectSetting = preferences.getBooleanSetting("FX Control when latched", "Sends", true);
      mControlSendEffectSetting.markInterested();

      mTransport = host.createTransport();
      mTransport.isMetronomeEnabled().markInterested();
      mTransport.tempo().markInterested();
      mTransport.isPlaying().markInterested();
      mTransport.isArrangerRecordEnabled().markInterested();
      mTransport.isArrangerAutomationWriteEnabled().markInterested();
      mTransport.isClipLauncherAutomationWriteEnabled().markInterested();
      mTransport.isClipLauncherOverdubEnabled().markInterested();
      mTransport.defaultLaunchQuantization().markInterested();

      mUserControls = host.createUserControls(8 * 5);
      for (int i = 0; i < 8 * 5; ++i)
         mUserControls.getControl(i).markInterested();

      mCueConrol = host.createUserControls(1);

      mMasterTrack = host.createMasterTrack(5);
      mMasterTrack.isStopped().markInterested();
      mMasterTrack.volume().setIndication(true);

      mTrackCursor = host.createCursorTrack(8, 0);
      mTrackCursor.exists().markInterested();
      mTrackCursor.isGroup().markInterested();
      mTrackCursor.volume().markInterested();
      mTrackCursor.pan().markInterested();
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackCursor.sendBank();
         sendBank.exists().markInterested();

         final Send send = sendBank.getItemAt(i);
         send.markInterested();
         send.exists().markInterested();
      }

      mIsMasterSelected = mTrackCursor.createEqualsValue(mMasterTrack);

      mChannelStripDevice = mTrackCursor.createCursorDevice("channel-strip", "Channel Strip", 4,
         CursorDeviceFollowMode.LAST_DEVICE);
      mChannelStripDevice.exists().markInterested();
      mChannelStripRemoteControls = mChannelStripDevice.createCursorRemoteControlsPage(8);
      mChannelStripRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);

      mDeviceCursor = mTrackCursor.createCursorDevice("device-control", "Device Control", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);
      mDeviceCursor.isEnabled().markInterested();
      mDeviceCursor.isPinned().markInterested();
      mDeviceCursor.hasNext().markInterested();
      mDeviceCursor.hasPrevious().markInterested();
      mDeviceCursor.isWindowOpen().markInterested();
      mDeviceCursor.exists().markInterested();
      mRemoteControls = mDeviceCursor.createCursorRemoteControlsPage(8);
      mRemoteControls.hasNext().markInterested();
      mRemoteControls.hasPrevious().markInterested();
      mRemoteControls.selectedPageIndex().markInterested();
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 4);

      mTrackBank = host.createTrackBank(8, 5, 5, false);
      mSceneBank = mTrackBank.sceneBank();
      mSceneBank.setIndication(true);

      mTrackBank.cursorIndex().markInterested();

      for (int j = 0; j < 5; ++j)
      {
         final Scene scene = mSceneBank.getScene(j);
         scene.exists().markInterested();
         scene.color().markInterested();
         mSceneLeds[j] = new RgbLed();
      }

      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl channelStripRemoteControlsParameter = mChannelStripRemoteControls
            .getParameter(i);
         channelStripRemoteControlsParameter.markInterested();
         channelStripRemoteControlsParameter.exists().markInterested();

         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         final ClipLauncherSlotBank clipLauncher = track.clipLauncherSlotBank();
         clipLauncher.setIndication(true);
         for (int j = 0; j < 5; ++j)
         {
            final ClipLauncherSlot slot = clipLauncher.getItemAt(j);
            slot.color().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.hasContent().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.isStopQueued().markInterested();
            slot.exists().markInterested();

            sendBank.cursorIndex().markInterested();
            final Send send = sendBank.getItemAt(j);
            send.markInterested();
            send.exists().markInterested();

            mPadLeds[i][j] = new RgbLed();
         }

         track.exists().markInterested();
         track.mute().markInterested();
         track.solo().markInterested();
         track.arm().markInterested();
         track.crossFadeMode().markInterested();
         track.isStopped().markInterested();
         track.pan().markInterested();
         track.pan().exists().markInterested();
         track.volume().setIndication(true);

         mIsTrackSelected[i] = mTrackCursor.createEqualsValue(track);
         mIsTrackSelected[i].markInterested();

         mMuteLeds[i] = new Led();
         mArmLeds[i] = new Led();
         mSoloLeds[i] = new Led();
         mABLeds[i] = new Led();
         mSelectTrackLeds[i] = new Led();
         mStopTrackLeds[i] = new Led();

         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.setIndication(true);
         parameter.markInterested();
         parameter.exists().markInterested();

         mTopControlLeds[i] = new KnobLed();
         mDeviceControlLeds[i] = new KnobLed();
      }

      mSendTrackBank = host.createEffectTrackBank(5, 0);
      for (int i = 0; i < 5; ++i)
      {
         final Track sendTrack = mSendTrackBank.getItemAt(i);
         sendTrack.exists().markInterested();
      }

      mMidiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      mMidiIn.setMidiCallback(this::onMidiIn);
      mMidiIn.setSysexCallback(this::onSysexIn);

      // introduction message
      mMidiOut.sendSysex("F0 7E 7F 06 01 F7");

      createHardwareControls();
      createLayers();
   }

   private void createLayers()
   {
      // We create all the layers here because the main layer might bind actions to activate other layers.
      mLayers = new Layers(this);
      mMainLayer = new Layer(mLayers, "Main");
      mChannelStripLayer = new Layer(mLayers, "ChannelStrip");
      mShiftLayer = new Layer(mLayers, "Shift");
      mBankLayer = new Layer(mLayers, "Bank");

      createMainLayer();
      createPanLayer();
      createUserLayers();
      createDebugLayer();
      createSendLayers();
      createChannelStripLayer();
      createShiftLayer();
      createBankLayer();
   }

   private void createBankLayer()
   {
      mBankLayer.bindPressed(mPrevDeviceButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(0), () -> "Select Remote Controls Page 1"));
      mBankLayer.bindPressed(mNextDeviceButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(1), () -> "Select Remote Controls Page 2"));
      mBankLayer.bindPressed(mPrevBankButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(2), () -> "Select Remote Controls Page 3"));
      mBankLayer.bindPressed(mNextBankButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(3), () -> "Select Remote Controls Page 4"));
      mBankLayer.bindPressed(mDeviceOnOffButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(4), () -> "Select Remote Controls Page 5"));
      mBankLayer.bindPressed(mDeviceLockButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(5), () -> "Select Remote Controls Page 6"));
      mBankLayer.bindPressed(mClipDeviceViewButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(6), () -> "Select Remote Controls Page 7"));
      mBankLayer.bindPressed(mDetailViewButton, getHost().createAction(() -> mRemoteControls.selectedPageIndex().set(7), () -> "Select Remote Controls Page 8"));

      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 0, mPrevDeviceLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 1, mNextDeviceLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 2, mPrevBankLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 3, mNextBankLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 4, mDeviceOnOffLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 5, mDeviceLockLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 6, mClipDeviceViewLed);
      mBankLayer.bind(() -> mRemoteControls.selectedPageIndex().get() == 7, mDetailViewLed);
   }

   private void createShiftLayer()
   {
      mShiftLayer.bindToggle(mRecordButton, mTransport.isArrangerRecordEnabled());
      mShiftLayer.bindToggle(mSessionButton, mTransport.isArrangerAutomationWriteEnabled());
   }

   private void createChannelStripLayer()
   {
      for (int i = 0; i < 8; ++i)
         mChannelStripLayer.bind(
            mTopKnobs[i],
            i < CHANNEL_STRIP_NUM_PARAMS
               ? mChannelStripRemoteControls.getParameter(i)
               : mTrackCursor.sendBank().getItemAt(i - CHANNEL_STRIP_NUM_PARAMS));
   }

   private void createSendLayers()
   {
      mSendLayers = new Layer[5];
      for (int sendIndex = 0; sendIndex < 5; ++sendIndex)
      {
         mSendLayers[sendIndex] = new Layer(mLayers, "Send-" + sendIndex);
         for (int i = 0; i < 8; ++i)
            mSendLayers[sendIndex].bind(mTopKnobs[i], mTrackBank.getItemAt(i).sendBank().getItemAt(sendIndex));
      }
   }

   private void createUserLayers()
   {
      mUserLayers = new Layer[5];
      for (int userIndex = 0; userIndex < 5; ++userIndex)
      {
         mUserLayers[userIndex] = new Layer(mLayers, "User-" + userIndex);
         for (int i = 0; i < 8; ++i)
            mUserLayers[userIndex].bind(mTopKnobs[i], mUserControls.getControl(i + mUserIndex * 8));
      }
   }

   private void createPanLayer()
   {
      mPanLayer = new Layer(mLayers, "Pan");
      for (int i = 0; i < 8; ++i)
         mPanLayer.bind(mTopKnobs[i], mTrackBank.getItemAt(i).pan());
   }

   private void createDebugLayer()
   {
      mDebugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
      mDebugLayer.activate();
   }

   private void createMainLayer()
   {
      for (int i = 0; i < 8; ++i)
      {
         mMainLayer.bind(mDeviceControlKnobs[i], mRemoteControls.getParameter(i));
         mMainLayer.bind(mTrackVolumeSliders[i], mTrackBank.getItemAt(i).volume());
      }
      mMainLayer.bind(mMasterTrackVolumeSlider, mMasterTrack.volume());
      mMainLayer.bind(mABCrossfadeSlider, mTransport.crossfade());
      // TODO: mMainLayer.bind(mCueLevelKnob, );

      for (int x = 0; x < 8; ++x)
      {
         final Track track = mTrackBank.getItemAt(x);
         for (int y = 0; y < 5; ++y)
         {
            final int offset = 8 * y + x;
            mMainLayer.bindPressed(mGridButtons[offset], track.clipLauncherSlotBank().getItemAt(y).launchAction());
         }
         mMainLayer.bindPressed(mMuteButtons[x], track.mute().toggleAction());
         mMainLayer.bindPressed(mSoloButtons[x], track.solo().toggleAction());
         mMainLayer.bindPressed(mArmButtons[x], track.arm().toggleAction());
         mMainLayer.bindPressed(mABButtons[x], getHost().createAction(() -> {
            final SettableEnumValue crossFadeMode = track.crossFadeMode();
            final int nextValue = (crossFadeToInt(crossFadeMode.get()) + 1) % 3;
            crossFadeMode.set(intToCrossFade(nextValue));
         }, () -> "Cycle through crossfade values"));
         mMainLayer.bindPressed(
            mTrackSelectButtons[x],
            // TODO: add new api for CursorTrack.select(Track).
            getHost().createAction(() -> track.selectInMixer(), () -> "Selects the track"));
         mMainLayer.bindPressed(mTrackStopButtons[x], track.stopAction());
      }
      mMainLayer.bindPressed(mMasterTrackSelectButton, getHost()
         .createAction(() -> mMasterTrack.selectInMixer(), () -> "Selects the master track"));
      mMainLayer.bindPressed(mMasterTrackStopButton, mSceneBank.stopAction());

      mMainLayer.bindToggle(mPlayButton, mTransport.isPlaying());
      mMainLayer.bindToggle(mRecordButton, mTransport.isClipLauncherOverdubEnabled());
      mMainLayer.bindToggle(mSessionButton, mTransport.isClipLauncherAutomationWriteEnabled());
      mMainLayer.bindToggle(mMetronomeButton, mTransport.isMetronomeEnabled());
      mMainLayer.bindPressed(mTapTempoButton, mTransport.tapTempoAction());

      // TODO: not the right way...
//      final HardwareActionBindable incTempoAction =
//         getHost().createAction(() -> mTransport.tempo().incRaw(1), () -> "Increments the tempo");
//      final HardwareActionBindable decTempoAction =
//         getHost().createAction(() -> mTransport.tempo().incRaw(1), () -> "Decrements the tempo");
//      mMainLayer.bind(mTempoKnob, getHost().createRelativeHardwareControlStepTarget(incTempoAction, decTempoAction));

      mMainLayer.bindPressed(mNextDeviceButton, mDeviceCursor.selectNextAction());
      mMainLayer.bind(mDeviceCursor.hasNext(), mNextDeviceLed);

      mMainLayer.bindPressed(mPrevDeviceButton, mDeviceCursor.selectPreviousAction());
      mMainLayer.bind(mDeviceCursor.hasPrevious(), mPrevDeviceLed);

      mMainLayer.bindPressed(mNextBankButton, mRemoteControls.selectNextAction());
      mMainLayer.bind(mRemoteControls.hasNext(), mNextBankLed);

      mMainLayer.bindPressed(mPrevBankButton, mRemoteControls.selectPreviousAction());
      mMainLayer.bind(mRemoteControls.hasPrevious(), mPrevBankLed);

      mMainLayer.bindToggle(mDeviceOnOffButton, mDeviceCursor.isEnabled());
      mMainLayer.bind(mDeviceCursor.isEnabled(), mDeviceOnOffLed);

      mMainLayer.bindToggle(mDeviceLockButton, mDeviceCursor.isPinned());
      mMainLayer.bind(mDeviceCursor.isPinned(), mDeviceLockLed);

      mMainLayer.bindPressed(
         mClipDeviceViewButton,
         getHost().createAction(() -> mApplication.nextSubPanel(), () -> "Next Sub Panel"));
      mMainLayer.bind(() -> true, mClipDeviceViewLed);

      mMainLayer.bindToggle(mDetailViewButton, mDeviceCursor.isWindowOpen());
      mMainLayer.bind(mDeviceCursor.isWindowOpen(), mDetailViewLed);

      mMainLayer.bindPressed(mLauncherUpButton, () -> {
         if (mShiftButton.isPressed().get() ^ mVerticalScrollByPageSetting.get())
            mSceneBank.scrollPageBackwards();
         else
            mSceneBank.scrollBackwards();
      });
      mMainLayer.bindPressed(mLauncherDownButton, () -> {
         if (mShiftButton.isPressed().get() ^ mVerticalScrollByPageSetting.get())
            mSceneBank.scrollPageForwards();
         else
            mSceneBank.scrollForwards();
      });
      mMainLayer.bindPressed(mLauncherLeftButton, () -> {
         if (mShiftButton.isPressed().get() ^ mHorizontalScrollByPageSetting.get())
            mTrackBank.scrollPageBackwards();
         else
            mTrackBank.scrollBackwards();
      });
      mMainLayer.bindPressed(mLauncherRightButton, () -> {
         if (mShiftButton.isPressed().get() ^ mHorizontalScrollByPageSetting.get())
            mTrackBank.scrollPageForwards();
         else
            mTrackBank.scrollForwards();
      });

      for (int y = 0; y < 5; ++y)
         mMainLayer.bindPressed(mSceneButtons[y], mSceneBank.getItemAt(y).launchAction());

      mMainLayer.bindPressed(mPanButton, getHost().createAction(() -> activateTopMode(mPanAsChannelStripSetting.get()
         ? TopMode.CHANNEL_STRIP
         : TopMode.PAN), () -> "Activate Pan mode or ChannelStrip mode"));
      mMainLayer.bindPressed(mSendsButton, getHost().createAction(() -> activateTopMode(TopMode.SENDS), () -> "Activate Sends mode"));
      mMainLayer.bindPressed(mUserButton, getHost().createAction(() -> activateTopMode(TopMode.USER), () -> "Activate User mode"));

      mMainLayer.bindPressed(mShiftButton, mShiftLayer.getActivateAction());
      mMainLayer.bindReleased(mShiftButton, mShiftLayer.getDeactivateAction());

      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(420, 260);

      createTopControls();
      createDeviceControls();
      createVolumeFaders();
      createGridButtons();
      createMuteButtons();
      createSoloButtons();
      createArmButtons();
      createABButtons();
      createTrackSelectButtons();
      createTrackStopButtons();
      createTransportControls();
   }

   private void createTransportControls()
   {
      mPlayButton = mHardwareSurface.createHardwareButton("Play");
      mPlayButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PLAY));
      mPlayButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PLAY));
      mPlayLed = mHardwareSurface.createOnOffHardwareLight("PlayLed");
      mPlayLed.onUpdateHardware(() -> sendLedUpdate(BT_PLAY, mPlayLed));
      mPlayButton.setBackgroundLight(mPlayLed);

      mRecordButton = mHardwareSurface.createHardwareButton("Record");
      mRecordButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_RECORD));
      mRecordButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_RECORD));
      mRecordLed = mHardwareSurface.createOnOffHardwareLight("RecordLed");
      mRecordLed.onUpdateHardware(() -> sendLedUpdate(BT_RECORD, mRecordLed));
      mRecordButton.setBackgroundLight(mRecordLed);

      mSessionButton = mHardwareSurface.createHardwareButton("Session");
      mSessionButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SESSION));
      mSessionButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SESSION));
      mSessionLed = mHardwareSurface.createOnOffHardwareLight("SessionLed");
      mSessionLed.onUpdateHardware(() -> sendLedUpdate(BT_SESSION, mSessionLed));
      mSessionButton.setBackgroundLight(mSessionLed);

      mMetronomeButton = mHardwareSurface.createHardwareButton("Metronome");
      mMetronomeButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_METRONOME));
      mMetronomeButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_METRONOME));
      mMetronomeLed = mHardwareSurface.createOnOffHardwareLight("MetronomeLed");
      mMetronomeLed.onUpdateHardware(() -> sendLedUpdate(BT_METRONOME, mMetronomeLed));
      mMetronomeButton.setBackgroundLight(mMetronomeLed);

      mTapTempoButton = mHardwareSurface.createHardwareButton("TapTempo");
      mTapTempoButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_TAP_TEMPO));
      mTapTempoButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_TAP_TEMPO));

      mNudgePlusButton = mHardwareSurface.createHardwareButton("Nudge+");
      mNudgePlusButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NUDGE_PLUS));
      mNudgePlusButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NUDGE_PLUS));

      mNudgeMinusButton = mHardwareSurface.createHardwareButton("Nudge-");
      mNudgeMinusButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NUDGE_MINUS));
      mNudgeMinusButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NUDGE_MINUS));

      mTempoKnob = mHardwareSurface.createRelativeHardwareKnob("Tempo");
      mTempoKnob.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, CC_TEMPO));
   }

   private void createTrackStopButtons()
   {
      mTrackStopButtons = new HardwareButton[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackStop-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_STOP));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_STOP));
         mTrackStopButtons[x] = bt;
      }

      mMasterTrackStopButton = mHardwareSurface.createHardwareButton("MasterTrackStop");
      mMasterTrackStopButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_MASTER_STOP));
      mMasterTrackStopButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_MASTER_STOP));
   }

   private void createTrackSelectButtons()
   {
      mTrackSelectButtons = new HardwareButton[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackSelect-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_SELECT));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_SELECT));
         mTrackSelectButtons[x] = bt;
      }

      mMasterTrackSelectButton = mHardwareSurface.createHardwareButton("MasterTrackSelect");
      mMasterTrackSelectButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_MASTER_SELECT));
      mMasterTrackSelectButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_MASTER_SELECT));
   }

   private void createABButtons()
   {
      mABButtons = new HardwareButton[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("AB-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_AB));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_AB));
         mABButtons[x] = bt;
      }
   }

   private void createArmButtons()
   {
      mArmButtons = new HardwareButton[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Arm-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_ARM));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_ARM));
         mArmButtons[x] = bt;
      }
   }

   private void createSoloButtons()
   {
      mSoloButtons = new HardwareButton[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Solo-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_SOLO));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_SOLO));
         mSoloButtons[x] = bt;
      }
   }

   private void createMuteButtons()
   {
      mMuteButtons = new HardwareButton[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Mute-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_MUTE));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_MUTE));
         mMuteButtons[x] = bt;
      }
   }

   private void createGridButtons()
   {
      mGridButtons = new HardwareButton[8 * 5];
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 5; ++y)
         {
            final HardwareButton bt = mHardwareSurface.createHardwareButton("Grid-" + x + "-" + y);
            final int note = BT_PAD0 + (4 - y) * 8 + x;
            bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, note));
            bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, note));
            mGridButtons[y * 8 + x] = bt;
         }
      }

      mSceneButtons = new HardwareButton[5];
      for (int y = 0; y < 5; ++y)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Scene-" + y);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SCENE0 + y));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SCENE0 + y));
         mSceneButtons[y] = bt;
      }
   }

   private void createVolumeFaders()
   {
      mTrackVolumeSliders = new HardwareSlider[8];
      for (int i = 0; i < 8; ++i)
      {
         final HardwareSlider slider = mHardwareSurface.createHardwareSlider("TrackVolumeFader-" + i);
         slider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(i, CC_TRACK_VOLUME));

         mTrackVolumeSliders[i] = slider;
      }

      mMasterTrackVolumeSlider = mHardwareSurface.createHardwareSlider("MasterTrackVolumeFader");
      mMasterTrackVolumeSlider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC_MASTER_VOLUME));

      mABCrossfadeSlider = mHardwareSurface.createHardwareSlider("AB-Crossfade");
      mABCrossfadeSlider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC_AB_CROSSFADE));

      mCueLevelKnob = mHardwareSurface.createRelativeHardwareKnob("Cue-Level");
      mCueLevelKnob.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, CC_CUE));
   }

   private void createTopControls()
   {
      mTopKnobs = new AbsoluteHardwareKnob[8];
      for (int i = 0; i < 8; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("TopKnob-" + i);
         final int CC = CC_TOP_CTL0 + i;
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC));
         // TODO: need to hook the event only when the value changes from Bitwig and not from the controller
         knob.targetValue().addValueObserver(newValue -> mMidiOut.sendMidi(0xB0, CC, (int)(127 * newValue)));

         mTopKnobs[i] = knob;
      }

      mPanButton = mHardwareSurface.createHardwareButton("Pan");
      mPanButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PAN));
      mPanButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PAN));
      mPanLed = mHardwareSurface.createOnOffHardwareLight("PanLed");
      mPanLed.onUpdateHardware(() -> sendLedUpdate(BT_PAN, mPanLed));

      mSendsButton = mHardwareSurface.createHardwareButton("Sends");
      mSendsButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SENDS));
      mSendsButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SENDS));
      mSendsLed = mHardwareSurface.createOnOffHardwareLight("SendsLed");
      mSendsLed.onUpdateHardware(() -> sendLedUpdate(BT_SENDS, mSendsLed));

      mUserButton = mHardwareSurface.createHardwareButton("User");
      mUserButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_USER));
      mUserButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_USER));
      mUserLed = mHardwareSurface.createOnOffHardwareLight("UserLed");
      mUserLed.onUpdateHardware(() -> sendLedUpdate(BT_USER, mUserLed));
   }

   private void createDeviceControls()
   {
      mDeviceControlKnobs = new AbsoluteHardwareKnob[8];
      for (int i = 0; i < 8; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("DeviceControl-" + i);
         final int CC = CC_DEV_CTL0 + i;
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC));
         knob.setBounds(285 + 32 * (i % 4), 90 + 35 * (i / 4), PHYSICAL_KNOB_WIDTH, PHYSICAL_KNOB_WIDTH);
         // TODO: need to hook the event only when the value changes from Bitwig and not from the controller
         knob.targetValue().addValueObserver(newValue -> mMidiOut.sendMidi(0xB0, CC, (int)(127 * newValue)));

         mDeviceControlKnobs[i] = knob;
      }

      mPrevDeviceButton = mHardwareSurface.createHardwareButton("PrevDevice");
      mPrevDeviceButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PREV_DEVICE));
      mPrevDeviceButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PREV_DEVICE));
      mPrevDeviceLed = mHardwareSurface.createOnOffHardwareLight("PrevDeviceLed");
      mPrevDeviceLed.onUpdateHardware(() -> sendLedUpdate(BT_PREV_DEVICE, mPrevDeviceLed));

      mNextDeviceButton = mHardwareSurface.createHardwareButton("NextDevice");
      mNextDeviceButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NEXT_DEVICE));
      mNextDeviceButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NEXT_DEVICE));
      mNextDeviceLed = mHardwareSurface.createOnOffHardwareLight("NextDeviceLed");
      mNextDeviceLed.onUpdateHardware(() -> sendLedUpdate(BT_NEXT_DEVICE, mNextDeviceLed));

      mPrevBankButton = mHardwareSurface.createHardwareButton("PrevBank");
      mPrevBankButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PREV_BANK));
      mPrevBankButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PREV_BANK));
      mPrevBankLed = mHardwareSurface.createOnOffHardwareLight("PrevBankLed");
      mPrevBankLed.onUpdateHardware(() -> sendLedUpdate(BT_PREV_BANK, mPrevBankLed));

      mNextBankButton = mHardwareSurface.createHardwareButton("NextBank");
      mNextBankButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NEXT_BANK));
      mNextBankButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NEXT_BANK));
      mNextBankLed = mHardwareSurface.createOnOffHardwareLight("NextBankLed");
      mNextBankLed.onUpdateHardware(() -> sendLedUpdate(BT_NEXT_BANK, mNextBankLed));

      mDeviceOnOffButton = mHardwareSurface.createHardwareButton("DeviceOnOff");
      mDeviceOnOffButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_DEVICE_ONOFF));
      mDeviceOnOffButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_DEVICE_ONOFF));
      mDeviceOnOffLed = mHardwareSurface.createOnOffHardwareLight("DeviceOnOffLed");
      mDeviceOnOffLed.onUpdateHardware(() -> sendLedUpdate(BT_DEVICE_ONOFF, mDeviceOnOffLed));

      mDeviceLockButton = mHardwareSurface.createHardwareButton("DeviceLock");
      mDeviceLockButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_DEVICE_LOCK));
      mDeviceLockButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_DEVICE_LOCK));
      mDeviceLockLed = mHardwareSurface.createOnOffHardwareLight("DeviceLockLed");
      mDeviceLockLed.onUpdateHardware(() -> sendLedUpdate(BT_DEVICE_LOCK, mDeviceLockLed));

      mClipDeviceViewButton = mHardwareSurface.createHardwareButton("ClipDeviceView");
      mClipDeviceViewButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_CLIP_DEVICE_VIEW));
      mClipDeviceViewButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_CLIP_DEVICE_VIEW));
      mClipDeviceViewLed = mHardwareSurface.createOnOffHardwareLight("ClipDeviceViewLed");
      mClipDeviceViewLed.onUpdateHardware(() -> sendLedUpdate(BT_CLIP_DEVICE_VIEW, mClipDeviceViewLed));

      mDetailViewButton = mHardwareSurface.createHardwareButton("DetailView");
      mDetailViewButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_DETAIL_VIEW));
      mDetailViewButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_DETAIL_VIEW));
      mDetailViewLed = mHardwareSurface.createOnOffHardwareLight("DetailViewLed");
      mDetailViewLed.onUpdateHardware(() -> sendLedUpdate(BT_DETAIL_VIEW, mDetailViewLed));

      mShiftButton = mHardwareSurface.createHardwareButton("Shift");
      mShiftButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SHIFT));
      mShiftButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SHIFT));
      mShiftButton.isPressed().markInterested();

      mBankButton = mHardwareSurface.createHardwareButton("Bank");
      mBankButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_BANK));
      mBankButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_BANK));
      mBankButton.isPressed().addValueObserver((isPressed) -> {
         mBankOn.stateChanged(isPressed);
         if (mBankOn.isOn())
            mBankLayer.activate();
         else
            mBankLayer.deactivate();
      });
      mBankLed = mHardwareSurface.createOnOffHardwareLight("BankLed");
      mBankLed.onUpdateHardware(() -> sendLedUpdate(BT_BANK, mBankLed));

      mLauncherUpButton = mHardwareSurface.createHardwareButton("LauncherUp");
      mLauncherUpButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_UP));
      mLauncherUpButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_UP));

      mLauncherDownButton = mHardwareSurface.createHardwareButton("LauncherDown");
      mLauncherDownButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_DOWN));
      mLauncherDownButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_DOWN));

      mLauncherLeftButton = mHardwareSurface.createHardwareButton("LauncherLeft");
      mLauncherLeftButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_LEFT));
      mLauncherLeftButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_LEFT));

      mLauncherRightButton = mHardwareSurface.createHardwareButton("LauncherRight");
      mLauncherRightButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_RIGHT));
      mLauncherRightButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_RIGHT));
   }

   private void sendLedUpdate(final int note, final OnOffHardwareLight light)
   {
      mMidiOut.sendMidi(MSG_NOTE_ON << 4, note, light.isOn().currentValue() ? 1 : 0);
   }

   private void activateTopMode(final TopMode topMode)
   {
      mTopMode = topMode;

      for (int i = 0; i < 5; ++i)
      {
         if (topMode == TopMode.SENDS && mSendIndex == i)
            mSendLayers[i].activate();
         else
            mSendLayers[i].deactivate();

         if (topMode == TopMode.USER && mUserIndex == i)
            mUserLayers[i].activate();
         else
            mUserLayers[i].deactivate();
      }

      if (topMode == TopMode.PAN)
         mPanLayer.activate();
      else
         mPanLayer.deactivate();

      if (topMode == TopMode.CHANNEL_STRIP)
         mChannelStripLayer.activate();
      else
         mChannelStripLayer.deactivate();

      mPanLed.isOn().setValue(topMode == TopMode.PAN || topMode == TopMode.CHANNEL_STRIP);
      mSendsLed.isOn().setValue(topMode == TopMode.SENDS);
      mUserLed.isOn().setValue(topMode == TopMode.USER);

      updateTopIndications();
   }

   private void updateTopIndications()
   {
      updatePanIndication(mTopMode == TopMode.PAN);
      updateSendIndication(mTopMode == TopMode.SENDS ? mSendIndex : -1);
      updateChannelStripIndication(mTopMode == TopMode.CHANNEL_STRIP);
   }

   private void updateChannelStripIndication(boolean shouldIndicate)
   {
      for (int i = 0; i < CHANNEL_STRIP_NUM_PARAMS; ++i)
         mChannelStripRemoteControls.getParameter(i).setIndication(shouldIndicate);

      for (int i = 0; i < CHANNEL_STRIP_NUM_SENDS; ++i)
         mTrackCursor.sendBank().getItemAt(i).setIndication(shouldIndicate);
   }

   private void updatePanIndication(boolean shouldIndicate)
   {
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         track.pan().setIndication(track.exists().get() && shouldIndicate);
      }
   }

   private void updateSendIndication(final int sendIndex)
   {
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         for (int j = 0; j < 5; ++j)
         {
            final Send send = sendBank.getItemAt(j);
            final boolean shouldIndicate = sendIndex == j && send.exists().get();
            send.setIndication(shouldIndicate);
         }
      }
   }

   private void onSysexIn(final String sysex)
   {
      getHost().println("SYSEX IN: " + sysex);

      // Set the device in the proper mode (Ableton Live Mode 2)
      mMidiOut.sendSysex("F0 47 7F 29 60 00 04 41 02 01 00 F7");
   }

   private void onMidiIn(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xF;
      final int msg = status >> 4;

      getHost().println("MIDI IN, msg: " + msg + " channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      if (msg == MSG_CC)
      {
         if (data1 == CC_TEMPO)
            mTransport.tempo().incRaw((mShiftButton.isPressed().get() ? 0.1 : 1) * (data2 == 1 ? 1 : -1));
         else if (data2 == CC_CUE)
            mCueConrol.getControl(0).inc((data2 < 64 ? data2 : 127 - data2) / 127.0);
      }
      else if (msg == MSG_NOTE_ON)
      {
         if (data1 == BT_TRACK_SELECT)
         {
            if (mShiftButton.isPressed().get())
            {
               final String quantization;

               switch (channel)
               {
               case 0:
                  quantization = "none";
                  break;

               case 1:
                  quantization = "8";
                  break;

               case 2:
                  quantization = "4";
                  break;

               case 3:
                  quantization = "2";
                  break;

               case 4:
                  quantization = "1";
                  break;

               case 5:
                  quantization = "1/4";
                  break;

               case 6:
                  quantization = "1/8";
                  break;

               case 7:
                  quantization = "1/16";
                  break;

               default:
                  quantization = "1";
                  break;
               }

               mTransport.defaultLaunchQuantization().set(quantization);
            }
         }
         else if (BT_SCENE0 <= data1 && data1 < BT_SCENE0 + 5)
         {
            final int index = data1 - BT_SCENE0;
            if (mUserOn.isOn())
               mUserIndex = index;
            else if (mSendsOn.isOn())
            {
               mSendIndex = index;
               if (mControlSendEffectSetting.get())
                  mTrackCursor.selectChannel(mSendTrackBank.getItemAt(mSendIndex));
               updateSendIndication(mSendIndex);
            }
         }
         else if (data1 == BT_PAN)
         {
            mTopMode = mPanAsChannelStripSetting.get() ? TopMode.CHANNEL_STRIP : TopMode.PAN;
            updateTopIndications();
         }
         else if (data1 == BT_SENDS)
         {
            mTopMode = TopMode.SENDS;
            mUserOn.clear();
            updateTopIndications();
         }
         else if (data1 == BT_USER)
         {
            mTopMode = TopMode.USER;
            mSendsOn.clear();
            updateTopIndications();
         }
         else if (mBankButton.isPressed().get() && BT_BANK0 <= data1 && data1 < BT_BANK0 + 8)
         {
            final int index = data1 - BT_BANK0;
            mRemoteControls.selectedPageIndex().set(index);
         }
         else if (data1 == BT_NUDGE_MINUS)
            mApplication.navigateToParentTrackGroup();
         else if (data1 == BT_NUDGE_PLUS)
         {
            if (mTrackCursor.isGroup().get())
               mApplication.navigateIntoTrackGroup(mTrackCursor);
         }
      }
   }

   private String intToCrossFade(final int index)
   {
      switch (index)
      {
      case 1:
         return "A";
      case 2:
         return "B";
      default:
         return "AB";
      }
   }

   private int crossFadeToInt(final String s)
   {
      if (s.equals("A"))
         return 1;
      if (s.equals("B"))
         return 2;
      return 0;
   }

   @Override
   public void exit()
   {

   }

   @Override
   public void flush()
   {
      paintMixer();
      //paintKnobs();
      paintPads();
      paintScenes();

      mHardwareSurface.updateHardware();
   }

   private void paintScenes()
   {
      for (int i = 0; i < 5; ++i)
      {
         final RgbLed rgbLed = mSceneLeds[i];
         if (mSendsOn.isOn())
         {
            rgbLed.setColor(mSendIndex == i ? RgbLed.COLOR_PLAYING : RgbLed.COLOR_STOPPING);
            rgbLed.setBlinkType(RgbLed.BLINK_NONE);
            rgbLed.setBlinkColor(RgbLed.COLOR_NONE);
         }
         else if (mUserOn.isOn())
         {
            rgbLed.setColor(mUserIndex == i ? RgbLed.COLOR_PLAYING : RgbLed.COLOR_STOPPING);
            rgbLed.setBlinkType(RgbLed.BLINK_NONE);
            rgbLed.setBlinkColor(RgbLed.COLOR_NONE);
         }
         else
         {
            final Scene scene = mSceneBank.getScene(i);
            if (scene.exists().get())
               rgbLed.setColor(scene.color());
            else
               rgbLed.setColor(RgbLed.COLOR_NONE);
            rgbLed.setBlinkType(RgbLed.BLINK_NONE);
            rgbLed.setBlinkColor(RgbLed.COLOR_NONE);
         }

         rgbLed.paint(mMidiOut, MSG_NOTE_ON, BT_SCENE0 + i);
      }
   }

   private void paintPads()
   {
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final ClipLauncherSlotBank clipLauncherSlotBank = track.clipLauncherSlotBank();
         for (int j = 0; j < 5; ++j)
         {
            final ClipLauncherSlot slot = clipLauncherSlotBank.getItemAt(j);
            final RgbLed rgbLed = mPadLeds[i][j];

            if (slot.exists().get() && slot.hasContent().get())
               rgbLed.setColor(slot.color().red(), slot.color().green(), slot.color().blue());
            else
               rgbLed.setColor(RgbLed.COLOR_NONE);

            /*
             * if (slot.isStopQueued().get()) { rgbLed.setBlinkType(RgbLed.BLINK_STOP_QUEUED);
             * rgbLed.setBlinkColor(RgbLed.COLOR_STOPPING); } else
             */if (slot.isRecordingQueued().get())
            {
               rgbLed.setBlinkType(RgbLed.BLINK_RECORD_QUEUED);
               rgbLed.setBlinkColor(RgbLed.COLOR_RECORDING);
            }
            else if (slot.isPlaybackQueued().get())
            {
               rgbLed.setBlinkType(RgbLed.BLINK_PLAY_QUEUED);
               rgbLed.setBlinkColor(RgbLed.COLOR_PLAYING);
            }
            else if (slot.isRecording().get())
            {
               rgbLed.setBlinkType(RgbLed.BLINK_ACTIVE);
               rgbLed.setBlinkColor(RgbLed.COLOR_RECORDING);
            }
            else if (slot.isPlaying().get())
            {
               rgbLed.setBlinkType(RgbLed.BLINK_ACTIVE);
               rgbLed.setBlinkColor(RgbLed.COLOR_PLAYING);
            }
            else /* stopped */
            {
               rgbLed.setBlinkType(RgbLed.BLINK_NONE);
               rgbLed.setBlinkColor(RgbLed.COLOR_NONE);
            }

            rgbLed.paint(mMidiOut, MSG_NOTE_ON, BT_PAD0 + i + (4 - j) * 8);
         }
      }
   }

   private void paintKnobs()
   {
      for (int i = 0; i < 8; ++i)
      {
         {
            final RemoteControl parameter = mRemoteControls.getParameter(i);
            final double value = parameter.get();
            final boolean exists = parameter.exists().get();

            if (exists)
            {
               assert value >= 0;
               assert value <= 1;

               mDeviceControlLeds[i].set((int)(value * 127));
               mDeviceControlLeds[i].setRing(KnobLed.RING_SINGLE);
            }
            else
               mDeviceControlLeds[i].setRing(KnobLed.RING_OFF);
            mDeviceControlLeds[i].paint(mMidiOut, MSG_CC, 0, CC_DEV_CTL0 + i);
         }

         {
            boolean exists = false;
            double value = 0;
            int ring = KnobLed.RING_OFF;

            final Track track = mTrackBank.getItemAt(i);
            switch (mTopMode)
            {
            case SENDS:
            {
               final Send send = track.sendBank().getItemAt(mSendIndex);
               exists = send.exists().get();
               value = send.get();
               ring = KnobLed.RING_VOLUME;
               break;
            }

            case PAN:
            {
               value = track.pan().get();
               exists = track.exists().get();
               ring = KnobLed.RING_PAN;
               break;
            }

            case USER:
            {
               value = mUserControls.getControl(i + 8 * mUserIndex).get();
               exists = true;
               ring = KnobLed.RING_SINGLE;
               break;
            }

            case CHANNEL_STRIP:
            {
               if (i < CHANNEL_STRIP_NUM_PARAMS)
               {
                  final RemoteControl parameter = mChannelStripRemoteControls.getParameter(i);
                  if (parameter.exists().get())
                  {
                     ring = KnobLed.RING_SINGLE;
                     value = parameter.get();
                     exists = true;
                  }
               }
               else
               {
                  final Send send = mTrackCursor.sendBank().getItemAt(i - CHANNEL_STRIP_NUM_PARAMS);
                  if (send.exists().get())
                  {
                     ring = KnobLed.RING_VOLUME;
                     value = send.get();
                     exists = true;
                  }
                  break;
               }
            }
            }

            assert value >= 0;
            assert value <= 1;

            mTopControlLeds[i].set((int)(value * 127));
            mTopControlLeds[i].setRing(exists ? ring : KnobLed.RING_OFF);
            mTopControlLeds[i].paint(mMidiOut, MSG_CC, 0, CC_TOP_CTL0 + i);
         }
      }
   }

   private int computeLaunchQuantizationIndex()
   {
      switch (mTransport.defaultLaunchQuantization().get())
      {
      case "none":
         return 0;
      case "8":
         return 1;
      case "4":
         return 2;
      case "2":
         return 3;
      case "1":
         return 4;
      case "1/4":
         return 5;
      case "1/8":
         return 6;
      case "1/16":
         return 7;
      default:
         return -1;
      }
   }

   private void paintMixer()
   {
      final int launchQuantizationIndex = computeLaunchQuantizationIndex();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final boolean exists = track.exists().get();

         mMuteLeds[i].set(exists && !track.mute().get() ? 1 : 0);
         mMuteLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_MUTE);

         mArmLeds[i].set(exists && track.arm().get() ? 1 : 0);
         mArmLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_ARM);

         mSoloLeds[i].set(exists && track.solo().get() ? 1 : 0);
         mSoloLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_SOLO);

         mABLeds[i].set(exists ? crossFadeToInt(track.crossFadeMode().get()) : 0);
         mABLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_AB);

         if (mShiftButton.isPressed().get())
         {
            // Show the launch quantization
            mSelectTrackLeds[i].set(launchQuantizationIndex == i ? 1 : 0);
            mSelectTrackLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_SELECT);
         }
         else
         {
            // Show the selected track.
            mSelectTrackLeds[i].set(exists && mIsTrackSelected[i].get() ? 1 : 0);
            mSelectTrackLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_SELECT);
         }

         mStopTrackLeds[i].set(exists && !track.isStopped().get() ? 1 : 0);
         mStopTrackLeds[i].paint(mMidiOut, MSG_NOTE_ON, i, BT_TRACK_STOP);
      }

      mStopMasterTrackLed.set(mMasterTrack.isStopped().get() ? 0 : 1);
      mStopMasterTrackLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_MASTER_STOP);

      mSelectMasterTrackLed.set(mIsMasterSelected.get() ? 1 : 0);
      mSelectMasterTrackLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_MASTER_SELECT);
   }

   private double getMaxVolume()
   {
      final String text = mMaxVolumeSetting.get();
      if (text.equals(FADER_VOLUME_ENUM[0]))
         return 7.937005259840999e-01;
      return 1.0;
   }

   private void setVolume(final Parameter volume, int midiVal)
   {
      volume.set(midiVal * getMaxVolume() / 127.0);
   }

   private Application mApplication = null;

   private Transport mTransport = null;

   private MasterTrack mMasterTrack = null;

   private BooleanValue mIsMasterSelected = null;

   private BooleanValue[] mIsTrackSelected = new BooleanValue[8];

   private TrackBank mTrackBank = null;

   private TrackBank mSendTrackBank = null;

   private SceneBank mSceneBank = null;

   private CursorTrack mTrackCursor = null;

   private PinnableCursorDevice mDeviceCursor = null;

   private PinnableCursorDevice mChannelStripDevice;

   private CursorRemoteControlsPage mRemoteControls = null;

   private CursorRemoteControlsPage mChannelStripRemoteControls;

   private SettableEnumValue mMaxVolumeSetting;

   private SettableBooleanValue mPanAsChannelStripSetting;

   private SettableBooleanValue mHorizontalScrollByPageSetting;

   private SettableBooleanValue mVerticalScrollByPageSetting;


   private MidiIn mMidiIn = null;
   private MidiOut mMidiOut = null;

   private UserControlBank mUserControls = null;

   private UserControlBank mCueConrol = null;

   /**
    * Helper class that will stay as pressed if there is a "double press" (like a double click).
    */
   private class DoublePressedButtonState
   {
      void stateChanged(boolean isPressed)
      {
         if (!isPressed)
         {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - mLastPressTime >= DOUBLE_MAX_TIME && !mShiftButton.isPressed().get())
               mIsOn = false;
            mLastPressTime = currentTimeMillis;
         }
         else
         {
            mIsOn = true;
         }
      }

      boolean isOn()
      {
         return mIsOn;
      }

      void clear()
      {
         mIsOn = false;
      }

      private boolean mIsOn = false;
      private long mLastPressTime = 0;
   }

   private final DoublePressedButtonState mBankOn = new DoublePressedButtonState();
   private final DoublePressedButtonState mUserOn = new DoublePressedButtonState();
   private final DoublePressedButtonState mSendsOn = new DoublePressedButtonState();

   private TopMode mTopMode = TopMode.PAN;

   private int mSendIndex = 0; // 0..4

   private int mUserIndex = 0; // 0..4

   private final Led[] mMuteLeds = new Led[8];

   private final Led[] mSoloLeds = new Led[8];

   private final Led[] mABLeds = new Led[8];

   private final Led[] mArmLeds = new Led[8];

   private final Led[] mSelectTrackLeds = new Led[8];

   private final Led mSelectMasterTrackLed = new Led();

   private final Led[] mStopTrackLeds = new Led[8];

   private final Led mStopMasterTrackLed = new Led();

   private final KnobLed[] mDeviceControlLeds = new KnobLed[8];

   private final KnobLed[] mTopControlLeds = new KnobLed[8];

   private final RgbLed[][] mPadLeds = new RgbLed[8][5];

   private final RgbLed[] mSceneLeds = new RgbLed[5];

   private final static String[] FADER_VOLUME_ENUM = new String[] { "0dB", "+6dB" };

   private final static String[] ON_OFF_ENUM = new String[] { "On", "Off" };

   private SettableBooleanValue mControlSendEffectSetting;

   private HardwareSurface mHardwareSurface;

   private Layers mLayers;
   private Layer mMainLayer;
   private Layer mDebugLayer;
   private Layer mPanLayer;
   private Layer[] mUserLayers;
   private Layer[] mSendLayers;
   private Layer mChannelStripLayer;
   private Layer mShiftLayer;
   private Layer mBankLayer;

   private AbsoluteHardwareKnob[] mTopKnobs;
   private AbsoluteHardwareKnob[] mDeviceControlKnobs;
   private HardwareSlider[] mTrackVolumeSliders;
   private HardwareSlider mMasterTrackVolumeSlider;
   private HardwareSlider mABCrossfadeSlider;
   private RelativeHardwareKnob mCueLevelKnob;
   private HardwareButton[] mGridButtons;
   private HardwareButton[] mMuteButtons;
   private HardwareButton[] mSoloButtons;
   private HardwareButton[] mArmButtons;
   private HardwareButton[] mABButtons;
   private HardwareButton[] mTrackSelectButtons;
   private HardwareButton mMasterTrackSelectButton;
   private HardwareButton[] mTrackStopButtons;
   private HardwareButton mMasterTrackStopButton;
   private HardwareButton mPlayButton;
   private HardwareButton mRecordButton;
   private HardwareButton mSessionButton;
   private HardwareButton mMetronomeButton;
   private HardwareButton mTapTempoButton;
   private HardwareButton mNudgePlusButton;
   private HardwareButton mNudgeMinusButton;
   private RelativeHardwareKnob mTempoKnob;
   private HardwareButton mPrevDeviceButton;
   private HardwareButton mNextDeviceButton;
   private HardwareButton mPrevBankButton;
   private HardwareButton mNextBankButton;
   private HardwareButton mDeviceOnOffButton;
   private HardwareButton mDeviceLockButton;
   private HardwareButton mClipDeviceViewButton;
   private HardwareButton mDetailViewButton;
   private HardwareButton mShiftButton;
   private HardwareButton mBankButton;
   private HardwareButton mLauncherUpButton;
   private HardwareButton mLauncherDownButton;
   private HardwareButton mLauncherLeftButton;
   private HardwareButton mLauncherRightButton;
   private HardwareButton[] mSceneButtons;
   private HardwareButton mPanButton;
   private HardwareButton mSendsButton;
   private HardwareButton mUserButton;
   private OnOffHardwareLight mPanLed;
   private OnOffHardwareLight mSendsLed;
   private OnOffHardwareLight mUserLed;
   private OnOffHardwareLight mMetronomeLed;
   private OnOffHardwareLight mPlayLed;
   private OnOffHardwareLight mRecordLed;
   private OnOffHardwareLight mSessionLed;
   private OnOffHardwareLight mPrevDeviceLed;
   private OnOffHardwareLight mNextDeviceLed;
   private OnOffHardwareLight mPrevBankLed;
   private OnOffHardwareLight mNextBankLed;
   private OnOffHardwareLight mDeviceOnOffLed;
   private OnOffHardwareLight mDeviceLockLed;
   private OnOffHardwareLight mClipDeviceViewLed;
   private OnOffHardwareLight mDetailViewLed;
   private OnOffHardwareLight mBankLed;
}
