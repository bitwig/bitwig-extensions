package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arpeggiator;
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
import com.bitwig.extension.controller.api.NoteLatch;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.UserControlBank;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;

final class LaunchpadProControllerExtension extends ControllerExtension
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

   private final double PHYSICAL_DEVICE_WIDTH = 260;
   private final double PHYSICAL_BUTTON_WIDTH = 20;
   private final double PHYSICAL_BUTTON_SPACE = 4;
   private final double PHYSICAL_BUTTON_OFFSET = 12;

   private static final String[] KEY_NAMES = new String[]{ "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

   public LaunchpadProControllerExtension(final LaunchpadProControllerExtensionDefinition driverDefinition, final ControllerHost host)
   {
      super(driverDefinition, host);
   }

   private void createLayers()
   {
      mLayers = new Layers(this);

      mMainLayer = new LaunchpadLayer(this, "main");

      mSessionMode = new SessionMode(this);
      mKeyboardMode = new KeyboardMode(this);
      final DrumMode drumMode = new DrumMode(this);
      mVolumeMode = new VolumeMode(this);
      final ScaleAndKeyChooserMode scaleAndKeyChooserMode = new ScaleAndKeyChooserMode(this);
      mSendsMode = new SendsMode(this);
      mPanMode = new PanMode(this);
      mDrumSequencerMode = new DrumSequencerMode(this);
      mStepSequencerMode = new StepSequencerMode(this);

      mPlayModes = new MultiplexerMode(this, "Play");
      mPlayModes.setMode(0, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.GUITAR));
      mPlayModes.setMode(1, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.LINE_3));
      mPlayModes.setMode(2, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.LINE_7));
      mPlayModes.setMode(3, mKeyboardMode, () -> setKeyboardLayout(KeyboardLayout.PIANO));
      mPlayModes.setMode(4, drumMode);
      mPlayModes.setMode(7, scaleAndKeyChooserMode);

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
      final Layer debugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
      debugLayer.setShouldReplaceBindingsInLayersBelow(false);
      //debugLayer.activate();
   }

   private void createMainLayer()
   {
      mMainLayer.bindPressed(mClickButton.mButton, () -> {
         if (isShiftOn())
            mTransport.tapTempo();
         else
            mTransport.isMetronomeEnabled().toggle();
      });
      mMainLayer.bindPressed(mUndoButton.mButton, () -> {
         if (isShiftOn())
            mApplication.redo();
         else
            mApplication.undo();
      });
      mMainLayer.bindPressed(mQuantizeButton.mButton, () -> {
         if (isShiftOn())
         {
            final SettableEnumValue recordQuantizationGrid = mApplication.recordQuantizationGrid();
            recordQuantizationGrid.set(recordQuantizationGrid.get().equals("OFF") ? "1/16" : "OFF");
         }
      });
      mMainLayer.bindPressed(mDuplicateButton.mButton, mApplication.duplicateAction());
      mMainLayer.bindPressed(mDoubleButton.mButton, () -> {
         if (isShiftOn())
            mTransport.isArrangerRecordEnabled().toggle();
         else
            mTransport.togglePlay();
      });
      mMainLayer.bindPressed(mRecordButton.mButton, () -> {
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
      mMainLayer.bindLightState(() -> isShiftOn() ? LedState.SHIFT_ON : LedState.SHIFT_OFF, mShiftButton);
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

      /* Needed by the main layer */
      mTransport.isMetronomeEnabled().subscribe();
      mTransport.isPlaying().subscribe();
      mTransport.isClipLauncherOverdubEnabled().subscribe();
      mTransport.isClipLauncherAutomationWriteEnabled().subscribe();
      mTransport.isArrangerRecordEnabled().subscribe();
   }

   private void createHardwareControls()
   {
      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(PHYSICAL_DEVICE_WIDTH, PHYSICAL_DEVICE_WIDTH);

      mShiftButton = createSideButton("shift", 0, 8);
      mShiftButton.mButton.isPressed().markInterested();
      mClickButton = createSideButton("click", 0, 7);
      mUndoButton = createSideButton("undo", 0, 6);
      mDeleteButton = createSideButton("delete", 0, 5);
      mDeleteButton.mButton.isPressed().markInterested();
      mQuantizeButton = createSideButton("quantize", 0, 4);
      mQuantizeButton.mButton.isPressed().markInterested();
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
      assert (x >= 1 && x < 9 && (y == 0 || y == 9)) || (y >= 1 && y < 9 && (x == 0 || x == 9));

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

      final HardwareButton button = bt.mButton;
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
      for (final Button bt : mGridButtons)
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
      final ControllerHost host = getHost();

      try
      {
         Thread.sleep(4000);
      }
      catch (final InterruptedException e)
      {
         host.println(e.toString());
      }

      mApplication = host.createApplication();
      mApplication.recordQuantizationGrid().markInterested();

      final Project project = host.getProject();
      mDocumentState = host.getDocumentState();

      mUserControls = host.createUserControls(64);
      mMasterTrack = host.createMasterTrack(8);
      mTransport = host.createTransport();
      mTransport.isClipLauncherAutomationWriteEnabled().markInterested();
      mTransport.isClipLauncherOverdubEnabled().markInterested();
      mTransport.isMetronomeEnabled().markInterested();
      mTransport.tempo().value().markInterested();
      mTransport.isPlaying().markInterested();
      mTransport.isArrangerRecordEnabled().markInterested();

      mCursorTrack = host.createCursorTrack(8, 0);
      mCursorTrack.color().markInterested();
      mCursorTrack.hasPrevious().markInterested();
      mCursorTrack.hasNext().markInterested();
      mCursorTrack.playingNotes().markInterested();

      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorDevice.hasDrumPads().markInterested();
      mDrumPadBank = mCursorDevice.createDrumPadBank(16);
      mDrumPadBank.setIndication(false);
      mDrumPadBank.exists().markInterested();
      mDrumPadBank.scrollPosition().markInterested();

      mCursorClip = mCursorTrack.createLauncherCursorClip("launchpad-pro", "Launchpad Pro", 32, 128);
      mCursorClip.exists().markInterested();
      mCursorClip.exists().addValueObserver(exists -> mCurrentMode.onCursorClipExists(exists));
      mCursorClip.hasPrevious().markInterested();
      mCursorClip.hasNext().markInterested();
      mCursorClip.color().markInterested();
      mCursorClip.getPlayStart().markInterested();
      mCursorClip.getPlayStop().markInterested();
      mCursorClip.getLoopStart().markInterested();
      mCursorClip.getLoopLength().markInterested();
      mCursorClip.playingStep().markInterested();
      final ClipLauncherSlot cursorClipSlot = mCursorClip.clipLauncherSlot();
      cursorClipSlot.sceneIndex().markInterested();

      mDrumScenesRemoteControls = mCursorDevice.createCursorRemoteControlsPage("scenes", 8, "drum-scenes");
      mDrumPerfsRemoteControls = mCursorDevice.createCursorRemoteControlsPage("perfs", 8, "drum-perfs");
      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl sceneParam = mDrumScenesRemoteControls.getParameter(i);
         sceneParam.exists().markInterested();
         sceneParam.value().markInterested();

         final RemoteControl perfParam = mDrumPerfsRemoteControls.getParameter(i);
         perfParam.exists().markInterested();
         perfParam.value().markInterested();
      }

      for (int i = 0; i < 16; ++i)
         initDrumPad(mDrumPadBank.getItemAt(i));

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
      final Preferences preferences = getHost().getPreferences();

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

      final String ARP_CATEGORY = "Arpeggiator";

      mArpModeSetting = mDocumentState.getEnumSetting("Mode", ARP_CATEGORY, mArpeggiator.mode().enumDefinition(), "flow");
      mArpModeSetting.markInterested();

      mArpOctaveSetting = mDocumentState.getNumberSetting("Octave", ARP_CATEGORY, 0, 4, 1, "", 1);
      mArpOctaveSetting.markInterested();
   }

   private void initMidi()
   {
      final ControllerHost host = getHost();
      mMidiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      mNoteInput = mMidiIn.createNoteInput("Input", "??????");
      mNoteInput.setShouldConsumeEvents(false);
      mNoteInput.setKeyTranslationTable(FILTER_ALL_NOTE_MAP);
      mNoteInput.includeInAllInputs().markInterested();

      mNoteLatch = mNoteInput.noteLatch();
      mNoteLatch.isEnabled().markInterested();
      mNoteLatch.mono().markInterested();
      mNoteLatch.mode().markInterested();

      mArpeggiator = mNoteInput.arpeggiator();
      mArpeggiator.gateLength().markInterested();
      mArpeggiator.rate().markInterested();
      mArpeggiator.isEnabled().markInterested();
      mArpeggiator.octaves().markInterested();
      mArpeggiator.mode().markInterested();
      mArpeggiator.usePressureToVelocity().markInterested();
      mArpeggiator.shuffle().markInterested();
      mArpeggiator.isFreeRunning().markInterested();

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
      mTrackBank = getHost().createTrackBank(8, 8, 8, true);
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

   public int getFlushIteration()
   {
      return mFlushIteration;
   }

   @Override
   public void flush()
   {
      ++mFlushIteration;
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
         bt.onButtonPressed(getHost());

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

   /**
    * x and y must be in the top left coords (natural coords).
    */
   final Button getPadButton(final int x, final int y)
   {
      assert x >= 0 && x < 8;
      assert y >= 0 && y < 8;

      return mGridButtons[8 * y + x];
   }

   boolean isShiftOn()
   {
      return mShiftButton.mButton.isPressed().get();
   }

   boolean isDeleteOn()
   {
      return mDeleteButton.mButton.isPressed().get();
   }

   public boolean isQuantizeOn()
   {
      return mQuantizeButton.mButton.isPressed().get();
   }

   MusicalScale getMusicalScale()
   {
      return mMusicalScale;
   }

   void setMusicalScale(final MusicalScale musicalScale)
   {
      mMusicalScale = musicalScale;
      getHost().showPopupNotification("Using scale: " + musicalScale.getName());
      mMusicalScaleSetting.set(musicalScale.getName());
   }

   int getMusicalKey()
   {
      return mMusicalKey;
   }

   void setMusicalKey(final int musicalKey)
   {
      mMusicalKey = musicalKey;
      final String keyName = KEY_NAMES[musicalKey];
      getHost().showPopupNotification("Using root key: " + keyName);
      mMusicalKeySetting.set(keyName);
   }

   void scheduleFlush()
   {
      getHost().requestFlush();
   }

   Color getTrackColor(final int i)
   {
      return new Color(mTrackBank.getItemAt(i).color());
   }

   Color getCursorTrackColor()
   {
      return new Color(mCursorTrack.color());
   }

   private void setKeyboardLayout(final KeyboardLayout keyboardLayout)
   {
      mKeyboardLayout = keyboardLayout;
      mKeyboardLayoutSetting.set(keyboardLayout.toString());
      invalidateKeyboardModes();
   }

   KeyboardLayout getKeyboardLayout()
   {
      return mKeyboardLayout;
   }

   public Layers getLayers()
   {
      return mLayers;
   }

   /* API Objects */
   Application mApplication;
   Transport mTransport;
   MidiIn mMidiIn;
   MidiOut mMidiOut;
   NoteInput mNoteInput;
   MasterTrack mMasterTrack;
   TrackBank mTrackBank;
   SceneBank mSceneBank;
   CursorTrack mCursorTrack;
   CursorDevice mCursorDevice;
   UserControlBank mUserControls;
   DocumentState mDocumentState;
   PinnableCursorClip mCursorClip;
   CursorRemoteControlsPage mDrumScenesRemoteControls;
   CursorRemoteControlsPage mDrumPerfsRemoteControls;
   Arpeggiator mArpeggiator;
   NoteLatch mNoteLatch;
   DrumPadBank mDrumPadBank;

   /* Settings */
   SettableEnumValue mMusicalKeySetting;
   SettableEnumValue mMusicalScaleSetting;
   SettableEnumValue mArpModeSetting;
   SettableRangedValue mArpOctaveSetting;

   /* Modes and Overlay context */
   private Mode mCurrentMode;
   private Overlay mBottomOverlay;

   /* Modes */
   private SessionMode mSessionMode;
   private MultiplexerMode mPlayModes;
   private KeyboardMode mKeyboardMode;
   private StepSequencerMode mStepSequencerMode;
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
   SettableBooleanValue mSafePitchesSetting;
   SettableBooleanValue mHighlightRootKeySetting;
   SettableBooleanValue mHighlightScaleSetting;
   SettableEnumValue mKeyboardLayoutSetting;

   /* Layers */
   private Layers mLayers;
   private LaunchpadLayer mMainLayer;

   /* Hardware Controls */
   HardwareSurface mHardwareSurface;
   Button[] mGridButtons;
   Button[] mSceneButtons;
   Button mShiftButton;
   Button mClickButton;
   Button mUndoButton;
   Button mDeleteButton;
   Button mQuantizeButton;
   Button mDuplicateButton;
   Button mDoubleButton;
   Button mRecordButton;
   Button mUpButton;
   Button mDownButton;
   Button mLeftButton;
   Button mRightButton;
   Button mSessionButton;
   Button mNoteButton;
   Button mDeviceButton;
   Button mUserButton;
   Button mArmButton;
   Button mStopButton;
   Button mSendsButton;
   Button mPanButton;
   Button mVolumeButton;
   Button mSoloButton;
   Button mMuteButton;
   Button mSelectButton;

   /* Used to cache complex computed values during the flush required for painting */
   private int mFlushIteration = 0;

   /* Sysex buffer for flushing */
   private final StringBuilder mLedClearSysexBuffer = new StringBuilder();
   private final StringBuilder mLedColorUpdateSysexBuffer = new StringBuilder();
   private final StringBuilder mLedPulseUpdateSysexBuffer = new StringBuilder();
}
