package com.bitwig.extensions.controllers.akai.apc40_mkii;

import com.bitwig.extension.api.Color;
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
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativePosition;
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
   private static final boolean ENABLE_DEBUG_LAYER = false;

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

   private static final int BT_GRID0 = 0;

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

   private static final int BT_PREV_DEVICE = BT_BANK0;

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
      mProject = host.getProject();
      mRootTrackGroup = mProject.getRootTrackGroup();

      createSettingsObjects(host);
      createTransportObject(host);

      mUserControls = host.createUserControls(8 * 5);
      for (int i = 0; i < 8 * 5; ++i)
         mUserControls.getControl(i).markInterested();

      mMasterTrack = host.createMasterTrack(5);
      mMasterTrack.isStopped().markInterested();

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
      mTrackBank.setSkipDisabledItems(true);
      mTrackBank.setShouldShowClipLauncherFeedback(true);

      mSceneBank = mTrackBank.sceneBank();
      mSceneBank.setIndication(true);

      mTrackBank.cursorIndex().markInterested();

      for (int j = 0; j < 5; ++j)
      {
         final Scene scene = mSceneBank.getScene(j);
         scene.exists().markInterested();
         scene.color().markInterested();
      }

      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl channelStripRemoteControlsParameter = mChannelStripRemoteControls
            .getParameter(i);
         channelStripRemoteControlsParameter.markInterested();
         channelStripRemoteControlsParameter.exists().markInterested();

         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         sendBank.setSkipDisabledItems(true);

         final ClipLauncherSlotBank clipLauncher = track.clipLauncherSlotBank();
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
         }

         track.exists().markInterested();
         track.mute().markInterested();
         track.solo().markInterested();
         track.arm().markInterested();
         track.crossFadeMode().markInterested();
         track.isStopped().markInterested();
         track.pan().markInterested();
         track.pan().exists().markInterested();

         mIsTrackSelected[i] = mTrackCursor.createEqualsValue(track);
         mIsTrackSelected[i].markInterested();

         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.markInterested();
         parameter.exists().markInterested();

         mTopControlKnobLeds[i] = new KnobLed();
         mDeviceControlKnobLeds[i] = new KnobLed();
      }

      mSendTrackBank = host.createEffectTrackBank(5, 0);
      mSendTrackBank.setSkipDisabledItems(true);
      for (int i = 0; i < 5; ++i)
      {
         final Track sendTrack = mSendTrackBank.getItemAt(i);
         sendTrack.exists().markInterested();
      }

      createMidiObjects(host);
      createHardwareControls();
      createLayers();

      host.scheduleTask(this::postInit, 0);
   }

   private void createMidiObjects(final ControllerHost host)
   {
      mMidiIn = host.getMidiInPort(0);
      mMidiIn.setSysexCallback(this::onSysexIn);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex("F0 7E 7F 06 01 F7"); // send introduction message
   }

   private void createTransportObject(final ControllerHost host)
   {
      mTransport = host.createTransport();
      mTransport.isMetronomeEnabled().markInterested();
      mTransport.tempo().markInterested();
      mTransport.isPlaying().markInterested();
      mTransport.isArrangerRecordEnabled().markInterested();
      mTransport.isArrangerAutomationWriteEnabled().markInterested();
      mTransport.isClipLauncherAutomationWriteEnabled().markInterested();
      mTransport.isClipLauncherOverdubEnabled().markInterested();
      mTransport.defaultLaunchQuantization().markInterested();
   }

   private void createSettingsObjects(final ControllerHost host)
   {
      final Preferences preferences = host.getPreferences();
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
   }

   private void postInit()
   {
      if (mPanAsChannelStripSetting.get())
         activateTopMode(TopMode.CHANNEL_STRIP);
      else
         activateTopMode(TopMode.PAN);
   }

   private void createLayers()
   {
      // We create all the layers here because the main layer might bind actions to activate other layers.
      mLayers = new Layers(this);
      mMainLayer = new Layer(mLayers, "Main");
      mChannelStripLayer = new Layer(mLayers, "ChannelStrip");
      mShiftLayer = new Layer(mLayers, "Shift");
      mBankLayer = new Layer(mLayers, "Bank");
      mSendSelectLayer = new Layer(mLayers, "SendSelect");
      mUserSelectLayer = new Layer(mLayers, "UserSelect");

      createMainLayer();
      createPanLayer();
      createUserLayers();
      createDebugLayer();
      createSendLayers();
      createChannelStripLayer();
      createShiftLayer();
      createBankLayer();
      createSendSelectLayer();
      createUserSelectLayer();
   }

   private void createUserSelectLayer()
   {
      for (int i = 0; i < 5; ++i)
      {
         final int I = i;
         mUserSelectLayer.bindPressed(mSceneButtons[i], () -> {
            mUserIndex = I;
            activateTopMode(mTopMode);
         });
      }
   }

   private void createSendSelectLayer()
   {
      for (int i = 0; i < 5; ++i)
      {
         final int I = i;
         mSendSelectLayer.bindPressed(mSceneButtons[i], () -> {
            mSendIndex = I;
            activateTopMode(mTopMode);
         });
      }
   }

   private void createBankLayer()
   {
      mBankLayer.bindPressed(mPrevDeviceButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(0), () -> "Select Remote Controls Page 1"));
      mBankLayer.bindPressed(mNextDeviceButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(1), () -> "Select Remote Controls Page 2"));
      mBankLayer.bindPressed(mPrevBankButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(2), () -> "Select Remote Controls Page 3"));
      mBankLayer.bindPressed(mNextBankButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(3), () -> "Select Remote Controls Page 4"));
      mBankLayer.bindPressed(mDeviceOnOffButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(4), () -> "Select Remote Controls Page 5"));
      mBankLayer.bindPressed(mDeviceLockButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(5), () -> "Select Remote Controls Page 6"));
      mBankLayer.bindPressed(mClipDeviceViewButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(6), () -> "Select Remote Controls Page 7"));
      mBankLayer.bindPressed(mDetailViewButton, getHost().createAction(
         () -> mRemoteControls.selectedPageIndex().set(7), () -> "Select Remote Controls Page 8"));

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
      mShiftLayer.bindPressed(mPlayButton, mTransport.continuePlaybackAction());
      mShiftLayer.bindToggle(mRecordButton, mTransport.isArrangerRecordEnabled());
      mShiftLayer.bindToggle(mSessionButton, mTransport.isArrangerAutomationWriteEnabled());

      for (int i = 0; i < 8; ++i)
      {
         final int x = i;
         mShiftLayer.bindPressed(mTrackSelectButtons[x], getHost().createAction(() -> {
            setLaunchQuantizationFromTrackSelect(x);
         }, () -> "Configures the default launch quantization"));
         mShiftLayer.bind(() -> x == computeLaunchQuantizationIndex(), mTrackSelectLeds[x]);

         final Track track = mTrackBank.getItemAt(i);
         final ClipLauncherSlotBank clipLauncherSlotBank = track.clipLauncherSlotBank();
         for (int j = 0; j < 5; ++j)
         {
            final ClipLauncherSlot slot = clipLauncherSlotBank.getItemAt(j);
            final HardwareButton clipButton = mGridButtons[i + 8 * j];
            mShiftLayer.bindPressed(clipButton, slot.launchWithOptionsAction("none", "continue_immediately"));
            mShiftLayer.bindReleased(clipButton, track.launchLastClipWithOptionsAction("none", "continue_immediately"));
         }
      }

      for (int i = 0; i < 5; ++i)
      {
         final Scene scene = mSceneBank.getScene(i);
         mShiftLayer.bindPressed(mSceneButtons[i], scene.launchWithOptionsAction("none", "continue_immediately"));
         mShiftLayer.bindReleased(mSceneButtons[i], scene.launchLastClipWithOptionsAction("none", "continue_immediately"));
      }
   }

   private void setLaunchQuantizationFromTrackSelect(final int x)
   {
      final String quantization;

      switch (x)
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

   private void createChannelStripLayer()
   {
      for (int i = 0; i < 8; ++i)
         mChannelStripLayer.bind(mTopControlKnobs[i],
            i < CHANNEL_STRIP_NUM_PARAMS ? mChannelStripRemoteControls.getParameter(i)
               : mTrackCursor.sendBank().getItemAt(i - CHANNEL_STRIP_NUM_PARAMS));
   }

   private void createSendLayers()
   {
      mSendLayers = new Layer[5];
      for (int sendIndex = 0; sendIndex < 5; ++sendIndex)
      {
         final Layer layer = new Layer(mLayers, "Send-" + sendIndex);
         for (int i = 0; i < 8; ++i)
            layer.bind(mTopControlKnobs[i], mTrackBank.getItemAt(i).sendBank().getItemAt(sendIndex));

         mSendLayers[sendIndex] = layer;
      }
   }

   private void createUserLayers()
   {
      mUserLayers = new Layer[5];
      for (int userIndex = 0; userIndex < 5; ++userIndex)
      {
         mUserLayers[userIndex] = new Layer(mLayers, "User-" + userIndex);
         for (int i = 0; i < 8; ++i)
            mUserLayers[userIndex].bind(mTopControlKnobs[i], mUserControls.getControl(i + userIndex * 8));
      }
   }

   private void createPanLayer()
   {
      mPanLayer = new Layer(mLayers, "Pan");
      for (int i = 0; i < 8; ++i)
         mPanLayer.bind(mTopControlKnobs[i], mTrackBank.getItemAt(i).pan());
   }

   private void createDebugLayer()
   {
      if (ENABLE_DEBUG_LAYER)
      {
         mDebugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
         mDebugLayer.activate();
      }
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
      mMainLayer.bind(mCueLevelKnob, mProject.cueVolume());

      for (int x = 0; x < 8; ++x)
      {
         final Track track = mTrackBank.getItemAt(x);
         for (int y = 0; y < 5; ++y)
         {
            final int offset = 8 * y + x;
            mMainLayer.bindPressed(mGridButtons[offset],
               track.clipLauncherSlotBank().getItemAt(y).launchAction());
         }
         mMainLayer.bindPressed(mMuteButtons[x], track.mute());
         mMainLayer.bind(() -> track.exists().get() && !track.mute().get(), mMuteLeds[x]);
         mMainLayer.bindToggle(mSoloButtons[x], track.solo());
         mMainLayer.bindToggle(mArmButtons[x], track.arm());
         mMainLayer.bindPressed(mABButtons[x], getHost().createAction(() -> {
            final SettableEnumValue crossFadeMode = track.crossFadeMode();
            final int nextValue = (crossFadeToInt(crossFadeMode.get()) + 1) % 3;
            crossFadeMode.set(intToCrossFade(nextValue));
         }, () -> "Cycle through crossfade values"));
         mMainLayer.bind(track.crossFadeMode(), mABLeds[x]);

         mMainLayer.bindPressed(mTrackSelectButtons[x],
            getHost().createAction(() -> mTrackCursor.selectChannel(track), () -> "Selects the track"));
         mMainLayer.bind(mIsTrackSelected[x], mTrackSelectLeds[x]);
         mMainLayer.bindPressed(mTrackStopButtons[x], track.stopAction());
         mMainLayer.bind(() -> track.exists().get() && !track.isStopped().get(), mTrackStopLeds[x]);
      }

      mMainLayer.bindPressed(mMasterTrackSelectButton,
         getHost().createAction(() -> mMasterTrack.selectInMixer(), () -> "Selects the master track"));
      mMainLayer.bind(mIsMasterSelected, mMasterTrackSelectLed);
      mMainLayer.bindPressed(mMasterTrackStopButton, mProject.getRootTrackGroup().stopAction());
      mRootTrackGroup.isStopped().markInterested();
      mMainLayer.bindInverted(mRootTrackGroup.isStopped(), mMasterTrackStopLed);

      mMainLayer.bindToggle(mPlayButton, mTransport.isPlaying());
      mMainLayer.bindToggle(mRecordButton, mTransport.isClipLauncherOverdubEnabled());
      mMainLayer.bindToggle(mSessionButton, mTransport.isClipLauncherAutomationWriteEnabled());
      mMainLayer.bindToggle(mMetronomeButton, mTransport.isMetronomeEnabled());
      mMainLayer.bindPressed(mTapTempoButton, mTransport.tapTempoAction());

      final HardwareActionBindable incTempoAction = getHost().createAction(
         () -> mTransport.tempo().incRaw(mShiftButton.isPressed().get() ? 0.1 : 1),
         () -> "Increments the tempo");
      final HardwareActionBindable decTempoAction = getHost().createAction(
         () -> mTransport.tempo().incRaw(mShiftButton.isPressed().get() ? -0.1 : -1),
         () -> "Decrements the tempo");
      mMainLayer.bind(mTempoKnob,
         getHost().createRelativeHardwareControlStepTarget(incTempoAction, decTempoAction));

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

      mMainLayer.bindPressed(mClipDeviceViewButton,
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

      mMainLayer.bindPressed(mPanButton,
         getHost().createAction(
            () -> activateTopMode(mPanAsChannelStripSetting.get() ? TopMode.CHANNEL_STRIP : TopMode.PAN),
            () -> "Activate Pan mode or ChannelStrip mode"));
      mMainLayer.bindPressed(mSendsButton,
         getHost().createAction(() -> activateTopMode(TopMode.SENDS), () -> "Activate Sends mode"));
      mMainLayer.bindPressed(mUserButton,
         getHost().createAction(() -> activateTopMode(TopMode.USER), () -> "Activate User mode"));

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
      createVolumeControls();
      createGridButtons();
      createMuteButtons();
      createSoloButtons();
      createArmButtons();
      createABButtons();
      createTrackSelectButtons();
      createTrackStopButtons();
      createTransportControls();

      setPhysicalPositions();
   }

   private void setPhysicalPositions()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("TopKnob-0").setBounds(11.25, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-1").setBounds(43.25, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-2").setBounds(75.0, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-3").setBounds(107.0, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-4").setBounds(139.0, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-5").setBounds(170.75, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-6").setBounds(202.75, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("TopKnob-7").setBounds(234.75, 12.25, 20.25, 20.0);
      surface.hardwareElementWithId("Pan").setBounds(295.5, 36.0, 13.0, 6.25);
      surface.hardwareElementWithId("Sends").setBounds(295.75, 54.5, 13.0, 6.25);
      surface.hardwareElementWithId("User").setBounds(295.75, 71.75, 13.0, 6.25);
      surface.hardwareElementWithId("DeviceControl-0").setBounds(288.5, 90.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-1").setBounds(320.5, 90.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-2").setBounds(352.75, 90.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-3").setBounds(384.75, 90.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-4").setBounds(288.5, 122.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-5").setBounds(320.5, 122.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-6").setBounds(352.75, 122.0, 27.5, 24.25);
      surface.hardwareElementWithId("DeviceControl-7").setBounds(384.75, 122.0, 27.5, 24.25);
      surface.hardwareElementWithId("PrevDevice").setBounds(296.0, 155.0, 13.0, 6.25);
      surface.hardwareElementWithId("NextDevice").setBounds(327.75, 155.0, 13.0, 6.25);
      surface.hardwareElementWithId("PrevBank").setBounds(359.75, 155.0, 13.0, 6.25);
      surface.hardwareElementWithId("NextBank").setBounds(391.0, 155.0, 13.0, 6.25);
      surface.hardwareElementWithId("DeviceOnOff").setBounds(296.0, 173.0, 13.0, 6.25);
      surface.hardwareElementWithId("DeviceLock").setBounds(327.75, 173.0, 13.0, 6.25);
      surface.hardwareElementWithId("ClipDeviceView").setBounds(359.75, 173.0, 13.0, 6.25);
      surface.hardwareElementWithId("DetailView").setBounds(391.0, 173.0, 13.0, 6.25);
      surface.hardwareElementWithId("Shift").setBounds(359.75, 191.25, 13.0, 6.25);
      surface.hardwareElementWithId("Bank").setBounds(391.0, 191.0, 13.0, 6.25);
      surface.hardwareElementWithId("LauncherUp").setBounds(306.0, 192.5, 13.25, 11.25);
      surface.hardwareElementWithId("LauncherDown").setBounds(306.0, 204.25, 13.25, 11.25);
      surface.hardwareElementWithId("LauncherLeft").setBounds(296.0, 192.5, 10.0, 23.0);
      surface.hardwareElementWithId("LauncherRight").setBounds(319.75, 192.5, 10.0, 23.0);
      surface.hardwareElementWithId("TrackVolumeFader-0").setBounds(9.75, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-1").setBounds(42.0, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-2").setBounds(73.75, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-3").setBounds(106.0, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-4").setBounds(138.0, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-5").setBounds(170.0, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-6").setBounds(201.75, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("TrackVolumeFader-7").setBounds(233.75, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("MasterTrackVolumeFader").setBounds(261.0, 182.0, 21.75, 64.0);
      surface.hardwareElementWithId("AB-Crossfade").setBounds(339.75, 230.25, 56.25, 10.75);
      surface.hardwareElementWithId("Cue-Level").setBounds(262.5, 156.25, 18.5, 20.0);
      surface.hardwareElementWithId("Grid-0-0").setBounds(6.75, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-0-1").setBounds(6.75, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-0-2").setBounds(6.75, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-0-3").setBounds(6.75, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-0-4").setBounds(6.75, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-1-0").setBounds(38.25, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-1-1").setBounds(38.25, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-1-2").setBounds(38.25, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-1-3").setBounds(38.25, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-1-4").setBounds(38.25, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-2-0").setBounds(70.5, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-2-1").setBounds(70.5, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-2-2").setBounds(70.5, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-2-3").setBounds(70.5, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-2-4").setBounds(70.5, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-3-0").setBounds(102.5, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-3-1").setBounds(102.5, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-3-2").setBounds(102.5, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-3-3").setBounds(102.5, 87.5, 28.75, 10.0);
      surface.hardwareElementWithId("Grid-3-4").setBounds(102.5, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-4-0").setBounds(134.5, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-4-1").setBounds(134.5, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-4-2").setBounds(134.5, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-4-3").setBounds(134.5, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-4-4").setBounds(134.5, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-5-0").setBounds(166.5, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-5-1").setBounds(166.5, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-5-2").setBounds(166.5, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-5-3").setBounds(166.5, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-5-4").setBounds(166.5, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-6-0").setBounds(198.5, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-6-1").setBounds(198.5, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-6-2").setBounds(198.5, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-6-3").setBounds(198.5, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-6-4").setBounds(198.5, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-7-0").setBounds(230.5, 41.75, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-7-1").setBounds(230.5, 57.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-7-2").setBounds(230.5, 72.0, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-7-3").setBounds(230.5, 87.5, 28.0, 10.0);
      surface.hardwareElementWithId("Grid-7-4").setBounds(230.5, 102.75, 28.0, 10.0);
      surface.hardwareElementWithId("Scene-0").setBounds(262.5, 41.75, 17.25, 11.0);
      surface.hardwareElementWithId("Scene-1").setBounds(262.5, 56.5, 17.25, 11.0);
      surface.hardwareElementWithId("Scene-2").setBounds(262.5, 72.0, 17.25, 11.0);
      surface.hardwareElementWithId("Scene-3").setBounds(262.5, 87.0, 17.25, 11.0);
      surface.hardwareElementWithId("Scene-4").setBounds(262.5, 102.75, 17.25, 11.0);
      surface.hardwareElementWithId("Mute-0").setBounds(7.0, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-1").setBounds(39.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-2").setBounds(70.75, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-3").setBounds(102.75, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-4").setBounds(135.0, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-5").setBounds(167.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-6").setBounds(199.25, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Mute-7").setBounds(231.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-0").setBounds(7.5, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-1").setBounds(39.25, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-2").setBounds(70.75, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-3").setBounds(103.0, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-4").setBounds(135.75, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-5").setBounds(167.5, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-6").setBounds(199.5, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Solo-7").setBounds(231.75, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-0").setBounds(23.0, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-1").setBounds(55.5, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-2").setBounds(87.25, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-3").setBounds(118.75, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-4").setBounds(151.75, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-5").setBounds(183.0, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-6").setBounds(216.0, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("Arm-7").setBounds(248.0, 166.75, 10.0, 10.0);
      surface.hardwareElementWithId("AB-0").setBounds(23.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-1").setBounds(55.75, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-2").setBounds(87.25, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-3").setBounds(119.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-4").setBounds(151.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-5").setBounds(182.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-6").setBounds(216.0, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("AB-7").setBounds(247.5, 152.5, 10.0, 10.0);
      surface.hardwareElementWithId("TrackSelect-0").setBounds(7.25, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-1").setBounds(39.5, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-2").setBounds(70.75, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-3").setBounds(103.0, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-4").setBounds(134.75, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-5").setBounds(166.75, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-6").setBounds(199.5, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("TrackSelect-7").setBounds(231.0, 137.25, 27.25, 9.5);
      surface.hardwareElementWithId("MasterTrackSelect").setBounds(262.5, 137.75, 16.75, 9.25);
      surface.hardwareElementWithId("TrackStop-0").setBounds(12.75, 119.75, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-1").setBounds(43.75, 120.0, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-2").setBounds(75.5, 120.0, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-3").setBounds(107.5, 120.0, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-4").setBounds(140.25, 119.75, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-5").setBounds(171.25, 120.25, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-6").setBounds(203.5, 120.5, 17.25, 11.0);
      surface.hardwareElementWithId("TrackStop-7").setBounds(235.5, 120.25, 17.25, 11.0);
      surface.hardwareElementWithId("MasterTrackStop").setBounds(262.5, 120.25, 17.25, 11.0);
      surface.hardwareElementWithId("Play").setBounds(328.0, 31.0, 13.0, 10.0);
      surface.hardwareElementWithId("Record").setBounds(360.0, 31.0, 13.0, 10.0);
      surface.hardwareElementWithId("Session").setBounds(391.25, 31.0, 13.0, 10.0);
      surface.hardwareElementWithId("Metronome").setBounds(327.75, 54.25, 13.0, 6.25);
      surface.hardwareElementWithId("TapTempo").setBounds(359.5, 54.75, 13.0, 6.25);
      surface.hardwareElementWithId("Nudge+").setBounds(327.5, 72.25, 13.0, 6.25);
      surface.hardwareElementWithId("Nudge-").setBounds(359.25, 72.25, 13.0, 6.25);
      surface.hardwareElementWithId("Tempo").setBounds(389.5, 57.25, 19.0, 20.5);
      surface.hardwareElementWithId("ABLed-0").setBounds(24.0, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-1").setBounds(56.0, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-2").setBounds(88.0, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-3").setBounds(119.75, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-4").setBounds(151.75, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-5").setBounds(183.75, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-6").setBounds(215.75, 153.25, 10.0, 10.0);
      surface.hardwareElementWithId("ABLed-7").setBounds(247.5, 153.25, 10.0, 10.0);
   }

   private void createTransportControls()
   {
      mPlayButton = mHardwareSurface.createHardwareButton("Play");
      mPlayButton.setLabel("PLAY");
      mPlayButton.setLabelPosition(RelativePosition.ABOVE);
      mPlayButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PLAY));
      mPlayButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PLAY));
      mPlayLed = mHardwareSurface.createOnOffHardwareLight("PlayLed");
      mPlayLed.onUpdateHardware(() -> sendLedUpdate(BT_PLAY, mPlayLed));
      mPlayLed.setOnColor(Color.fromRGB(0, 1, 0));
      mPlayButton.setBackgroundLight(mPlayLed);

      mRecordButton = mHardwareSurface.createHardwareButton("Record");
      mRecordButton.setLabel("RECORD");
      mRecordButton.setLabelPosition(RelativePosition.ABOVE);
      mRecordButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_RECORD));
      mRecordButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_RECORD));
      mRecordLed = mHardwareSurface.createOnOffHardwareLight("RecordLed");
      mRecordLed.setOnColor(Color.fromRGB(1, 0, 0));
      mRecordLed.onUpdateHardware(() -> sendLedUpdate(BT_RECORD, mRecordLed));
      mRecordButton.setBackgroundLight(mRecordLed);

      mSessionButton = mHardwareSurface.createHardwareButton("Session");
      mSessionButton.setLabel("SESSION");
      mSessionButton.setLabelPosition(RelativePosition.ABOVE);
      mSessionButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SESSION));
      mSessionButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SESSION));
      mSessionLed = mHardwareSurface.createOnOffHardwareLight("SessionLed");
      mSessionLed.setOnColor(Color.fromRGB(1, 0, 0));
      mSessionLed.onUpdateHardware(() -> sendLedUpdate(BT_SESSION, mSessionLed));
      mSessionButton.setBackgroundLight(mSessionLed);

      mMetronomeButton = mHardwareSurface.createHardwareButton("Metronome");
      mMetronomeButton.setLabel("METRONOME");
      mMetronomeButton.setLabelPosition(RelativePosition.ABOVE);
      mMetronomeButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_METRONOME));
      mMetronomeButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_METRONOME));
      mMetronomeLed = mHardwareSurface.createOnOffHardwareLight("MetronomeLed");
      mMetronomeLed.setOnColor(Color.fromRGB255(255,165,0));
      mMetronomeLed.onUpdateHardware(() -> sendLedUpdate(BT_METRONOME, mMetronomeLed));
      mMetronomeButton.setBackgroundLight(mMetronomeLed);

      mTapTempoButton = mHardwareSurface.createHardwareButton("TapTempo");
      mTapTempoButton.setLabel("TAP TEMPO");
      mTapTempoButton.setLabelPosition(RelativePosition.ABOVE);
      mTapTempoButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_TAP_TEMPO));
      mTapTempoButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_TAP_TEMPO));

      mNudgePlusButton = mHardwareSurface.createHardwareButton("Nudge+");
      mNudgePlusButton.setLabel("NUDGE +");
      mNudgePlusButton.setLabelPosition(RelativePosition.ABOVE);
      mNudgePlusButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NUDGE_PLUS));
      mNudgePlusButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NUDGE_PLUS));

      mNudgeMinusButton = mHardwareSurface.createHardwareButton("Nudge-");
      mNudgeMinusButton.setLabel("NUDGE -");
      mNudgeMinusButton.setLabelPosition(RelativePosition.ABOVE);
      mNudgeMinusButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NUDGE_MINUS));
      mNudgeMinusButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NUDGE_MINUS));

      mTempoKnob = mHardwareSurface.createRelativeHardwareKnob("Tempo");
      mTempoKnob.setLabel("TEMPO");
      mTempoKnob.setLabelPosition(RelativePosition.ABOVE);
      mTempoKnob.setAdjustValueMatcher(mMidiIn.createRelative2sComplementCCValueMatcher(0, CC_TEMPO, 128));
      mTempoKnob.setStepSize(1 / 128.0);
   }

   private void createTrackStopButtons()
   {
      mTrackStopButtons = new HardwareButton[8];
      mTrackStopLeds = new OnOffHardwareLight[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackStop-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_STOP));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_STOP));
         bt.setIndexInGroup(x);
         mTrackStopButtons[x] = bt;

         final int channel = x;
         final OnOffHardwareLight led = mHardwareSurface.createOnOffHardwareLight("TrackStopLed-" + x);
         led.setOnColor(Color.fromRGB255(255,165,0));
         led.onUpdateHardware(() -> sendLedUpdate(BT_TRACK_STOP, channel, led));
         bt.setBackgroundLight(led);
         mTrackStopLeds[x] = led;
      }

      mMasterTrackStopButton = mHardwareSurface.createHardwareButton("MasterTrackStop");
      mMasterTrackStopButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_MASTER_STOP));
      mMasterTrackStopButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_MASTER_STOP));
      mMasterTrackStopLed = mHardwareSurface.createOnOffHardwareLight("MasterTrackStopLed");
      mMasterTrackStopLed.setOnColor(Color.fromRGB255(255,165,0));
      mMasterTrackStopButton.setBackgroundLight(mMasterTrackStopLed);
      mMasterTrackStopLed.onUpdateHardware(() -> sendLedUpdate(BT_MASTER_STOP, mMasterTrackStopLed));
   }

   private void createTrackSelectButtons()
   {
      mTrackSelectButtons = new HardwareButton[8];
      mTrackSelectLeds = new OnOffHardwareLight[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackSelect-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_SELECT));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_SELECT));
         bt.setIndexInGroup(x);
         mTrackSelectButtons[x] = bt;

         final int channel = x;
         final OnOffHardwareLight led = mHardwareSurface.createOnOffHardwareLight("TrackSelectLed-" + x);
         led.setOnColor(Color.fromRGB255(255,165,0));
         led.onUpdateHardware(() -> sendLedUpdate(BT_TRACK_SELECT, channel, led));
         bt.setBackgroundLight(led);
         mTrackSelectLeds[x] = led;
      }

      mMasterTrackSelectButton = mHardwareSurface.createHardwareButton("MasterTrackSelect");
      mMasterTrackSelectButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_MASTER_SELECT));
      mMasterTrackSelectButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_MASTER_SELECT));
      mMasterTrackSelectLed = mHardwareSurface.createOnOffHardwareLight("MasterTrackSelectLed");
      mMasterTrackSelectLed.setOnColor(Color.fromRGB255(255,165,0));
      mMasterTrackSelectButton.setBackgroundLight(mMasterTrackSelectLed);
      mMasterTrackSelectLed.onUpdateHardware(() -> sendLedUpdate(BT_MASTER_SELECT, mMasterTrackSelectLed));
   }

   private void createABButtons()
   {
      mABButtons = new HardwareButton[8];
      mABLeds = new HardwareTextDisplay[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("AB-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_AB));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_AB));
         mABButtons[x] = bt;

         final int channel = x;
         final HardwareTextDisplay led = mHardwareSurface.createHardwareTextDisplay("ABLed-" + x, 1);
         led.onUpdateHardware(() -> {
            sendLedUpdate(BT_TRACK_AB, channel, crossFadeToInt(led.line(0).text().currentValue()));
         });
         mABLeds[x] = led;
      }
   }

   private static Color getABLedColor(final int i)
   {
      switch (i)
      {
      case 1:
         return Color.fromRGB(1.0, 0.5, 0);
      case 2:
         return Color.fromRGB(0, 0, 1.0);
      default:
         return Color.fromRGB(0, 0, 0);
      }
   }

   private void createArmButtons()
   {
      mArmButtons = new HardwareButton[8];
      mArmLeds = new OnOffHardwareLight[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Arm-" + x);
         bt.setLabel("\u25CF");
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_ARM));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_ARM));
         bt.setIndexInGroup(x);
         mArmButtons[x] = bt;

         final int channel = x;
         final OnOffHardwareLight led = mHardwareSurface.createOnOffHardwareLight("ArmLed-" + x);
         led.setOnColor(Color.fromRGB(1, 0, 0));
         led.onUpdateHardware(() -> sendLedUpdate(BT_TRACK_ARM, channel, led));
         bt.setBackgroundLight(led);
         mArmLeds[x] = led;
      }
   }

   private void createSoloButtons()
   {
      mSoloButtons = new HardwareButton[8];
      mSoloLeds = new OnOffHardwareLight[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Solo-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_SOLO));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_SOLO));
         bt.setLabel("S");
         bt.setIndexInGroup(x);
         mSoloButtons[x] = bt;

         final int channel = x;
         final OnOffHardwareLight led = mHardwareSurface.createOnOffHardwareLight("SoloLed-" + x);
         led.setOnColor(Color.fromRGB(0, 0, 1));
         led.onUpdateHardware(() -> sendLedUpdate(BT_TRACK_SOLO, channel, led));
         bt.setBackgroundLight(led);
         mSoloLeds[x] = led;
      }
   }

   private void createMuteButtons()
   {
      mMuteButtons = new HardwareButton[8];
      mMuteLeds = new OnOffHardwareLight[8];
      for (int x = 0; x < 8; ++x)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Mute-" + x);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(x, BT_TRACK_MUTE));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(x, BT_TRACK_MUTE));
         bt.setLabel(String.valueOf(x + 1));
         bt.setIndexInGroup(x);
         mMuteButtons[x] = bt;

         final int channel = x;
         final OnOffHardwareLight led = mHardwareSurface.createOnOffHardwareLight("MuteLed-" + x);
         led.setOnColor(Color.fromRGB255(255,165,0));
         led.onUpdateHardware(() -> sendLedUpdate(BT_TRACK_MUTE, channel, led));
         bt.setBackgroundLight(led);
         mMuteLeds[x] = led;
      }
   }

   private void createGridButtons()
   {
      mGridButtons = new HardwareButton[8 * 5];
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 5; ++y)
         {
            final String id = "Grid-" + x + "-" + y;
            final HardwareButton bt = mHardwareSurface.createHardwareButton(id);
            final int note = BT_GRID0 + (4 - y) * 8 + x;
            bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, note));
            bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, note));

            mGridButtons[y * 8 + x] = bt;
            mGridLeds[x][y] = new RgbLed(bt, mHardwareSurface, MSG_NOTE_ON, BT_GRID0 + x + (4 - y) * 8);
         }
      }

      mSceneButtons = new HardwareButton[5];

      for (int y = 0; y < 5; ++y)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("Scene-" + y);
         bt.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SCENE0 + y));
         bt.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SCENE0 + y));
         mSceneButtons[y] = bt;

         mSceneLeds[y] = new RgbLed(bt, mHardwareSurface, MSG_NOTE_ON, BT_SCENE0 + y);
      }
   }

   private void createVolumeControls()
   {
      mTrackVolumeSliders = new HardwareSlider[8];
      for (int i = 0; i < 8; ++i)
      {
         final HardwareSlider slider = mHardwareSurface.createHardwareSlider("TrackVolumeFader-" + i);
         slider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(i, CC_TRACK_VOLUME));
         slider.setIndexInGroup(i);

         mTrackVolumeSliders[i] = slider;
      }

      mMasterTrackVolumeSlider = mHardwareSurface.createHardwareSlider("MasterTrackVolumeFader");
      mMasterTrackVolumeSlider
         .setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC_MASTER_VOLUME));

      mABCrossfadeSlider = mHardwareSurface.createHardwareSlider("AB-Crossfade");
      mABCrossfadeSlider.setIsHorizontal(true);
      mABCrossfadeSlider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC_AB_CROSSFADE));

      mCueLevelKnob = mHardwareSurface.createRelativeHardwareKnob("Cue-Level");
      mCueLevelKnob.setAdjustValueMatcher(mMidiIn.createRelative2sComplementCCValueMatcher(0, CC_CUE, 128));
   }

   private void createTopControls()
   {
      mTopControlKnobs = new AbsoluteHardwareKnob[8];
      for (int i = 0; i < 8; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("TopKnob-" + i);
         final int I = i;
         final int CC = CC_TOP_CTL0 + i;
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC));
         knob.disableTakeOver();
         knob.isUpdatingTargetValue().markInterested();
         knob.hasTargetValue().addValueObserver(newValue -> updateTopControlRing(I));
         knob.targetValue().addValueObserver(newValue -> updateTopControlRing(I));
         knob.setIndexInGroup(i);
         mTopControlKnobs[i] = knob;
      }

      mPanButton = mHardwareSurface.createHardwareButton("Pan");
      mPanButton.setLabel("PAN");
      mPanButton.setLabelPosition(RelativePosition.ABOVE);
      mPanButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PAN));
      mPanButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PAN));
      mPanLed = mHardwareSurface.createOnOffHardwareLight("PanLed");
      mPanLed.setOnColor(Color.fromRGB255(255,165,0));
      mPanButton.setBackgroundLight(mPanLed);
      mPanLed.onUpdateHardware(() -> sendLedUpdate(BT_PAN, mPanLed));

      mSendsButton = mHardwareSurface.createHardwareButton("Sends");
      mSendsButton.setLabel("SENDS");
      mSendsButton.setLabelPosition(RelativePosition.ABOVE);
      mSendsButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SENDS));
      mSendsButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SENDS));
      mSendsButton.isPressed().addValueObserver(isPressed -> {
         mSendsOn.stateChanged(isPressed);
         if (mSendsOn.isOn())
            mSendSelectLayer.activate();
         else
            mSendSelectLayer.deactivate();
      });
      mSendsLed = mHardwareSurface.createOnOffHardwareLight("SendsLed");
      mSendsLed.setOnColor(Color.fromRGB255(255,165,0));
      mSendsButton.setBackgroundLight(mSendsLed);
      mSendsLed.onUpdateHardware(() -> sendLedUpdate(BT_SENDS, mSendsLed));

      mUserButton = mHardwareSurface.createHardwareButton("User");
      mUserButton.setLabel("USER");
      mUserButton.setLabelPosition(RelativePosition.ABOVE);
      mUserButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_USER));
      mUserButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_USER));
      mUserButton.isPressed().addValueObserver(isPressed -> {
         mUserOn.stateChanged(isPressed);
         if (mUserOn.isOn())
            mUserSelectLayer.activate();
         else
            mUserSelectLayer.deactivate();
      });
      mUserLed = mHardwareSurface.createOnOffHardwareLight("UserLed");
      mUserLed.setOnColor(Color.fromRGB255(255,165,0));
      mUserButton.setBackgroundLight(mUserLed);
      mUserLed.onUpdateHardware(() -> sendLedUpdate(BT_USER, mUserLed));
   }

   private void createDeviceControls()
   {
      mDeviceControlKnobs = new AbsoluteHardwareKnob[8];

      for (int i = 0; i < 8; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("DeviceControl-" + i);
         final int I = i;
         final int CC = CC_DEV_CTL0 + i;
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC));
         knob.disableTakeOver();
         knob.setBounds(285 + 32 * (i % 4), 90 + 35 * (i / 4), PHYSICAL_KNOB_WIDTH, PHYSICAL_KNOB_WIDTH);
         knob.isUpdatingTargetValue().markInterested();
         knob.hasTargetValue().addValueObserver(newValue -> updateDeviceControlRing(I));
         knob.targetValue().addValueObserver(newValue -> updateDeviceControlRing(I));
         knob.setLabel(String.valueOf(i + 1));
         knob.setLabelPosition(RelativePosition.BELOW);
         knob.setIndexInGroup(i);

         mDeviceControlKnobs[i] = knob;
      }

      mPrevDeviceButton = mHardwareSurface.createHardwareButton("PrevDevice");
      mPrevDeviceButton.setLabel("\u2190DEVICE");
      mPrevDeviceButton.setLabelPosition(RelativePosition.BELOW);
      mPrevDeviceButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PREV_DEVICE));
      mPrevDeviceButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PREV_DEVICE));
      mPrevDeviceLed = mHardwareSurface.createOnOffHardwareLight("PrevDeviceLed");
      mPrevDeviceLed.setOnColor(Color.fromRGB255(255,165,0));
      mPrevDeviceButton.setBackgroundLight(mPrevDeviceLed);
      mPrevDeviceLed.onUpdateHardware(() -> sendLedUpdate(BT_PREV_DEVICE, mPrevDeviceLed));

      mNextDeviceButton = mHardwareSurface.createHardwareButton("NextDevice");
      mNextDeviceButton.setLabel("DEVICE\u2192");
      mNextDeviceButton.setLabelPosition(RelativePosition.BELOW);
      mNextDeviceButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NEXT_DEVICE));
      mNextDeviceButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NEXT_DEVICE));
      mNextDeviceLed = mHardwareSurface.createOnOffHardwareLight("NextDeviceLed");
      mNextDeviceLed.setOnColor(Color.fromRGB255(255,165,0));
      mNextDeviceButton.setBackgroundLight(mNextDeviceLed);
      mNextDeviceLed.onUpdateHardware(() -> sendLedUpdate(BT_NEXT_DEVICE, mNextDeviceLed));

      mPrevBankButton = mHardwareSurface.createHardwareButton("PrevBank");
      mPrevBankButton.setLabel("\u2190BANK");
      mPrevBankButton.setLabelPosition(RelativePosition.BELOW);
      mPrevBankButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_PREV_BANK));
      mPrevBankButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_PREV_BANK));
      mPrevBankLed = mHardwareSurface.createOnOffHardwareLight("PrevBankLed");
      mPrevBankLed.setOnColor(Color.fromRGB255(255,165,0));
      mPrevBankButton.setBackgroundLight(mPrevBankLed);
      mPrevBankLed.onUpdateHardware(() -> sendLedUpdate(BT_PREV_BANK, mPrevBankLed));

      mNextBankButton = mHardwareSurface.createHardwareButton("NextBank");
      mNextBankButton.setLabel("BANK\u2192");
      mNextBankButton.setLabelPosition(RelativePosition.BELOW);
      mNextBankButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_NEXT_BANK));
      mNextBankButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_NEXT_BANK));
      mNextBankLed = mHardwareSurface.createOnOffHardwareLight("NextBankLed");
      mNextBankLed.setOnColor(Color.fromRGB255(255,165,0));
      mNextBankButton.setBackgroundLight(mNextBankLed);
      mNextBankLed.onUpdateHardware(() -> sendLedUpdate(BT_NEXT_BANK, mNextBankLed));

      mDeviceOnOffButton = mHardwareSurface.createHardwareButton("DeviceOnOff");
      mDeviceOnOffButton.setLabel("DEV ON/OFF");
      mDeviceOnOffButton.setLabelPosition(RelativePosition.BELOW);
      mDeviceOnOffButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_DEVICE_ONOFF));
      mDeviceOnOffButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_DEVICE_ONOFF));
      mDeviceOnOffLed = mHardwareSurface.createOnOffHardwareLight("DeviceOnOffLed");
      mDeviceOnOffLed.setOnColor(Color.fromRGB255(255,165,0));
      mDeviceOnOffButton.setBackgroundLight(mDeviceOnOffLed);
      mDeviceOnOffLed.onUpdateHardware(() -> sendLedUpdate(BT_DEVICE_ONOFF, mDeviceOnOffLed));

      mDeviceLockButton = mHardwareSurface.createHardwareButton("DeviceLock");
      mDeviceLockButton.setLabel("DEV LOCK");
      mDeviceLockButton.setLabelPosition(RelativePosition.BELOW);
      mDeviceLockButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_DEVICE_LOCK));
      mDeviceLockButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_DEVICE_LOCK));
      mDeviceLockLed = mHardwareSurface.createOnOffHardwareLight("DeviceLockLed");
      mDeviceLockLed.setOnColor(Color.fromRGB255(255,165,0));
      mDeviceLockButton.setBackgroundLight(mDeviceLockLed);
      mDeviceLockLed.onUpdateHardware(() -> sendLedUpdate(BT_DEVICE_LOCK, mDeviceLockLed));

      mClipDeviceViewButton = mHardwareSurface.createHardwareButton("ClipDeviceView");
      mClipDeviceViewButton.setLabel("CLIP/DEV VIEW");
      mClipDeviceViewButton.setLabelPosition(RelativePosition.BELOW);
      mClipDeviceViewButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_CLIP_DEVICE_VIEW));
      mClipDeviceViewButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_CLIP_DEVICE_VIEW));
      mClipDeviceViewLed = mHardwareSurface.createOnOffHardwareLight("ClipDeviceViewLed");
      mClipDeviceViewLed.setOnColor(Color.fromRGB255(255,165,0));
      mClipDeviceViewButton.setBackgroundLight(mClipDeviceViewLed);
      mClipDeviceViewLed.onUpdateHardware(() -> sendLedUpdate(BT_CLIP_DEVICE_VIEW, mClipDeviceViewLed));

      mDetailViewButton = mHardwareSurface.createHardwareButton("DetailView");
      mDetailViewButton.setLabel("DETAIL VIEW");
      mDetailViewButton.setLabelPosition(RelativePosition.BELOW);
      mDetailViewButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_DETAIL_VIEW));
      mDetailViewButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_DETAIL_VIEW));
      mDetailViewLed = mHardwareSurface.createOnOffHardwareLight("DetailViewLed");
      mDetailViewLed.setOnColor(Color.fromRGB255(255,165,0));
      mDetailViewButton.setBackgroundLight(mDetailViewLed);
      mDetailViewLed.onUpdateHardware(() -> sendLedUpdate(BT_DETAIL_VIEW, mDetailViewLed));

      mShiftButton = mHardwareSurface.createHardwareButton("Shift");
      mShiftButton.setLabel("SHIFT");
      mShiftButton.setLabelPosition(RelativePosition.BELOW);
      mShiftButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_SHIFT));
      mShiftButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_SHIFT));
      mShiftButton.isPressed().markInterested();

      mBankButton = mHardwareSurface.createHardwareButton("Bank");
      mBankButton.setLabel("BANK");
      mBankButton.setLabelPosition(RelativePosition.BELOW);
      mBankButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_BANK));
      mBankButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_BANK));
      mBankButton.isPressed().addValueObserver((isPressed) -> {
         mBankOn.stateChanged(isPressed);
         if (mBankOn.isOn())
         {
            mBankLayer.activate();
            sendLedUpdate(BT_BANK, 0, 1);
         }
         else
         {
            mBankLayer.deactivate();
            sendLedUpdate(BT_BANK, 0, 0);
         }
      });
      mBankLed = mHardwareSurface.createOnOffHardwareLight("BankLed");
      mBankLed.setOnColor(Color.fromRGB255(255,165,0));
      mBankButton.setBackgroundLight(mBankLed);
      mBankLed.onUpdateHardware(() -> sendLedUpdate(BT_BANK, mBankLed));

      mLauncherUpButton = mHardwareSurface.createHardwareButton("LauncherUp");
      mLauncherUpButton.setLabel("\u2191");
      mLauncherUpButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_UP));
      mLauncherUpButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_UP));

      mLauncherDownButton = mHardwareSurface.createHardwareButton("LauncherDown");
      mLauncherDownButton.setLabel("\u2193");
      mLauncherDownButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_DOWN));
      mLauncherDownButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_DOWN));

      mLauncherLeftButton = mHardwareSurface.createHardwareButton("LauncherLeft");
      mLauncherLeftButton.setLabel("\u2190");
      mLauncherLeftButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_LEFT));
      mLauncherLeftButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_LEFT));

      mLauncherRightButton = mHardwareSurface.createHardwareButton("LauncherRight");
      mLauncherRightButton.setLabel("\u2192");
      mLauncherRightButton.pressedAction()
         .setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, BT_LAUNCHER_RIGHT));
      mLauncherRightButton.releasedAction()
         .setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, BT_LAUNCHER_RIGHT));
   }

   private void updateTopControlRing(final int knobIndex)
   {
      final KnobLed knobLed = mTopControlKnobLeds[knobIndex];
      final AbsoluteHardwareKnob knob = mTopControlKnobs[knobIndex];
      final int value = (int)(127 * knob.targetValue().get());
      if (!knob.isUpdatingTargetValue().get())
         knobLed.set(value);
      else
         knobLed.setDisplayedValue(value);

      final int ring;
      switch (mTopMode)
      {
      case PAN:
         ring = KnobLed.RING_PAN;
         break;
      case SENDS:
         ring = KnobLed.RING_VOLUME;
         break;
      case CHANNEL_STRIP:
         ring = knobIndex < CHANNEL_STRIP_NUM_PARAMS ? KnobLed.RING_SINGLE : KnobLed.RING_VOLUME;
         break;
      case USER:
         ring = KnobLed.RING_SINGLE;
         break;
      default:
         throw new IllegalStateException();
      }

      if (knobLed.wantsFlush())
         knobLed.setRing(knob.hasTargetValue().get() ? ring : KnobLed.RING_OFF);
   }

   private void updateDeviceControlRing(final int knobIndex)
   {
      final KnobLed knobLed = mDeviceControlKnobLeds[knobIndex];
      final AbsoluteHardwareKnob knob = mDeviceControlKnobs[knobIndex];
      final int value = (int)(127 * knob.targetValue().get());
      if (!knob.isUpdatingTargetValue().get())
         knobLed.set(value);
      else
         knobLed.setDisplayedValue(value);
      knobLed.setRing(knob.hasTargetValue().get() ? KnobLed.RING_SINGLE : KnobLed.RING_OFF);

      if (knobLed.wantsFlush())
         getHost().requestFlush();
   }

   private void sendLedUpdate(final int note, final OnOffHardwareLight led)
   {
      sendLedUpdate(note, 0, led);
   }

   private void sendLedUpdate(final int note, final int channel, final OnOffHardwareLight led)
   {
      mMidiOut.sendMidi((MSG_NOTE_ON << 4) | channel, note, led.isOn().currentValue() ? 1 : 0);
   }

   private void sendLedUpdate(final int note, final int channel, final int color)
   {
      mMidiOut.sendMidi((MSG_NOTE_ON << 4) | channel, note, color);
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

      if (topMode == TopMode.SENDS && mControlSendEffectSetting.get())
         mTrackCursor.selectChannel(mSendTrackBank.getItemAt(mSendIndex));
   }

   private void onSysexIn(final String sysex)
   {
      // Set the device in the proper mode (Ableton Live Mode 2)
      mMidiOut.sendSysex("F0 47 7F 29 60 00 04 41 02 01 00 F7");
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
      flushKnobs();
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
            rgbLed.setColor(mSendIndex == i ? RGBLedState.COLOR_PLAYING : RGBLedState.COLOR_STOPPING);
            rgbLed.setBlinkType(RGBLedState.BLINK_NONE);
            rgbLed.setBlinkColor(RGBLedState.COLOR_NONE);
         }
         else if (mUserOn.isOn())
         {
            rgbLed.setColor(mUserIndex == i ? RGBLedState.COLOR_PLAYING : RGBLedState.COLOR_STOPPING);
            rgbLed.setBlinkType(RGBLedState.BLINK_NONE);
            rgbLed.setBlinkColor(RGBLedState.COLOR_NONE);
         }
         else
         {
            final Scene scene = mSceneBank.getScene(i);
            if (scene.exists().get())
               rgbLed.setColor(scene.color());
            else
               rgbLed.setColor(RGBLedState.COLOR_NONE);
            rgbLed.setBlinkType(RGBLedState.BLINK_NONE);
            rgbLed.setBlinkColor(RGBLedState.COLOR_NONE);
         }

         rgbLed.paint(mMidiOut);
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
            final RgbLed rgbLed = mGridLeds[i][j];

            if (slot.exists().get() && slot.hasContent().get())
               rgbLed.setColor(slot.color().red(), slot.color().green(), slot.color().blue());
            else
               rgbLed.setColor(RGBLedState.COLOR_NONE);

            /*
             * if (slot.isStopQueued().get()) { rgbLed.setBlinkType(RgbLed.BLINK_STOP_QUEUED);
             * rgbLed.setBlinkColor(RgbLed.COLOR_STOPPING); } else
             */

            if (slot.isRecordingQueued().get())
            {
               rgbLed.setBlinkType(RGBLedState.BLINK_RECORD_QUEUED);
               rgbLed.setBlinkColor(RGBLedState.COLOR_RECORDING);
            }
            else if (slot.isPlaybackQueued().get())
            {
               rgbLed.setBlinkType(RGBLedState.BLINK_PLAY_QUEUED);
               rgbLed.setBlinkColor(RGBLedState.COLOR_PLAYING_QUEUED);
            }
            else if (slot.isRecording().get())
            {
               rgbLed.setColor(RGBLedState.COLOR_NONE);
               rgbLed.setBlinkType(RGBLedState.BLINK_ACTIVE);
               rgbLed.setBlinkColor(RGBLedState.COLOR_RECORDING);
            }
            else if (slot.isPlaying().get())
            {
               rgbLed.setColor(RGBLedState.COLOR_NONE);
               rgbLed.setBlinkType(RGBLedState.BLINK_ACTIVE);
               rgbLed.setBlinkColor(RGBLedState.COLOR_PLAYING);
            }
            else /* stopped */
            {
               rgbLed.setBlinkType(RGBLedState.BLINK_NONE);
               rgbLed.setBlinkColor(RGBLedState.COLOR_NONE);
            }

            rgbLed.paint(mMidiOut);
         }
      }
   }

   private void flushKnobs()
   {
      for (int i = 0; i < 8; ++i)
      {
         mDeviceControlKnobLeds[i].flush(mMidiOut, MSG_CC, 0, CC_DEV_CTL0 + i);
         mTopControlKnobLeds[i].flush(mMidiOut, MSG_CC, 0, CC_TOP_CTL0 + i);
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

   /**
    * Helper class that will stay as pressed if there is a "double press" (like a double click).
    */
   private class DoublePressedButtonState
   {
      void stateChanged(final boolean isPressed)
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

   ////////////////////////
   // Host Proxy Objects //
   ////////////////////////

   private Application mApplication = null;

   private Project mProject;

   private Track mRootTrackGroup;

   private Transport mTransport = null;

   private MasterTrack mMasterTrack = null;

   private BooleanValue mIsMasterSelected = null;

   private TrackBank mTrackBank = null;

   private BooleanValue[] mIsTrackSelected = new BooleanValue[8];

   private TrackBank mSendTrackBank = null;

   private SceneBank mSceneBank = null;

   private CursorTrack mTrackCursor = null;

   private PinnableCursorDevice mDeviceCursor = null;

   private PinnableCursorDevice mChannelStripDevice;

   private CursorRemoteControlsPage mRemoteControls = null;

   private CursorRemoteControlsPage mChannelStripRemoteControls;

   private UserControlBank mUserControls = null;

   private MidiIn mMidiIn = null;

   private MidiOut mMidiOut = null;

   //////////////
   // Settings //
   //////////////

   private SettableBooleanValue mPanAsChannelStripSetting;

   private SettableBooleanValue mHorizontalScrollByPageSetting;

   private SettableBooleanValue mVerticalScrollByPageSetting;

   private SettableBooleanValue mControlSendEffectSetting;

   ///////////
   // State //
   ///////////

   private final DoublePressedButtonState mBankOn = new DoublePressedButtonState();

   private final DoublePressedButtonState mUserOn = new DoublePressedButtonState();

   private final DoublePressedButtonState mSendsOn = new DoublePressedButtonState();

   private TopMode mTopMode = TopMode.PAN;

   private int mSendIndex = 0; // 0..4

   private int mUserIndex = 0; // 0..4

   ////////////
   // Layers //
   ////////////

   private Layers mLayers;

   private Layer mMainLayer;

   private Layer mDebugLayer;

   private Layer mPanLayer;

   private Layer[] mUserLayers;

   private Layer[] mSendLayers;

   private Layer mChannelStripLayer;

   private Layer mShiftLayer;

   private Layer mBankLayer;

   private Layer mSendSelectLayer;

   private Layer mUserSelectLayer;

   ///////////////////////
   // Hardware Controls //
   ///////////////////////

   private HardwareSurface mHardwareSurface;

   private AbsoluteHardwareKnob[] mTopControlKnobs;

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

   private OnOffHardwareLight[] mMuteLeds;

   private OnOffHardwareLight[] mSoloLeds;

   private OnOffHardwareLight[] mArmLeds;

   private HardwareTextDisplay[] mABLeds;

   private OnOffHardwareLight[] mTrackSelectLeds;

   private OnOffHardwareLight mMasterTrackSelectLed;

   private OnOffHardwareLight[] mTrackStopLeds;

   private OnOffHardwareLight mMasterTrackStopLed;

   /////////////////
   // Custom LEDs //
   /////////////////

   private final KnobLed[] mDeviceControlKnobLeds = new KnobLed[8];

   private final KnobLed[] mTopControlKnobLeds = new KnobLed[8];

   private final RgbLed[][] mGridLeds = new RgbLed[8][5];

   private final RgbLed[] mSceneLeds = new RgbLed[5];
}
