package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.Project;
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
import jdk.nashorn.internal.runtime.Debug;

public final class LaunchpadProControllerExtension extends ControllerExtension
{
   /* Helper used to prevent Bitwig Studio from receiving MIDI notes. */
   public static final Integer[] FILTER_ALL_NOTE_MAP = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
   };

   final double PHYSICAL_DEVICE_WIDTH = 260;
   final double PHYSICAL_BUTTON_WIDTH = 20;
   final double PHYSICAL_BUTTON_SPACE = 4;
   final double PHYSICAL_BUTTON_OFFSET = 12;

   private static final String[] KEY_NAMES = new String[]{ "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

   public LaunchpadProControllerExtension(final LaunchpadProControllerExtensionDefinition driverDefinition, final ControllerHost host)
   {
      super(driverDefinition, host);
      mHost = host;

      initButtonStates();

      mSessionMode = new SessionMode(this);
      mDrumMode = new DrumMode(this);

      mKeyboardMode = new KeyboardMode(this);

      mVolumeMode = new VolumeMode(this);
      mScaleAndKeyChooserMode = new ScaleAndKeyChooserMode(this);
      mSendMode = new SendMode(this);
      mPanMode = new PanMode(this);
      mDrumSequencerMode = new DrumSequencerMode(this);
      mStepSequencerMode = new StepSequencerMode(this);

      mPlayNoteModes = new MultiplexerMode(this);
      mPlayNoteModes.setMode(0, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.GUITAR));
      mPlayNoteModes.setMode(1, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.LINE_3));
      mPlayNoteModes.setMode(2, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.LINE_7));
      mPlayNoteModes.setMode(3, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.PIANO));
      mPlayNoteModes.setMode(4, mScaleAndKeyChooserMode);
      mPlayNoteModes.setMode(5, mDrumMode);

      mRecordArmOverlay = new RecordArmOverlay(this);
      mTrackSelectOverlay = new TrackSelectOverlay(this);
      mMuteOverlay = new MuteOverlay(this);
      mStopClipOverlay = new StopClipOverlay(this);
      mSoloOverlay = new SoloOverlay(this);

      mCurrentMode = mSessionMode;
      mPreviousMode = mPlayNoteModes;
   }

   private void createLayers()
   {
      mLayers = new Layers(this);
      createMainLayer();
      createDebugLayer();
   }

   private void createDebugLayer()
   {
      mDebugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
      mDebugLayer.setShouldReplaceBindingsInLayersBelow(false);
      mDebugLayer.activate();
   }

   private void createMainLayer()
   {
      mMainLayer = new Layer(mLayers, "main");
      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(PHYSICAL_DEVICE_WIDTH, PHYSICAL_DEVICE_WIDTH);

      mShiftButton = createSideButton("shift", 0, 8);
      mClickButton = createSideButton("click", 0, 7);
      mUndoButton = createSideButton("undo", 0, 6);
      mDeleteButton = createSideButton("delete", 0, 5);
      mQuantizeButton = createSideButton("quantize", 0, 4);
      mDuplicateButton = createSideButton("duplicate", 0, 3);
      mDoubleButton = createSideButton("double", 0, 2);
      mRecordButton = createSideButton("record", 0, 1);

      mUpButton = createSideButton("up", 1, 9);
      mDownButton = createSideButton("down", 2, 9);
      mLeftButton = createSideButton("left", 3, 9);
      mRightButton = createSideButton("right", 4, 9);
      mSessionButton = createSideButton("session", 5, 9);
      mNoteButton = createSideButton("note", 6, 9);
      mDeviceButton = createSideButton("device", 7, 9);
      mUserButton = createSideButton("user", 8, 9);

      mArmButton = createSideButton("arm", 1, 0);
      mSelectButton = createSideButton("select", 2, 0);
      mMuteButton = createSideButton("mute", 3, 0);
      mSoloButton = createSideButton("solo", 4, 0);
      mVolumeButton = createSideButton("volume", 5, 0);
      mPanButton = createSideButton("pan", 6, 0);
      mSendsButton = createSideButton("sends", 7, 0);
      mStopButton = createSideButton("stop", 8, 0);

      mSceneButtons = new LaunchpadButtonAndLed[8];
      for (int y = 0; y < 8; ++y)
         mSceneButtons[y] = createSideButton("scene-" + y, 9, y + 1);

      mGridButtons = new LaunchpadButtonAndLed[8 * 8];
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 8; ++y)
         {
            final int index = (x + 1) + 10 * (y + 1);
            final String id = "grid-" + x + "-" + y;

            final LaunchpadButtonAndLed bt =
               new LaunchpadButtonAndLed(mHardwareSurface, id, mMidiIn, index, true);
            mGridButtons[y * 8 + x] = bt;
            setButtonPhysicalPosition(bt, x + 1, y + 1);
         }
      }
   }

   private LaunchpadButtonAndLed createSideButton(final String id, final int x, final int y)
   {
      final int index = x + 10 * y;
      final LaunchpadButtonAndLed bt =
         new LaunchpadButtonAndLed(mHardwareSurface, id, mMidiIn, index, false);
      setButtonPhysicalPosition(bt, x, y);
      return bt;
   }

   private void setButtonPhysicalPosition(final LaunchpadButtonAndLed bt, final int x, final int y)
   {
      bt.getButton().setBounds(calculatePhysicalPosition(x), calculatePhysicalPosition(y), PHYSICAL_BUTTON_WIDTH, PHYSICAL_BUTTON_WIDTH);
   }

   private double calculatePhysicalPosition(final int x)
   {
      return PHYSICAL_BUTTON_OFFSET + x * (PHYSICAL_BUTTON_WIDTH + PHYSICAL_BUTTON_SPACE);
   }

   private void initButtonStates()
   {
      for (int x = 0; x < 8; ++x)
      {
         // Grid
         for (int y = 0; y < 8; ++y)
            mButtonStates[getPadIndex(x, y)] = new ButtonState(1 + x, 1 + y);

         // Top and Bottom
         mButtonStates[getTopButtonIndex(x)] = new ButtonState(1 + x, 9);
         mButtonStates[getBottomButtonIndex(x)] = new ButtonState(1 + x, 0);
         mButtonStates[getLeftButtonIndex(x)] = new ButtonState(0, 1 + x);
         mButtonStates[getRightButtonIndex(x)] = new ButtonState(9, 1 + x);
      }

      assert checkButtonStatesIndex();
   }

   private boolean checkButtonStatesIndex()
   {
      for (int i = 0; i < mButtonStates.length; ++i)
      {
         final ButtonState buttonState = mButtonStates[i];
         if (buttonState == null)
            continue;

         final int buttonIndex = getButtonIndex(buttonState.getX(), buttonState.getY());
         assert buttonIndex == i;
      }
      return true;
   }

   ButtonState getButtonState(final int x, final int y)
   {
      return mButtonStates[x + 10 * y];
   }

   ButtonState getPadState(final int x, final int y)
   {
      return mButtonStates[getPadIndex(x, y)];
   }

   List<ButtonState> findPadsInHoldState()
   {
      final ArrayList<ButtonState> buttonStates = new ArrayList<>();
      for (ButtonState buttonState : mButtonStates)
         if (buttonState != null && buttonState.mState == ButtonState.State.HOLD)
            buttonStates.add(buttonState);

      return buttonStates;
   }

   private int getButtonIndex(final int x, final int y)
   {
      assert 0 <= x && x <= 9;
      assert 0 <= y && y <= 9;

      return x + y * 10;
   }

   private int getPadIndex(final int x, final int y)
   {
      return getButtonIndex(x + 1, y + 1);
   }

   private int getTopButtonIndex(final int x)
   {
      return getButtonIndex(x + 1, 9);
   }

   private int getBottomButtonIndex(final int x)
   {
      return getButtonIndex(x + 1, 0);
   }

   private int getLeftButtonIndex(final int y)
   {
      return getButtonIndex(0, y + 1);
   }

   private int getRightButtonIndex(final int y)
   {
      return getButtonIndex(9, y + 1);
   }

   @Override
   public void init()
   {
      try
      {
         Thread.sleep(4000);
      }
      catch (final InterruptedException e)
      {
         mHost.println(e.toString());
      }

      mApplication = mHost.createApplication();
      mApplication.recordQuantizationGrid().markInterested();

      mProject = mHost.getProject();
      mDocumentState = mHost.getDocumentState();

      mUserControls = mHost.createUserControls(64);
      mMasterTrack = mHost.createMasterTrack(8);
      mTransport = mHost.createTransport();
      mTransport.isClipLauncherAutomationWriteEnabled().markInterested();
      mTransport.isClipLauncherOverdubEnabled().markInterested();
      mTransport.isMetronomeEnabled().markInterested();
      mTransport.tempo().value().markInterested();
      mTransport.isPlaying().markInterested();
      mTransport.isArrangerRecordEnabled().markInterested();

      mCursorTrack = mHost.createCursorTrack(8, 0);
      mCursorTrack.color().markInterested();
      mCursorTrack.hasPrevious().markInterested();
      mCursorTrack.hasNext().markInterested();
      mCursorTrack.playingNotes().markInterested();

      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorDevice.hasDrumPads().markInterested();
      mCursorTrackDrumPads = mCursorDevice.createDrumPadBank(16);
      mCursorTrackDrumPads.setIndication(false);
      mCursorTrackDrumPads.exists().markInterested();
      mCursorTrackDrumPads.scrollPosition().markInterested();

      mCursorClip = mHost.createLauncherCursorClip(8 * 16, 128);
      mCursorClip.exists().markInterested();
      mCursorClip.exists().addValueObserver(mCurrentMode::onCursorClipExists);
      mCursorClip.color().markInterested();
      mCursorClip.getPlayStart().markInterested();
      mCursorClip.getPlayStop().markInterested();
      mCursorClip.getLoopStart().markInterested();
      mCursorClip.getLoopLength().markInterested();
      mCursorClip.playingStep().markInterested();
      mCursorClipSlot = mCursorClip.clipLauncherSlot();
      mCursorClipSlot.sceneIndex().markInterested();
      mCursorClipTrack = mCursorClip.getTrack();
      mCursorClipTrack.playingNotes().markInterested();
      mCursorClipTrack.color().markInterested();
      mCursorClipDevice = mCursorClipTrack.createCursorDevice();
      mCursorClipDevice.hasDrumPads().markInterested();
      mDrumScenesRemoteControls = mCursorClipDevice.createCursorRemoteControlsPage("scenes", 8, "drum-scenes");
      mDrumPerfsRemoteControls = mCursorClipDevice.createCursorRemoteControlsPage("perfs", 8, "drum-perfs");

      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl sceneParam = mDrumScenesRemoteControls.getParameter(i);
         sceneParam.exists().markInterested();
         sceneParam.value().markInterested();

         final RemoteControl perfParam = mDrumPerfsRemoteControls.getParameter(i);
         perfParam.exists().markInterested();
         perfParam.value().markInterested();
      }

      mCursorClipDrumPads = mCursorClipDevice.createDrumPadBank(16);
      mCursorClipDrumPads.setIndication(false);
      mCursorClipDrumPads.exists().markInterested();
      mCursorClipDrumPads.scrollPosition().markInterested();
      mCursorClipDrumPads.scrollPosition().addValueObserver(mDrumSequencerMode::invalidateDrumPosition);

      for (int i = 0; i < 16; ++i)
      {
         initDrumPad(mCursorClipDrumPads.getItemAt(i));
         initDrumPad(mCursorTrackDrumPads.getItemAt(i));
      }

      initMidi();
      initTrackBank();

      mSessionMode.activate();
      mSessionMode.paintModeButton();
      mPlayNoteModes.selectMode(0);
      mPlayNoteModes.paintModeButton();
      mDrumSequencerMode.paintModeButton();
      mStepSequencerMode.paintModeButton();

      mRecordArmOverlay.paintModeButton();
      mTrackSelectOverlay.paintModeButton();
      mMuteOverlay.paintModeButton();
      mSoloOverlay.paintModeButton();
      mVolumeMode.paintModeButton();
      mPanMode.paintModeButton();
      mSendMode.paintModeButton();
      mStopClipOverlay.paintModeButton();

      initDocumentMusicalInfo();
      initPreferences();
      createHardwareControls();
      createLayers();
   }

   private void initDrumPad(final DrumPad drumPad)
   {
      drumPad.exists().markInterested();
      drumPad.color().markInterested();
      drumPad.mute().markInterested();
      drumPad.solo().markInterested();
      drumPad.isMutedBySolo().markInterested();
   }

   private void initPreferences()
   {
      final Preferences preferences = mHost.getPreferences();

      final String NOTE_INPUT_CATEGORY = "Note Input";

      mSafePitchesSetting = preferences.getBooleanSetting("Safe Pitches", NOTE_INPUT_CATEGORY, false);
      mSafePitchesSetting.markInterested();
      mSafePitchesSetting.addValueObserver(newValue -> invalidateKeyboardModes());

      mHighlightRootKeySetting = preferences.getBooleanSetting("Highlight Root Key", NOTE_INPUT_CATEGORY, true);
      mHighlightRootKeySetting.markInterested();
      mHighlightRootKeySetting.addValueObserver(newValue -> invalidateKeyboardModes());

      mHighlightScaleSetting = preferences.getBooleanSetting("Highlight Scale", NOTE_INPUT_CATEGORY, true);
      mHighlightScaleSetting.markInterested();
      mHighlightScaleSetting.addValueObserver(newValue -> invalidateKeyboardModes());

      mKeyboardLayoutSetting = preferences.getEnumSetting("Keyboard Layout", NOTE_INPUT_CATEGORY, KeyboardLayout.OPTIONS, "Guitar");
      mKeyboardLayoutSetting.markInterested();
      mKeyboardLayoutSetting.addValueObserver(newValue -> {
         mKeyboardLayout = KeyboardLayout.fromString(newValue);
         invalidateKeyboardModes();
      });
   }

   private void initMidi()
   {
      mMidiIn = mHost.getMidiInPort(0);
      mMidiOut = mHost.getMidiOutPort(0);

      mMidiIn.setMidiCallback(LaunchpadProControllerExtension.this::onMidiReceived);
      mMidiIn.setSysexCallback(LaunchpadProControllerExtension.this::onSysexReceived);
      mNoteInput = mMidiIn.createNoteInput("Input", "??????");
      mNoteInput.setShouldConsumeEvents(false);
      mNoteInput.setKeyTranslationTable(FILTER_ALL_NOTE_MAP);
      mNoteInput.includeInAllInputs().markInterested();

      mArpeggiator = mNoteInput.arpeggiator();

      /* select the programmer layout */
      mMidiOut.sendSysex("F0 00 20 29 02 10 2C 03 F7");

      /* light off every leds */
      mMidiOut.sendSysex("F0 00 20 29 02 10 0E 00 F7");

      /* light shift */
      sendLedUpdateSysex(" 50 63 63 63");

      /* light undo */
      sendLedUpdateSysex(" 3C 63 63 63");
   }

   private void initTrackBank()
   {
      mTrackBank = mHost.createTrackBank(8, 8, 8, true);
      mTrackBank.followCursorTrack(mCursorTrack);
      mTrackBank.cursorIndex().markInterested();
      mSceneBank = mTrackBank.sceneBank();
      mSceneBank.canScrollBackwards().markInterested();
      mSceneBank.canScrollForwards().markInterested();
      mTrackBank.canScrollChannelsDown().markInterested();
      mTrackBank.canScrollChannelsUp().markInterested();
      mSceneBank.itemCount().markInterested();

      for (int i = 0; i < 8; ++i)
      {
         final Scene scene = mSceneBank.getItemAt(i);
         scene.color().markInterested();
         scene.exists().markInterested();

         final Track channel = mTrackBank.getItemAt(i);
         channel.exists().markInterested();
         channel.mute().markInterested();
         channel.solo().markInterested();
         channel.arm().markInterested();
         channel.volume().value().markInterested();
         channel.pan().value().markInterested();
         channel.color().markInterested();

         final ClipLauncherSlotBank clipLauncherSlots = channel.clipLauncherSlotBank();
         clipLauncherSlots.setIndication(false);

         final SendBank sendBank = channel.sendBank();

         for (int j = 0; j < 8; ++j)
         {
            final ClipLauncherSlot slot = clipLauncherSlots.getItemAt(j);
            slot.color().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.hasContent().markInterested();
            slot.isRecording().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.isStopQueued().markInterested();
            slot.isSelected().markInterested();

            final Send send = sendBank.getItemAt(j);
            send.value().markInterested();
            send.sendChannelColor().markInterested();
            send.exists().markInterested();
         }
      }
   }

   private void initDocumentMusicalInfo()
   {
      final String[] scalesName = MusicalScaleLibrary.getInstance().getScalesName();

      mMusicalKeySetting = mDocumentState.getEnumSetting("Key", "Musical Info", KEY_NAMES, "A");
      mMusicalScaleSetting = mDocumentState.getEnumSetting("Scale", "Musical Info", scalesName, scalesName[6]);
      mMusicalScaleSetting.addValueObserver(this::musicalScaleChanged);
      mMusicalKeySetting.addValueObserver(this::musicalKeyChanged);
   }

   private void musicalKeyChanged(final String keyName)
   {
      mMusicalKey = 0;
      for (int i = 0; i < 12; ++i)
      {
         if (keyName.equals(KEY_NAMES[i]))
         {
            mMusicalKey = i;
            break;
         }
      }

      invalidateKeyboardModes();
   }

   private void musicalScaleChanged(final String scaleName)
   {
      mMusicalScale = MusicalScaleLibrary.getInstance().getMusicalScale(scaleName);
      if (mMusicalScale == null)
         mMusicalScale = MusicalScaleLibrary.getInstance().getMusicalScale(0);

      invalidateKeyboardModes();
   }

   private void invalidateKeyboardModes()
   {
      mKeyboardMode.invalidate();
      mStepSequencerMode.invalidate();
   }

   private void sendLedUpdateSysex(final String ledUpdate)
   {
      assert !ledUpdate.isEmpty();

      mMidiOut.sendSysex("F0 00 20 29 02 10 0B" + ledUpdate + " F7");
   }

   @Override
   public void exit()
   {
      /* light off every leds */
      mMidiOut.sendSysex("F0 00 20 29 02 10 0E 00 F7");
   }

   @Override
   public void flush()
   {
      paintLeftLeds();
      mCurrentMode.paint();
      if (mBottomOverlay != null)
         mBottomOverlay.paint();

      final StringBuilder ledClear = new StringBuilder();
      StringBuilder ledUpdate = new StringBuilder();
      final StringBuilder ledPulseUpdate = new StringBuilder();

      for (final ButtonState buttonState : mButtonStates)
      {
         if (buttonState == null)
            continue;

         final Led led = buttonState.getLed();

         ledClear.append(led.updateClearSysex());
         ledUpdate.append(led.updateLightLEDSysex());
         ledPulseUpdate.append(led.updatePulseSysex());

         // Lets not send sysex that are too big
         if (ledUpdate.length() >= 4 * 3 * 48)
         {
            sendLedUpdateSysex(ledUpdate.toString());
            ledUpdate = new StringBuilder();
         }
      }

      if (ledClear.length() > 0)
         mMidiOut.sendSysex("F0 00 20 29 02 10 0A" + ledClear + " F7");

      if (ledUpdate.length() > 0)
         sendLedUpdateSysex(ledUpdate.toString());

      if (ledPulseUpdate.length() > 0)
         mMidiOut.sendSysex("F0 00 20 29 02 10 28" + ledPulseUpdate + " F7");
   }

   private boolean isRecording()
   {
      return mTransport.isClipLauncherOverdubEnabled().get() |
         mTransport.isClipLauncherAutomationWriteEnabled().get();
   }

   private void paintLeftLeds()
   {
      getLeftLed(7).setColor(Color.WHITE); // Shift
      getLeftLed(6).setColor(mTransport.isMetronomeEnabled().get() ? Color.YELLOW : Color.YELLOW_LOW); // Click
      getLeftLed(5).setColor(Color.ORANGE); // Undo
      getLeftLed(4).setColor(Color.ORANGE); // Delete
      getLeftLed(3).setColor(Color.CYAN); // Quantize
      getLeftLed(2).setColor(Color.CYAN); // Duplicate
      if (isShiftOn())
         getLeftLed(1).setColor(mTransport.isArrangerRecordEnabled().get() ? Color.RED : Color.RED_LOW); // Arranger Record
      else
         getLeftLed(1).setColor(mTransport.isPlaying().get() ? Color.GREEN : Color.GREEN_LOW); // Tranport Play
      getLeftLed(0).setColor(isRecording() ? Color.RED : Color.RED_LOW); // Clip Launcher Record
   }

   private void onSysexReceived(final String data)
   {
   }

   private void onMidiReceived(final int statusByte, final int data1, final int data2)
   {
      // mHost.println("MIDI: " + statusByte + ", " + data1 + ", " + data2);

      final int msg = statusByte & 0xF0;
      switch (msg)
      {
         case 0x90: /* Note On */
         {
            final int x = data1 % 10 - 1;
            final int y = data1 / 10 - 1;

            final ButtonState padState = getPadState(x, y);
            if (data2 > 0)
            {
               /* Pressed */
               padState.onButtonPressed(mHost);

               if (y == 0 && mBottomOverlay != null)
                  mBottomOverlay.onPadPressed(x, data2);
               else
                  mCurrentMode.onPadPressed(x, y, data2);
            }
            else
            {
               /* Released */
               final boolean wasHeld = padState.mState == ButtonState.State.HOLD;
               padState.onButtonReleased();

               if (y == 0 && mBottomOverlay != null)
                  mBottomOverlay.onPadReleased(x, data2);
               else
                  mCurrentMode.onPadReleased(x, y, data2, wasHeld);
            }
            break;
         }

         case 0xA0: /* Polyphonic After touch */
         {
            final int x = data1 % 10 - 1;
            final int y = data1 / 10 - 1;

            mCurrentMode.onPadPressure(x, y, data2);
            break;
         }

         case 0xB0: /* Control Change */
         {
            switch (data1)
            {
               case 91:
                  if (data2 > 0)
                     mCurrentMode.onArrowUpPressed();
                  else
                     mCurrentMode.onArrowUpReleased();
                  break;

               case 92:
                  if (data2 > 0)
                     mCurrentMode.onArrowDownPressed();
                  else
                     mCurrentMode.onArrowDownReleased();
                  break;

               case 93:
                  if (data2 > 0)
                     mCurrentMode.onArrowLeftPressed();
                  else
                     mCurrentMode.onArrowLeftReleased();
                  break;

               case 94:
                  if (data2 > 0)
                     mCurrentMode.onArrowRightPressed();
                  else
                     mCurrentMode.onArrowRightReleased();
                  break;

               case 95:
                  if (data2 > 0)
                     setMode(mSessionMode);
                  break;

               case 96:
                  if (data2 > 0)
                     setMode(mPlayNoteModes);
                  break;

               case 97:
                  if (data2 > 0)
                     setMode(mDrumSequencerMode);
                  break;

               case 98:
                  if (data2 > 0)
                     setMode(mStepSequencerMode);
                  break;

               case 80:
                  mIsShiftOn = data2 > 0;

                  if (data2 > 0)
                     mCurrentMode.onShiftPressed();
                  else
                     mCurrentMode.onShiftReleased();
                  break;

               case 70:
                  if (data2 > 0)
                  {
                     if (isShiftOn())
                        mTransport.tapTempo();
                     else
                        mTransport.isMetronomeEnabled().toggle();
                  }
                  break;

               case 60:
                  if (data2 > 0)
                  {
                     if (mIsShiftOn)
                        mApplication.redo();
                     else
                        mApplication.undo();
                  }
                  break;

               case 50:
                  //if (data2 > 0)
                  //   mApplication.remove();

                  mIsDeleteOn = data2 > 0;

                  if (mIsDeleteOn)
                     mCurrentMode.onDeletePressed();
                  else
                     mCurrentMode.onDeleteReleased();
                  break;

               case 40:
                  mIsQuantizeOn = data2 > 0;

                  if (mIsShiftOn)
                  {
                     if (data2 > 0)
                     {
                        final SettableEnumValue recordQuantizationGrid = mApplication.recordQuantizationGrid();
                        recordQuantizationGrid.set(recordQuantizationGrid.get().equals("OFF") ? "1/16" : "OFF");
                     }
                  }
                  else
                  {
                     if (mIsQuantizeOn)
                        mCurrentMode.onQuantizePressed();
                     else
                        mCurrentMode.onQuantizeReleased();
                  }
                  break;

               case 30:
                  if (data2 > 0)
                     mApplication.duplicate();
                  break;

               case 20:
                  /* double */
                  if (data2 > 0)
                  {
                     if (isShiftOn())
                        mTransport.isArrangerRecordEnabled().toggle();
                     else
                        mTransport.togglePlay();
                  }
                  break;

               case 10:
                  if (data2 > 0)
                  {
                     final boolean enabled = isRecording();
                     mTransport.isClipLauncherOverdubEnabled().set(!enabled);
                     mTransport.isClipLauncherAutomationWriteEnabled().set(!enabled);
                  }
                  break;

               case 11:
                  /* record arm */
                  break;

               case 12:
                  if (data2 > 0)
                     mCurrentMode.onTrackSelectPressed();
                  else
                     mCurrentMode.onTrackSelectReleased();
                  break;

               case 13:
                  if (data2 > 0)
                     mCurrentMode.onMutePressed();
                  else
                     mCurrentMode.onMuteReleased();
                  break;

               case 14:
                  if (data2 > 0)
                     mCurrentMode.onSoloPressed();
                  else
                     mCurrentMode.onSoloReleased();
                  break;

               case 15:
                  if (data2 > 0)
                     mCurrentMode.onVolumePressed();
                  else
                     mCurrentMode.onVolumeReleased();
                  break;

               case 16:
                  if (data2 > 0)
                     mCurrentMode.onPanPressed();
                  else
                     mCurrentMode.onPanReleased();
                  break;

               case 17:
                  if (data2 > 0)
                     mCurrentMode.onSendPressed();
                  else
                     mCurrentMode.onSendReleased();
                  break;

               case 18:
                  if (data2 > 0)
                     mCurrentMode.onStopClipPressed();
                  else
                     mCurrentMode.onStopClipReleased();
                  break;

               case 19:
               case 29:
               case 39:
               case 49:
               case 59:
               case 69:
               case 79:
               case 89:
               case 99:
                  final int col = data1 / 10 - 1;
                  if (data2 > 0)
                     mCurrentMode.onSceneButtonPressed(col);
                  else
                     mCurrentMode.onSceneButtonReleased(col);
                  break;

               case 1:
                  setBottomOverlay(mRecordArmOverlay, data2 > 0, getButtonState(1, 0));
                  break;

               case 2:
                  setBottomOverlay(mTrackSelectOverlay, data2 > 0, getButtonState(2, 0));
                  break;

               case 3:
                  setBottomOverlay(mMuteOverlay, data2 > 0, getButtonState(3, 0));
                  break;

               case 4:
                  setBottomOverlay(mSoloOverlay, data2 > 0, getButtonState(4, 0));
                  break;

               case 5:
                  if (data2 > 0)
                     setMode(mVolumeMode);
                  break;

               case 6:
                  if (data2 > 0)
                     setMode(mPanMode);
                  break;

               case 7:
                  if (data2 > 0)
                     setMode(mSendMode);
                  break;

               case 8:
                  if (data2 > 0 && mIsShiftOn)
                     mTrackBank.sceneBank().stop();
                  else
                     setBottomOverlay(mStopClipOverlay, data2 > 0, getButtonState(8, 0));
                  break;

               default:
               break;
            }
         }
      }
   }

   private final void setMode(final Mode mode)
   {
      mCurrentMode.deactivate();

      /* update current/previous */
      if (mode == mCurrentMode)
      {
         /* Set previous mode */
         mCurrentMode = mPreviousMode;
         mPreviousMode = mode;
      }
      else
      {
         /* set the new mode, and update previous mode */
         mPreviousMode = mCurrentMode;
         mCurrentMode = mode;
      }

      clearPads();

      mCurrentMode.activate();
      updateKeyTranslationTable();
   }

   private final void setBottomOverlay(final Overlay overlay, final boolean isPressed, final ButtonState buttonState)
   {
      if (isPressed)
         buttonState.onButtonPressed(mHost);

      if (mBottomOverlay != null)
         mBottomOverlay.deactivate();

      if (isPressed)
      {
         if (mBottomOverlay != overlay)
            mBottomOverlay = overlay;
         else
            mBottomOverlay = null;
      }
      else
      {
         if (mBottomOverlay == overlay && buttonState.mState == ButtonState.State.HOLD)
            mBottomOverlay = null;
      }

      if (mBottomOverlay != null)
         mBottomOverlay.activate();

      updateKeyTranslationTable();
   }

   void updateKeyTranslationTable()
   {
      final Integer[] table = FILTER_ALL_NOTE_MAP.clone();
      mCurrentMode.updateKeyTranslationTable(table);
      if (mBottomOverlay != null)
         mBottomOverlay.updateKeyTranslationTable(table);
      mNoteInput.setKeyTranslationTable(table);
   }

   private void clearPads()
   {
      for (int x = 0; x < 8; ++x)
         for (int y = 0; y < 8; ++y)
            getPadLed(x, y).setColor(0.f, 0.f, 0.f);
   }

   final Led getPadLed(final int x, final int y)
   {
      final int index = getPadIndex(x, y);
      return mButtonStates[index].getLed();
   }

   Led getTopLed(final int x)
   {
      return mButtonStates[getTopButtonIndex(x)].getLed();
   }

   Led getBottomLed(final int x)
   {
      return mButtonStates[getBottomButtonIndex(x)].getLed();
   }

   Led getLeftLed(final int y)
   {
      return mButtonStates[getLeftButtonIndex(y)].getLed();
   }

   Led getRightLed(final int y)
   {
      return mButtonStates[getRightButtonIndex(y)].getLed();
   }

   TrackBank getTrackBank()
   {
      return mTrackBank;
   }

   SceneBank getSceneBank()
   {
      return mSceneBank;
   }

   boolean isShiftOn()
   {
      return mIsShiftOn;
   }

   boolean isDeleteOn()
   {
      return mIsDeleteOn;
   }

   public boolean isQuantizeOn()
   {
      return mIsQuantizeOn;
   }

   CursorDevice getCursorDevice()
   {
      return mCursorDevice;
   }

   CursorTrack getCursorTrack()
   {
      return mCursorTrack;
   }

   public DrumPadBank getCursorTrackDrumPads()
   {
      return mCursorTrackDrumPads;
   }

   UserControlBank getUserControls()
   {
      return mUserControls;
   }

   MasterTrack getMasterTrack()
   {
      return mMasterTrack;
   }

   MusicalScale getMusicalScale()
   {
      return mMusicalScale;
   }

   void setMusicalScale(final MusicalScale musicalScale)
   {
      mMusicalScale = musicalScale;
      mHost.showPopupNotification("Using scale: " + musicalScale.getName());
      getMusicalScaleSetting().set(musicalScale.getName());
   }

   private SettableEnumValue getMusicalScaleSetting()
   {
      return mMusicalScaleSetting;
   }

   SettableEnumValue getMusicalKeySetting()
   {
      return mMusicalKeySetting;
   }

   int getMusicalKey()
   {
      return mMusicalKey;
   }

   void setMusicalKey(final int musicalKey)
   {
      mMusicalKey = musicalKey;
      final String keyName = KEY_NAMES[musicalKey];
      mHost.showPopupNotification("Using root key: " + keyName);
      getMusicalKeySetting().set(keyName);
   }

   NoteInput getNoteInput()
   {
      return mNoteInput;
   }

   Transport getTransport()
   {
      return mTransport;
   }

   void scheduleFlush()
   {
      mHost.requestFlush();
   }

   Application getApplication()
   {
      return mApplication;
   }

   Clip getCursorClip()
   {
      return mCursorClip;
   }

   CursorDevice getCursorClipDevice()
   {
      return mCursorClipDevice;
   }

   Track getCursorClipTrack()
   {
      return mCursorClipTrack;
   }

   DrumPadBank getCursorClipDrumPads()
   {
      return mCursorClipDrumPads;
   }

   Color getTrackColor(final int i)
   {
      return new Color(mTrackBank.getItemAt(i).color());
   }

   Color getCursorTrackColor()
   {
      return new Color(mCursorTrack.color());
   }

   CursorRemoteControlsPage getDrumPerfsRemoteControls()
   {
      return mDrumPerfsRemoteControls;
   }

   CursorRemoteControlsPage getDrumScenesRemoteControls()
   {
      return mDrumScenesRemoteControls;
   }

   boolean wantsSafePitches()
   {
      return mSafePitchesSetting.get();
   }

   boolean shouldHighlightScale()
   {
      return mHighlightScaleSetting.get();
   }

   boolean shouldHihlightRootKey()
   {
      return mHighlightRootKeySetting.get();
   }

   void setKeyboardLayout(final KeyboardLayout keyboardLayout)
   {
      mKeyboardLayout = keyboardLayout;
      mKeyboardLayoutSetting.set(keyboardLayout.toString());
      invalidateKeyboardModes();
   }

   KeyboardLayout getKeyboardLayout()
   {
      return mKeyboardLayout;
   }

   public Arpeggiator getArpeggiator()
   {
      return mArpeggiator;
   }

   /* API Objects */
   private final ControllerHost mHost;
   private Application mApplication;
   private Transport mTransport;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private NoteInput mNoteInput;
   private MasterTrack mMasterTrack;
   private TrackBank mTrackBank;
   private SceneBank mSceneBank;
   private CursorTrack mCursorTrack;
   private CursorDevice mCursorDevice;
   private UserControlBank mUserControls;
   private Project mProject;
   private DocumentState mDocumentState;
   private SettableEnumValue mMusicalKeySetting;
   private SettableEnumValue mMusicalScaleSetting;
   private Clip mCursorClip;
   private CursorDevice mCursorClipDevice;
   private Track mCursorClipTrack;
   private DrumPadBank mCursorClipDrumPads;
   private CursorRemoteControlsPage mDrumScenesRemoteControls;
   private CursorRemoteControlsPage mDrumPerfsRemoteControls;
   private Arpeggiator mArpeggiator;
   private ClipLauncherSlot mCursorClipSlot;
   private DrumPadBank mCursorTrackDrumPads;

   /* Modes and Overlay context */
   private Mode mCurrentMode;
   private Mode mPreviousMode;
   private Overlay mBottomOverlay;

   /* Modes */
   private final SessionMode mSessionMode;
   private final MultiplexerMode mPlayNoteModes;
   private final KeyboardMode mKeyboardMode;
   private final StepSequencerMode mStepSequencerMode;
   private final ScaleAndKeyChooserMode mScaleAndKeyChooserMode;
   private final DrumMode mDrumMode;
   private final DrumSequencerMode mDrumSequencerMode;
   private final VolumeMode mVolumeMode;
   private final SendMode mSendMode;
   private final PanMode mPanMode;

   /* Overlays */
   private final RecordArmOverlay mRecordArmOverlay;
   private final TrackSelectOverlay mTrackSelectOverlay;
   private final StopClipOverlay mStopClipOverlay;
   private final MuteOverlay mMuteOverlay;
   private final SoloOverlay mSoloOverlay;

   /* Button States */
   private final ButtonState[] mButtonStates = new ButtonState[100];
   private boolean mIsShiftOn = false;
   private boolean mIsDeleteOn;
   private boolean mIsQuantizeOn = false;

   /* Musical Context */
   private MusicalScale mMusicalScale = MusicalScaleLibrary.getInstance().getMusicalScale(0);
   private int mMusicalKey = 0; // 0: C, 2: D, and so on...
   private KeyboardLayout mKeyboardLayout = KeyboardLayout.GUITAR;

   /* Preferences */
   private SettableBooleanValue mSafePitchesSetting;
   private SettableBooleanValue mHighlightRootKeySetting;
   private SettableBooleanValue mHighlightScaleSetting;
   private SettableEnumValue mKeyboardLayoutSetting;

   /* Layers */
   private Layers mLayers;
   private Layer mMainLayer;
   private Layer mDebugLayer;

   /* Hardware Controls */
   private HardwareSurface mHardwareSurface;
   private LaunchpadButtonAndLed[] mGridButtons;
   private LaunchpadButtonAndLed[] mSceneButtons;
   private LaunchpadButtonAndLed mShiftButton;
   private LaunchpadButtonAndLed mClickButton;
   private LaunchpadButtonAndLed mUndoButton;
   private LaunchpadButtonAndLed mDeleteButton;
   private LaunchpadButtonAndLed mQuantizeButton;
   private LaunchpadButtonAndLed mDuplicateButton;
   private LaunchpadButtonAndLed mDoubleButton;
   private LaunchpadButtonAndLed mRecordButton;
   private LaunchpadButtonAndLed mUpButton;
   private LaunchpadButtonAndLed mDownButton;
   private LaunchpadButtonAndLed mLeftButton;
   private LaunchpadButtonAndLed mRightButton;
   private LaunchpadButtonAndLed mSessionButton;
   private LaunchpadButtonAndLed mNoteButton;
   private LaunchpadButtonAndLed mDeviceButton;
   private LaunchpadButtonAndLed mUserButton;
   private LaunchpadButtonAndLed mArmButton;
   private LaunchpadButtonAndLed mStopButton;
   private LaunchpadButtonAndLed mSendsButton;
   private LaunchpadButtonAndLed mPanButton;
   private LaunchpadButtonAndLed mVolumeButton;
   private LaunchpadButtonAndLed mSoloButton;
   private LaunchpadButtonAndLed mMuteButton;
   private LaunchpadButtonAndLed mSelectButton;
}
