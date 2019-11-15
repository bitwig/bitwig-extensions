package com.bitwig.extensions.controllers.akai.apc40_mkii;

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
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
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

   private static final int BT_LAUNCHER_TOP = 94;

   private static final int BT_LAUNCHER_BOTTOM = 95;

   private static final int BT_NUDGE_MINUS = 100;

   private static final int BT_NUDGE_PLUS = 101;

   private static final int BT_BANK0 = 58;

   private static final int BT_DEVICE_LEFT = 58;

   private static final int BT_DEVICE_RIGHT = 59;

   private static final int BT_BANK_LEFT = 60;

   private static final int BT_BANK_RIGHT = 61;

   private static final int BT_DEV_ONOFF = 62;

   private static final int BT_DEV_LOCK = 63;

   private static final int BT_CLIP_DEV_VIEW = 64;

   private static final int BT_DETAIL_VIEW = 65;

   private static final long DOUBLE_MAX_TIME = 250;

   private static final double PHYSICAL_KNOB_WIDTH = 20;


   protected APC40MKIIControllerExtension(
      final ControllerExtensionDefinition controllerExtensionDefinition,
      final ControllerHost host)
   {
      super(controllerExtensionDefinition, host);

      mBankLeds[0] = mDevPrevLed;
      mBankLeds[1] = mDevNextLed;
      mBankLeds[2] = mDevControlsPrevLed;
      mBankLeds[3] = mDevControlsNextLed;
      mBankLeds[4] = mDevOnOffLed;
      mBankLeds[5] = mDevLockLed;
      mBankLeds[6] = mClipDevViewLed;
      mBankLeds[7] = mDetailViewLed;
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
      mTransport.isClipLauncherAutomationWriteEnabled().markInterested();
      mTransport.isMetronomeEnabled().markInterested();
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
      mLayers = new Layers(this);

      createMainLayer();
      createPanLayer();
      createUserLayers();
      createDebugLayer();
      createSendLayers();
      createChannelStripLayer();
   }

   private void createChannelStripLayer()
   {
      mChannelStripLayer = new Layer(mLayers, "ChannelStrip");
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
      mMainLayer = new Layer(mLayers, "Main");

      for (int i = 0; i < 8; ++i)
      {
         mMainLayer.bind(mDeviceControlKnobs[i], mRemoteControls.getParameter(i));
         mMainLayer.bind(mTrackVolumeFaders[i], mTrackBank.getItemAt(i).volume());
      }
      mMainLayer.bind(mMasterTrackVolumeFader, mMasterTrack.volume());
      mMainLayer.bind(mABCrossfade, mTransport.crossfade());
      // TODO: mMainLayer.bind(mCueLevelKnob, );

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 5; ++y)
         {
            final int offset = 8 * y + x;
            mMainLayer.bindPressed(mGridButtons[offset], mTrackBank.getItemAt(x).clipLauncherSlotBank().getItemAt(y).launchAction());
         }
      }

      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(420, 260);

      createTopKnobs();
      createDeviceControlKnobs();
      createVolumeFaders();
      createGridButtons();
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
   }

   private void createVolumeFaders()
   {
      mTrackVolumeFaders = new AbsoluteHardwareKnob[8];
      for (int i = 0; i < 8; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("TrackVolumeFader-" + i);
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(i, CC_TRACK_VOLUME));

         mTrackVolumeFaders[i] = knob;
      }

      mMasterTrackVolumeFader = mHardwareSurface.createAbsoluteHardwareKnob("MasterTrackVolumeFader");
      mMasterTrackVolumeFader.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC_MASTER_VOLUME));

      mABCrossfade = mHardwareSurface.createAbsoluteHardwareKnob("AB-Crossfade");
      mABCrossfade.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, CC_AB_CROSSFADE));

      mCueLevelKnob = mHardwareSurface.createRelativeHardwareKnob("Cue-Level");
      mCueLevelKnob.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, CC_CUE));
   }

   private void createTopKnobs()
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
   }

   private void createDeviceControlKnobs()
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
            mTransport.tempo().incRaw((mIsShiftOn ? 0.1 : 1) * (data2 == 1 ? 1 : -1));
         else if (data2 == CC_CUE)
            mCueConrol.getControl(0).inc((data2 < 64 ? data2 : 127 - data2) / 127.0);
      }
      else if (msg == MSG_NOTE_ON)
      {
         if (data1 == BT_TRACK_STOP)
            mTrackBank.getItemAt(channel).stop();
         else if (data1 == BT_TRACK_SELECT)
         {
            if (mIsShiftOn)
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
            else
               mTrackBank.getItemAt(channel).selectInMixer();
         }
         else if (data1 == BT_MASTER_SELECT)
            mMasterTrack.selectInMixer();
         else if (data1 == BT_MASTER_STOP)
            mSceneBank.stop();
         else if (BT_SCENE0 <= data1 && data1 < BT_SCENE0 + 5)
         {
            final int index = data1 - BT_SCENE0;
            if (mIsUserOn)
               mUserIndex = index;
            else if (mIsSendsOn)
            {
               mSendIndex = index;
               if (mControlSendEffectSetting.get())
                  mTrackCursor.selectChannel(mSendTrackBank.getItemAt(mSendIndex));
               updateSendIndication(mSendIndex);
            }
            else
            {
               final Scene scene = mSceneBank.getScene(index);
               scene.launch();
            }
         }
         else if (data1 == BT_TRACK_MUTE)
            mTrackBank.getItemAt(channel).mute().toggle();
         else if (data1 == BT_TRACK_SOLO)
            mTrackBank.getItemAt(channel).solo().toggle();
         else if (data1 == BT_TRACK_ARM)
            mTrackBank.getItemAt(channel).arm().toggle();
         else if (data1 == BT_TRACK_AB)
         {
            final SettableEnumValue crossFadeMode = mTrackBank.getItemAt(channel).crossFadeMode();
            final int nextValue = (crossFadeToInt(crossFadeMode.get()) + 1) % 3;
            crossFadeMode.set(intToCrossFade(nextValue));
         }
         else if (data1 == BT_PLAY)
            mTransport.togglePlay();
         else if (data1 == BT_RECORD)
            getRecordButtonValue().toggle();
         else if (data1 == BT_SESSION)
            getSessionButtonValue().toggle();
         else if (data1 == BT_METRONOME)
            mTransport.isMetronomeEnabled().toggle();
         else if (data1 == BT_TAP_TEMPO)
            mTransport.tapTempo();
         else if (data1 == BT_LAUNCHER_TOP)
         {
            if (mIsShiftOn ^ mVerticalScrollByPageSetting.get())
               mSceneBank.scrollPageBackwards();
            else
               mSceneBank.scrollBackwards();
         }
         else if (data1 == BT_LAUNCHER_BOTTOM)
         {
            if (mIsShiftOn ^ mVerticalScrollByPageSetting.get())
               mSceneBank.scrollPageForwards();
            else
               mSceneBank.scrollForwards();
         }
         else if (data1 == BT_LAUNCHER_LEFT)
         {
            if (mIsShiftOn ^ mHorizontalScrollByPageSetting.get())
               mTrackBank.scrollPageBackwards();
            else
               mTrackBank.scrollBackwards();
         }
         else if (data1 == BT_LAUNCHER_RIGHT)
         {
            if (mIsShiftOn ^ mHorizontalScrollByPageSetting.get())
               mTrackBank.scrollPageForwards();
            else
               mTrackBank.scrollForwards();
         }
         else if (data1 == BT_SHIFT)
            mIsShiftOn = true;
         else if (data1 == BT_BANK)
            mIsBankOn = !mIsBankOn;
         else if (data1 == BT_PAN)
         {
            mTopMode = mPanAsChannelStripSetting.get() ? TopMode.CHANNEL_STRIP : TopMode.PAN;
            updateTopIndications();
         }
         else if (data1 == BT_SENDS)
         {
            mTopMode = TopMode.SENDS;
            mIsSendsOn = true;
            mIsUserOn = false;
            updateTopIndications();
         }
         else if (data1 == BT_USER)
         {
            mTopMode = TopMode.USER;
            mIsUserOn = true;
            mIsSendsOn = false;
            updateTopIndications();
         }
         else if (!mIsBankOn && data1 == BT_DEVICE_LEFT)
         {
            mDeviceCursor.selectPrevious();
         }
         else if (!mIsBankOn && data1 == BT_DEVICE_RIGHT)
         {
            mDeviceCursor.selectNext();
         }
         else if (!mIsBankOn && data1 == BT_BANK_LEFT)
         {
            mRemoteControls.selectPrevious();
         }
         else if (!mIsBankOn && data1 == BT_BANK_RIGHT)
         {
            mRemoteControls.selectNext();
         }
         else if (!mIsBankOn && data1 == BT_DEV_ONOFF)
            mDeviceCursor.isEnabled().toggle();
         else if (!mIsBankOn && data1 == BT_DEV_LOCK)
            mDeviceCursor.isPinned().toggle();
         else if (!mIsBankOn && data1 == BT_DETAIL_VIEW)
            mDeviceCursor.isWindowOpen().toggle();
         else if (!mIsBankOn && data1 == BT_CLIP_DEV_VIEW)
            mApplication.nextSubPanel();
         else if (mIsBankOn && BT_BANK0 <= data1 && data1 < BT_BANK0 + 8)
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
      else if (msg == MSG_NOTE_OFF)
      {
         if (data1 == BT_SHIFT)
            mIsShiftOn = false;
         else if (data1 == BT_SENDS)
         {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - mLastBtSendTime >= DOUBLE_MAX_TIME && !mIsShiftOn)
               mIsSendsOn = false;
            mLastBtSendTime = currentTimeMillis;
         }
         else if (data1 == BT_USER)
         {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - mLastBtUserTime >= DOUBLE_MAX_TIME && !mIsShiftOn)
               mIsUserOn = false;
            mIsUserOn = false;
            mLastBtUserTime = currentTimeMillis;
         }
         else if (data1 == BT_BANK)
         {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - mLastBtBankTime >= DOUBLE_MAX_TIME && !mIsShiftOn)
               mIsBankOn = false;
            mLastBtBankTime = currentTimeMillis;
         }
      }
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
      paintButtons();
      paintPads();
      paintScenes();
   }

   private void paintScenes()
   {
      for (int i = 0; i < 5; ++i)
      {
         final RgbLed rgbLed = mSceneLeds[i];
         if (mIsSendsOn)
         {
            rgbLed.setColor(mSendIndex == i ? RgbLed.COLOR_PLAYING : RgbLed.COLOR_STOPPING);
            rgbLed.setBlinkType(RgbLed.BLINK_NONE);
            rgbLed.setBlinkColor(RgbLed.COLOR_NONE);
         }
         else if (mIsUserOn)
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

   private void paintButtons()
   {
      mPlayLed.set(mTransport.isPlaying().get() ? 1 : 0);
      mPlayLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_PLAY);

      mRecordLed.set(getRecordButtonValue().get() ? 1 : 0);
      mRecordLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_RECORD);

      mSessionLed.set(getSessionButtonValue().get() ? 1 : 0);
      mSessionLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_SESSION);

      mPanLed.set((mTopMode == TopMode.PAN || mTopMode == TopMode.CHANNEL_STRIP) ? 1 : 0);
      mPanLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_PAN);

      mSendsLed.set(mTopMode == TopMode.SENDS ? 1 : 0);
      mSendsLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_SENDS);

      mUserLed.set(mTopMode == TopMode.USER ? 1 : 0);
      mUserLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_USER);

      mMetronomeLed.set(mTransport.isMetronomeEnabled().get() ? 1 : 0);
      mMetronomeLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_METRONOME);

      mBankOnLed.set(mIsBankOn ? 1 : 0);
      mBankOnLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_BANK);

      if (!mIsBankOn)
      {
         mDevOnOffLed.set(mDeviceCursor.isEnabled().get() ? 1 : 0);
         mDevOnOffLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_DEV_ONOFF);

         mDevLockLed.set(mDeviceCursor.isPinned().get() ? 1 : 0);
         mDevLockLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_DEV_LOCK);

         mDevPrevLed.set(mDeviceCursor.hasPrevious().get() ? 1 : 0);
         mDevPrevLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_DEVICE_LEFT);

         mDevNextLed.set(mDeviceCursor.hasNext().get() ? 1 : 0);
         mDevNextLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_DEVICE_RIGHT);

         mDevControlsPrevLed.set(mRemoteControls.hasPrevious().get() ? 1 : 0);
         mDevControlsPrevLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_BANK_LEFT);

         mDevControlsNextLed.set(mRemoteControls.hasNext().get() ? 1 : 0);
         mDevControlsNextLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_BANK_RIGHT);

         mClipDevViewLed.set(1);
         mClipDevViewLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_CLIP_DEV_VIEW);

         mDetailViewLed.set((mDeviceCursor.exists().get() && mDeviceCursor.isWindowOpen().get()) ? 1 : 0);
         mDetailViewLed.paint(mMidiOut, MSG_NOTE_ON, 0, BT_DETAIL_VIEW);
      }
      else
      {
         for (int i = 0; i < 8; ++i)
         {
            mBankLeds[i].set(i == mRemoteControls.selectedPageIndex().get() ? 1 : 0);
            mBankLeds[i].paint(mMidiOut, MSG_NOTE_ON, 0, BT_BANK0 + i);
         }
      }
   }

   private SettableBooleanValue getRecordButtonValue()
   {
      return mIsShiftOn ? mTransport.isArrangerRecordEnabled() : mTransport.isClipLauncherOverdubEnabled();
   }

   private SettableBooleanValue getSessionButtonValue()
   {
      return mIsShiftOn ? mTransport.isArrangerAutomationWriteEnabled() : mTransport.isClipLauncherAutomationWriteEnabled();
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

         if (mIsShiftOn)
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

   private boolean mIsShiftOn = false;

   private boolean mIsBankOn = false;

   private boolean mIsUserOn = false;

   private boolean mIsSendsOn = false;

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

   private final Led mPlayLed = new Led();

   private final Led mRecordLed = new Led();

   private final Led mSessionLed = new Led();

   private final Led mPanLed = new Led();

   private final Led mSendsLed = new Led();

   private final Led mUserLed = new Led();

   private final Led mMetronomeLed = new Led();

   private final Led mBankOnLed = new Led();

   private final Led mDevOnOffLed = new Led();

   private final Led mDevLockLed = new Led();

   private final Led mDevPrevLed = new Led();

   private final Led mDevNextLed = new Led();

   private final Led mDevControlsPrevLed = new Led();

   private final Led mDevControlsNextLed = new Led();

   private final Led mClipDevViewLed = new Led();

   private final Led mDetailViewLed = new Led();

   private final Led[] mBankLeds = new Led[8];

   private final KnobLed[] mDeviceControlLeds = new KnobLed[8];

   private final KnobLed[] mTopControlLeds = new KnobLed[8];

   private final RgbLed[][] mPadLeds = new RgbLed[8][5];

   private final RgbLed[] mSceneLeds = new RgbLed[5];

   private final static String[] FADER_VOLUME_ENUM = new String[] { "0dB", "+6dB" };

   private final static String[] ON_OFF_ENUM = new String[] { "On", "Off" };

   private long mLastBtSendTime;

   private long mLastBtUserTime;

   private long mLastBtBankTime;

   private SettableBooleanValue mControlSendEffectSetting;
   private HardwareSurface mHardwareSurface;
   private AbsoluteHardwareKnob[] mTopKnobs;
   private AbsoluteHardwareKnob[] mDeviceControlKnobs;
   private Layers mLayers;
   private Layer mMainLayer;
   private Layer mDebugLayer;
   private Layer mPanLayer;
   private AbsoluteHardwareKnob[] mTrackVolumeFaders;
   private AbsoluteHardwareKnob mMasterTrackVolumeFader;
   private AbsoluteHardwareKnob mABCrossfade;
   private Layer[] mUserLayers;
   private Layer[] mSendLayers;
   private Layer mChannelStripLayer;
   private RelativeHardwareKnob mCueLevelKnob;
   private HardwareButton[] mGridButtons;
}
