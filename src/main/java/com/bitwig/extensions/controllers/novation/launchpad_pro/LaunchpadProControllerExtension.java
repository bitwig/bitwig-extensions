package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arpeggiator;
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
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
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
   }

   private void createLayers()
   {
      mLayers = new Layers(this);

      mMainLayer = new LaunchpadLayer(this, "main");

      mSessionMode = new SessionMode(this);
      mKeyboardMode = new KeyboardMode(this);
      mDrumMode = new DrumMode(this);
      mVolumeMode = new VolumeMode(this);
      mScaleAndKeyChooserMode = new ScaleAndKeyChooserMode(this);
      mSendsMode = new SendsMode(this);
      mPanMode = new PanMode(this);
      mDrumSequencerMode = new DrumSequencerMode(this);
      mStepSequencerMode = new StepSequencerMode(this);

      mPlayModes = new MultiplexerMode(this, "Play");
      mPlayModes.setMode(0, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.GUITAR));
      mPlayModes.setMode(1, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.LINE_3));
      mPlayModes.setMode(2, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.LINE_7));
      mPlayModes.setMode(3, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.PIANO));
      mPlayModes.setMode(4, mDrumMode);
      mPlayModes.setMode(7, mScaleAndKeyChooserMode);

      mRecordArmOverlay = new RecordArmOverlay(this);
      mTrackSelectOverlay = new TrackSelectOverlay(this);
      mMuteOverlay = new MuteOverlay(this);
      mStopClipOverlay = new StopClipOverlay(this);
      mSoloOverlay = new SoloOverlay(this);

      mCurrentMode = mSessionMode;

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
      if (false)
      {
         for (int x = 0; x < 8; ++x)
         {
            for (int y = 0; y < 8; ++y)
            {
               final int X = x;
               final int Y = y;
               final Button bt = mGridButtons[y * 8 + x];

               mMainLayer.bindPressed(bt.getButton(), v -> {
                  bt.onButtonPressed(mHost);

                  final int velocity = (int) (v * 127.0);

                  if (Y == 0 && mBottomOverlay != null)
                     mBottomOverlay.onPadPressed(Y, velocity);
                  else
                     mCurrentMode.onPadPressed(X, Y, velocity);
               });

               mMainLayer.bindReleased(bt.getButton(), v -> {
                  final boolean wasHeld = bt.getButtonState() == Button.State.HOLD;
                  bt.onButtonReleased();

                  final int velocity = (int) (v * 127.0);

                  if (Y == 0 && mBottomOverlay != null)
                     mBottomOverlay.onPadReleased(Y, velocity);
                  else
                     mCurrentMode.onPadReleased(X, Y, velocity, wasHeld);
               });

               mMainLayer.bind(bt.getAfterTouch(), v -> mCurrentMode.onPadPressure(X, Y, (int) (127. * v)));
               mMainLayer.bindLightState(LedState.OFF, bt);
            }
         }

         for (int y = 0; y < 8; ++y)
         {
            final int Y = y;
            final Button sceneButton = mSceneButtons[y];
            final HardwareButton bt = sceneButton.getButton();
            mMainLayer.bindPressed(bt, () -> mCurrentMode.onSceneButtonPressed(Y));
            mMainLayer.bindLightState(LedState.OFF, sceneButton);
         }

         mMainLayer.bindPressed(mUpButton.getButton(), () -> mCurrentMode.onArrowUpPressed());
         mMainLayer.bindReleased(mUpButton.getButton(), () -> mCurrentMode.onArrowUpReleased());
         mMainLayer.bindPressed(mDownButton.getButton(), () -> mCurrentMode.onArrowDownPressed());
         mMainLayer.bindReleased(mDownButton.getButton(), () -> mCurrentMode.onArrowDownReleased());
         mMainLayer.bindPressed(mLeftButton.getButton(), () -> mCurrentMode.onArrowLeftPressed());
         mMainLayer.bindReleased(mLeftButton.getButton(), () -> mCurrentMode.onArrowLeftReleased());
         mMainLayer.bindPressed(mRightButton.getButton(), () -> mCurrentMode.onArrowRightPressed());
         mMainLayer.bindReleased(mRightButton.getButton(), () -> mCurrentMode.onArrowRightReleased());
      }

      mMainLayer.bindPressed(mShiftButton.getButton(), () -> mCurrentMode.onShiftPressed());
      mMainLayer.bindReleased(mShiftButton.getButton(), () -> mCurrentMode.onShiftReleased());
      mMainLayer.bindPressed(mClickButton.getButton(), () -> {
         if (isShiftOn())
            mTransport.tapTempo();
         else
            mTransport.isMetronomeEnabled().toggle();
      });
      mMainLayer.bindPressed(mUndoButton.getButton(), () -> {
         if (isShiftOn())
            mApplication.redo();
         else
            mApplication.undo();
      });
      mMainLayer.bindPressed(mDeleteButton.getButton(), () -> mCurrentMode.onDeletePressed());
      mMainLayer.bindPressed(mQuantizeButton.getButton(), () -> {
         if (isShiftOn())
         {
            final SettableEnumValue recordQuantizationGrid = mApplication.recordQuantizationGrid();
            recordQuantizationGrid.set(recordQuantizationGrid.get().equals("OFF") ? "1/16" : "OFF");
         }
         else
            mCurrentMode.onQuantizePressed();
      });
      mMainLayer.bindPressed(mDuplicateButton.getButton(), mApplication.duplicateAction());
      mMainLayer.bindPressed(mDoubleButton.getButton(), () -> {
         if (isShiftOn())
            mTransport.isArrangerRecordEnabled().toggle();
         else
            mTransport.togglePlay();
      });
      mMainLayer.bindPressed(mRecordButton.getButton(), () -> {
         final boolean enabled = isRecording();
         mTransport.isClipLauncherOverdubEnabled().set(!enabled);
         mTransport.isClipLauncherAutomationWriteEnabled().set(!enabled);
      });

      mMainLayer.bindOverlay(mArmButton, mRecordArmOverlay, LedState.REC_OFF);
      mMainLayer.bindOverlay(mSelectButton, mTrackSelectOverlay, LedState.TRACK_LOW);
      mMainLayer.bindOverlay(mMuteButton, mMuteOverlay, LedState.MUTE_LOW);
      mMainLayer.bindOverlay(mSoloButton, mSoloOverlay, LedState.SOLO_LOW);
      mMainLayer.bindOverlay(mStopButton, mStopClipOverlay, LedState.STOP_CLIP_OFF);
      mMainLayer.bindMode(mSessionButton, mSessionMode, LedState.SESSION_MODE_OFF);
      mMainLayer.bindMode(mNoteButton, mPlayModes, LedState.PLAY_MODE_OFF);
      mMainLayer.bindMode(mDeviceButton, mDrumSequencerMode, LedState.DRUM_SEQ_MODE_OFF);
      mMainLayer.bindMode(mUserButton, mStepSequencerMode, LedState.STEP_SEQ_MODE_OFF);
      mMainLayer.bindMode(mVolumeButton, mVolumeMode, LedState.VOLUME_MODE_LOW);
      mMainLayer.bindMode(mPanButton, mPanMode, LedState.PAN_MODE_LOW);
      mMainLayer.bindMode(mSendsButton, mSendsMode, LedState.SENDS_MODE_LOW);

      mMainLayer.bindLightState(LedState.OFF, mUpButton);
      mMainLayer.bindLightState(LedState.OFF, mDownButton);
      mMainLayer.bindLightState(LedState.OFF, mLeftButton);
      mMainLayer.bindLightState(LedState.OFF, mRightButton);
      mMainLayer.bindLightState(LedState.SHIFT_OFF, mShiftButton);
      mMainLayer.bindLightState(() -> mTransport.isMetronomeEnabled().get() ? LedState.CLICK_ON : LedState.CLICK_OFF, mClickButton);
      mMainLayer.bindLightState(LedState.UNDO_ON, mUndoButton);
      mMainLayer.bindLightState(LedState.DELETE_ON, mDeleteButton);
      mMainLayer.bindLightState(LedState.QUANTIZE_ON, mQuantizeButton);
      mMainLayer.bindLightState(LedState.DUPLICATE_ON, mDuplicateButton);
      mMainLayer.bindLightState(() -> {
         if (isShiftOn())
            return mTransport.isArrangerRecordEnabled().get() ? LedState.REC_ON : LedState.REC_OFF;
         else
            return mTransport.isPlaying().get() ? LedState.PLAY_ON : LedState.PLAY_OFF;
      }, mDoubleButton);
      mMainLayer.bindLightState(() -> isRecording() ? LedState.REC_ON : LedState.REC_OFF, mRecordButton);

      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(PHYSICAL_DEVICE_WIDTH, PHYSICAL_DEVICE_WIDTH);

      mShiftButton = createSideButton("shift", 0, 8);
      mShiftButton.getButton().isPressed().markInterested();
      mClickButton = createSideButton("click", 0, 7);
      mUndoButton = createSideButton("undo", 0, 6);
      mDeleteButton = createSideButton("delete", 0, 5);
      mDeleteButton.getButton().isPressed().markInterested();
      mQuantizeButton = createSideButton("quantize", 0, 4);
      mQuantizeButton.getButton().isPressed().markInterested();
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

      mSceneButtons = new Button[8];
      for (int y = 0; y < 8; ++y)
         mSceneButtons[y] = createSideButton("scene-" + y, 9, y + 1);

      mGridButtons = new Button[8 * 8];
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 8; ++y)
         {
            final int index = (x + 1) + 10 * (y + 1);
            final String id = "grid-" + x + "-" + y;

            final Button bt =
               new Button(this, id, mMidiIn, index, true, x + 1, y + 1);
            mGridButtons[8 * y + x] = bt;
            setButtonPhysicalPosition(bt, x + 1, y + 1, false);
         }
      }
   }

   private Button createSideButton(final String id, final int x, final int y)
   {
      assert x >= 1 && x < 9 && (y == 0 || y == 9);
      assert y >= 1 && y < 9 && (x == 0 || x == 9);

      final int index = x + 10 * y;
      final Button bt =
         new Button(this, id, mMidiIn, index, false, x, y);
      setButtonPhysicalPosition(bt, x, y, true);
      return bt;
   }

   private void setButtonPhysicalPosition(final Button bt, final int x, final int y, final boolean isRound)
   {
      assert x >= 0 && x < 10;
      assert y >= 0 && y < 10;

      final HardwareButton button = bt.getButton();
      button.setBounds(
         calculatePhysicalPosition(x),
         calculatePhysicalPosition(9 - y),
         PHYSICAL_BUTTON_WIDTH,
         PHYSICAL_BUTTON_WIDTH);

      if (isRound)
         button.setRoundedCornerRadius(PHYSICAL_BUTTON_WIDTH / 2);
   }

   private double calculatePhysicalPosition(final int i)
   {
      return PHYSICAL_BUTTON_OFFSET + i * (PHYSICAL_BUTTON_WIDTH + PHYSICAL_BUTTON_SPACE);
   }

   List<Button> findPadsInHoldState()
   {
      final ArrayList<Button> buttons = new ArrayList<>();
      for (Button bt : mGridButtons)
         if (bt.getButtonState() == Button.State.HOLD)
            buttons.add(bt);

      return buttons;
   }

   private int getButtonIndex(final int x, final int y)
   {
      assert 0 <= x && x <= 9;
      assert 0 <= y && y <= 9;

      return x + y * 10;
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
      mCursorClip.exists().addValueObserver(exists -> mCurrentMode.onCursorClipExists(exists));
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
      mCursorClipDrumPads.scrollPosition().addValueObserver(exists -> mDrumSequencerMode.invalidateDrumPosition(exists));
      mCursorClipDrumPads.scrollPosition().markInterested();

      for (int i = 0; i < 16; ++i)
      {
         initDrumPad(mCursorClipDrumPads.getItemAt(i));
         initDrumPad(mCursorTrackDrumPads.getItemAt(i));
      }

      initMidi();
      initTrackBank();
      initDocumentMusicalInfo();
      initPreferences();

      createHardwareControls();
      createLayers();

      mSessionMode.activate();
      mPlayModes.selectMinorMode(0);
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
         channel.isStopped().markInterested();
         channel.isQueuedForStop().markInterested();

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

   public void updateButtonLed(final Button button)
   {
      button.appendLedUpdate(mLedClearSysexBuffer, mLedColorUpdateSysexBuffer, mLedPulseUpdateSysexBuffer);

      // Lets not send sysex that are too big
      if (mLedColorUpdateSysexBuffer.length() >= 4 * 3 * 48)
      {
         sendLedUpdateSysex(mLedColorUpdateSysexBuffer.toString());
         mLedColorUpdateSysexBuffer.setLength(0); // clears it
      }
   }

   @Override
   public void flush()
   {
      mLedClearSysexBuffer.setLength(0);
      mLedColorUpdateSysexBuffer.setLength(0);
      mLedPulseUpdateSysexBuffer.setLength(0);

      mHardwareSurface.updateHardware();

      if (mLedClearSysexBuffer.length() > 0)
         mMidiOut.sendSysex("F0 00 20 29 02 10 0A" + mLedClearSysexBuffer + " F7");

      if (mLedColorUpdateSysexBuffer.length() > 0)
         sendLedUpdateSysex(mLedColorUpdateSysexBuffer.toString());

      if (mLedPulseUpdateSysexBuffer.length() > 0)
         mMidiOut.sendSysex("F0 00 20 29 02 10 28" + mLedPulseUpdateSysexBuffer + " F7");
   }

   private void paint()
   {
      paintLeftLeds();
      mCurrentMode.paint();
      if (mBottomOverlay != null)
         mBottomOverlay.paint();
   }

   private void paintLeftLeds()
   {
      mShiftButton.setColor(Color.WHITE);
      mClickButton.setColor(mTransport.isMetronomeEnabled().get() ? Color.YELLOW : Color.YELLOW_LOW);
      mUndoButton.setColor(Color.ORANGE);
      mDeleteButton.setColor(Color.ORANGE);
      mQuantizeButton.setColor(Color.CYAN);
      mDuplicateButton.setColor(Color.CYAN);
      if (isShiftOn())
         mDoubleButton.setColor(mTransport.isArrangerRecordEnabled().get() ? Color.RED : Color.RED_LOW); // Arranger Record
      else
         mDoubleButton.setColor(mTransport.isPlaying().get() ? Color.GREEN : Color.GREEN_LOW); // Tranport Play
      mRecordButton.setColor(isRecording() ? Color.RED : Color.RED_LOW); // Clip Launcher Record
   }

   private boolean isRecording()
   {
      return mTransport.isClipLauncherOverdubEnabled().get() |
         mTransport.isClipLauncherAutomationWriteEnabled().get();
   }

   final void setMode(final Mode mode)
   {
      assert mode != null;

      if (mCurrentMode == mode)
         return;

      mCurrentMode.deactivate();
      mCurrentMode = mode;
      mCurrentMode.activate();

      updateKeyTranslationTable();
   }

   final void setBottomOverlay(final Overlay overlay, final boolean isPressed, final Button bt)
   {
      assert overlay != null;

      if (isPressed)
         bt.onButtonPressed(mHost);

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
         if (mBottomOverlay == overlay && bt.getButtonState() == Button.State.HOLD)
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
            getPadButton(x, y).setColor(0.f, 0.f, 0.f);
   }

   /**
    * x and y must be in the top left coords (natural coords).
    */
   final Button getPadButton(final int x, final int y)
   {
      assert x >= 0 && x < 8;
      assert y >= 0 && y < 8;

      return mGridButtons[8 * y + x];
   }

   Button getButtonOnTheTop(final int x)
   {
      switch (x)
      {
         case 0:
            return mUpButton;
         case 1:
            return mDownButton;
         case 2:
            return mLeftButton;
         case 3:
            return mRightButton;
         case 4:
            return mSessionButton;
         case 5:
            return mNoteButton;
         case 6:
            return mDeviceButton;
         case 7:
            return mUserButton;
         default:
            throw new IllegalStateException();
      }
   }

   Button getButtonOnTheBottom(final int x)
   {
      switch (x)
      {
         case 0:
            return mArmButton;
         case 1:
            return mSelectButton;
         case 2:
            return mMuteButton;
         case 3:
            return mSoloButton;
         case 4:
            return mVolumeButton;
         case 5:
            return mPanButton;
         case 6:
            return mSendsButton;
         case 7:
            return mStopButton;
         default:
            throw new IllegalStateException();
      }
   }

   Button getButtonOnTheLeft(final int y)
   {
      switch (y)
      {
         case 0:
            return mShiftButton;
         case 1:
            return mClickButton;
         case 2:
            return mUndoButton;
         case 3:
            return mDeleteButton;
         case 4:
            return mQuantizeButton;
         case 5:
            return mDuplicateButton;
         case 6:
            return mDoubleButton;
         case 7:
            return mRecordButton;
         default:
            throw new IllegalStateException();
      }
   }

   Button getButtonOnTheRight(final int y)
   {
      return mSceneButtons[y];
   }

   public Button getUpButton()
   {
      return mUpButton;
   }

   public Button getDownButton()
   {
      return mDownButton;
   }

   public Button getLeftButton()
   {
      return mLeftButton;
   }

   public Button getRightButton()
   {
      return mRightButton;
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
      return mShiftButton.getButton().isPressed().get();
   }

   boolean isDeleteOn()
   {
      return mDeleteButton.getButton().isPressed().get();
   }

   public boolean isQuantizeOn()
   {
      return mQuantizeButton.getButton().isPressed().get();
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

   public Layers getLayers()
   {
      return mLayers;
   }

   public Button getShiftButton()
   {
      return mShiftButton;
   }

   public Button getPanButton()
   {
      return mPanButton;
   }

   public Button getSendsButton()
   {
      return mSendsButton;
   }

   public Button getVolumeButton()
   {
      return mVolumeButton;
   }

   public Button getSceneButton(int y)
   {
      return mSceneButtons[y];
   }

   public Button getRecordButton()
   {
      return mRecordButton;
   }

   public Button getArmButton()
   {
      return mArmButton;
   }

   public Button getMuteButton()
   {
      return mMuteButton;
   }

   public Button getStopButton()
   {
      return mStopButton;
   }

   public Button getSelectButton()
   {
      return mSelectButton;
   }

   public Button getDeleteButton()
   {
      return mDeleteButton;
   }

   public Button getSoloButton()
   {
      return mSoloButton;
   }

   public Button getQuantizeButton()
   {
      return mQuantizeButton;
   }

   public HardwareSurface getHardwareSurface()
   {
      return mHardwareSurface;
   }

   public Button getSessionButton()
   {
      return mSessionButton;
   }

   public Button getDeviceButton()
   {
      return mDeviceButton;
   }

   public Button getNoteButton()
   {
      return mNoteButton;
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
   private Overlay mBottomOverlay;

   /* Modes */
   private SessionMode mSessionMode;
   private MultiplexerMode mPlayModes;
   private KeyboardMode mKeyboardMode;
   private StepSequencerMode mStepSequencerMode;
   private ScaleAndKeyChooserMode mScaleAndKeyChooserMode;
   private DrumMode mDrumMode;
   private DrumSequencerMode mDrumSequencerMode;
   private VolumeMode mVolumeMode;
   private SendsMode mSendsMode;
   private PanMode mPanMode;

   /* Overlays */
   private RecordArmOverlay mRecordArmOverlay;
   private TrackSelectOverlay mTrackSelectOverlay;
   private StopClipOverlay mStopClipOverlay;
   private MuteOverlay mMuteOverlay;
   private SoloOverlay mSoloOverlay;

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
   private LaunchpadLayer mMainLayer;
   private Layer mDebugLayer;

   /* Hardware Controls */
   private HardwareSurface mHardwareSurface;
   private Button[] mGridButtons;
   private Button[] mSceneButtons;
   private Button mShiftButton;
   private Button mClickButton;
   private Button mUndoButton;
   private Button mDeleteButton;
   private Button mQuantizeButton;
   private Button mDuplicateButton;
   private Button mDoubleButton;
   private Button mRecordButton;
   private Button mUpButton;
   private Button mDownButton;
   private Button mLeftButton;
   private Button mRightButton;
   private Button mSessionButton;
   private Button mNoteButton;
   private Button mDeviceButton;
   private Button mUserButton;
   private Button mArmButton;
   private Button mStopButton;
   private Button mSendsButton;
   private Button mPanButton;
   private Button mVolumeButton;
   private Button mSoloButton;
   private Button mMuteButton;
   private Button mSelectButton;

   private StringBuilder mLedClearSysexBuffer = new StringBuilder();
   private StringBuilder mLedColorUpdateSysexBuffer = new StringBuilder();
   private StringBuilder mLedPulseUpdateSysexBuffer = new StringBuilder();
}
