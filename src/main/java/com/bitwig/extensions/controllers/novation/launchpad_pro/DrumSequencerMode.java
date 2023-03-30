package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.util.NoteInputUtils;

final class DrumSequencerMode extends AbstractSequencerMode
{
   DrumSequencerMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "drum-sequencer");

      final PinnableCursorClip cursorClip = driver.mCursorClip;

      mShiftLayer = new LaunchpadLayer(driver, "drum-sequencer-shift");
      mDrumPadsLayer = new LaunchpadLayer(driver, "drum-pads");
      mMainActionsLayer = new LaunchpadLayer(driver, "drum-main-actions");
      mSceneAndPerfsLayer = new LaunchpadLayer(driver, "drum-scenes-and-perfs");
      mMixDataLayer = new LaunchpadLayer(driver, "drum-seq-mix-data");
      mSoundDataLayer = new LaunchpadLayer(driver, "drum-seq-sound-data");

      bindLightState(LedState.DRUM_SEQ_MODE, driver.mDeviceButton);

      // Step sequencer
      for (int y = 0; y < 4; ++y)
      {
         for (int x = 0; x < 8; ++x)
         {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, y + 4);
            final int clipStepIndex = calculateClipStepIndex(x, 3 - y);
            bindPressed(bt, v -> {
               bt.onButtonPressed(driver.getHost());
               onStepPressed(clipStepIndex, (int) (v * 127.0));
            });
            bindReleased(bt, () -> {
               final boolean wasHeld = bt.getButtonState() == Button.State.HOLD;
               bt.onButtonReleased();
               onStepReleased(clipStepIndex, wasHeld);
            });
            bindLightState(() -> computeStepSeqLedState(X, 3 - Y), bt);
         }

         final Button sceneButton = driver.mSceneButtons[y + 4];
         final int page = 3 - y;
         final int Y = y;
         bindPressed(sceneButton, () -> {
            mPage = page;
            cursorClip.scrollToStep(32 * page);
         });
         bindLightState(() -> computePatternOffsetLedState(3 - Y), sceneButton);

         final Button dataChoiceBt = driver.mSceneButtons[y];
         bindPressed(dataChoiceBt, () -> setDataMode(Y));
         bindLightState(() -> computeDataChoiceLedState(Y), dataChoiceBt);
      }

      // Scene buttons in shift layer
      for (int y = 0; y < 8; ++y)
      {
         final Button sceneButton = driver.mSceneButtons[y];
         final int page = 8 - y;
         final int Y = y;
         mShiftLayer.bindPressed(sceneButton, () -> setClipLength(page * 4));
         mShiftLayer.bindLightState(() -> computeClipLengthSelectionLedState(7 - Y), sceneButton);
      }

      final SettableIntegerValue drumPosition = driver.mDrumPadBank.scrollPosition();
      mShiftLayer.bindPressed(driver.mUpButton, this::drumPadsUp);
      mShiftLayer.bindPressed(driver.mDownButton, this::drumpPadsDown);
      mShiftLayer.bindLightState(() -> drumPosition.get() < 116 ? LedState.PITCH : LedState.PITCH_LOW, driver.mUpButton);
      mShiftLayer.bindLightState(() -> drumPosition.get() > 0 ? LedState.PITCH : LedState.PITCH_LOW, driver.mDownButton);

      // Drum Pads
      for (int x = 0; x < 4; ++x)
      {
         for (int y = 0; y < 4; ++y)
         {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, y);
            mDrumPadsLayer.bindPressed(bt, () -> onDrumPadPressed(X, Y));
            mDrumPadsLayer.bindLightState(() -> computeDrumPadLedState(X, Y), bt);

            mShiftLayer.bindPressed(bt, () -> {
               if (mDataMode == DataMode.Main || mDataMode == DataMode.MainAlt)
                  onDrumPadPressed(X, Y);
            });

            final Button actionBt = driver.getPadButton(x + 4, y);
            mSceneAndPerfsLayer.bindLightState(() -> computePerfAndScenesLedState(X, Y), actionBt);
            if (y < 2)
               mSceneAndPerfsLayer.bindPressed(actionBt, () -> onDrumScenePressed(X, Y));
            else
            {
               mSceneAndPerfsLayer.bind(actionBt.mAfterTouch, v -> onDrumPerfPressure(X, Y, (int) (127. * v)));
               mSceneAndPerfsLayer.bindReleased(actionBt, () -> onDrumPerfReleased(X, Y));
            }

            mMainActionsLayer.bindPressed(actionBt, v -> onDrumActionPressed(X, Y, (int) (v * 127)));
            mMainActionsLayer.bindReleased(actionBt, () -> onDrumActionReleased(X, Y));
            if (X == 0 && Y == 2)
               mMainActionsLayer.bind(actionBt.mAfterTouch, v -> onAutoNoteRepeatPressure((int) (127. * v)));
         }
      }

      // Step Data
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 4; ++y)
         {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, y);

            mMixDataLayer.bindPressed(bt, () -> onMixDataPressed(X, 3 - Y));
            mMixDataLayer.bindLightState(() -> computeMixDataLedState(X, Y), bt);

            mSoundDataLayer.bindPressed(bt, () -> onSoundDataPressed(X, 3 - Y));
            mSoundDataLayer.bindLightState(() -> computeSoundDataLedState(X, Y), bt);
         }
      }

      mMainActionsLayer.bindLightState(() -> new LedState(isDrumPadSelectOn() ? Color.TRACK : Color.TRACK_LOW), mDriver.getPadButton(4, 0));
      mMainActionsLayer.bindLightState(() -> new LedState(isDrumPadMuteOn() || mDriver.isDeleteOn() ? Color.MUTE : Color.MUTE_LOW), mDriver.getPadButton(5, 0));
      mMainActionsLayer.bindLightState(() -> new LedState(isDrumPadSoloOn() || mDriver.isDeleteOn() ? Color.SOLO : Color.SOLO_LOW), mDriver.getPadButton(6, 0));
      mMainActionsLayer.bindLightState(LedState.OFF, mDriver.getPadButton(7, 0));

      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(4, 1) ? Color.BLUE : Color.BLUE_LOW), mDriver.getPadButton(4, 1));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(5, 1) ? Color.RED : Color.RED_LOW), mDriver.getPadButton(5, 1));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(6, 1) ? Color.GREEN : Color.GREEN_LOW), mDriver.getPadButton(6, 1));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(7, 1) ? Color.WHITE : Color.WHITE_LOW), mDriver.getPadButton(7, 1));

      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(4, 2) ? Color.PURPLE : Color.PURPLE_LOW), mDriver.getPadButton(4, 2));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(5, 2) ? Color.ORANGE : Color.ORANGE_LOW), mDriver.getPadButton(5, 2));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(6, 2) ? Color.ORANGE : Color.ORANGE_LOW), mDriver.getPadButton(6, 2));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(7, 2) ? Color.ORANGE : Color.ORANGE_LOW), mDriver.getPadButton(7, 2));

      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(4, 3) ? Color.YELLOW : Color.YELLOW_LOW), mDriver.getPadButton(4, 3));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(5, 3) ? Color.YELLOW : Color.YELLOW_LOW), mDriver.getPadButton(5, 3));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(6, 3) ? Color.YELLOW : Color.YELLOW_LOW), mDriver.getPadButton(6, 3));
      mMainActionsLayer.bindLightState(() -> new LedState(isActionOn(7, 3) ? Color.YELLOW : Color.YELLOW_LOW), mDriver.getPadButton(7, 3));
   }

   @Override
   protected void doActivate()
   {
      super.doActivate();

      final DrumPadBank drumPads = mDriver.mDrumPadBank;
      drumPads.exists().subscribe();
      drumPads.scrollPosition().subscribe();
      drumPads.hasMutedPads().subscribe();
      drumPads.hasSoloedPads().subscribe();
      drumPads.subscribe();
      drumPads.setIndication(true);

      for (int i = 0; i < 16; ++i)
      {
         final DrumPad drumPad = drumPads.getItemAt(i);
         drumPad.subscribe();
         drumPad.exists().subscribe();
         drumPad.color().subscribe();
         drumPad.mute().subscribe();
         drumPad.isMutedBySolo().subscribe();
         drumPad.solo().subscribe();
      }

      final CursorDevice cursorDevice = mDriver.mCursorDevice;
      cursorDevice.hasDrumPads().subscribe();
      cursorDevice.subscribe();

      final CursorRemoteControlsPage drumScenesRemoteControls = mDriver.mDrumScenesRemoteControls;
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.mDrumPerfsRemoteControls;
      for (int i = 0; i < 8; ++i)
      {
         {
            final RemoteControl parameter = drumScenesRemoteControls.getParameter(i);
            parameter.exists().subscribe();
            parameter.value().subscribe();
            parameter.subscribe();
         }

         {
            final RemoteControl parameter = drumPerfsRemoteControls.getParameter(i);
            parameter.exists().subscribe();
            parameter.value().subscribe();
            parameter.subscribe();
         }
      }

      updateDrumPadsBankPosition();

      setDataMode(mDataMode);
   }

   @Override
   protected void doDeactivate()
   {
      deactivateEveryLayers();

      final DrumPadBank drumPads = mDriver.mDrumPadBank;
      drumPads.exists().unsubscribe();
      drumPads.scrollPosition().unsubscribe();
      drumPads.hasMutedPads().unsubscribe();
      drumPads.hasSoloedPads().unsubscribe();
      drumPads.unsubscribe();
      drumPads.setIndication(false);

      for (int i = 0; i < 16; ++i)
      {
         final DrumPad drumPad = drumPads.getItemAt(i);
         drumPad.exists().unsubscribe();
         drumPad.color().unsubscribe();
         drumPad.mute().unsubscribe();
         drumPad.isMutedBySolo().unsubscribe();
         drumPad.solo().unsubscribe();
         drumPad.unsubscribe();
      }

      final CursorDevice cursorDevice = mDriver.mCursorDevice;
      cursorDevice.hasDrumPads().unsubscribe();
      cursorDevice.unsubscribe();

      final CursorRemoteControlsPage drumScenesRemoteControls = mDriver.mDrumScenesRemoteControls;
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.mDrumPerfsRemoteControls;
      for (int i = 0; i < 8; ++i)
      {
         {
            final RemoteControl parameter = drumScenesRemoteControls.getParameter(i);
            parameter.exists().unsubscribe();
            parameter.value().unsubscribe();
            parameter.unsubscribe();
         }

         {
            final RemoteControl parameter = drumPerfsRemoteControls.getParameter(i);
            parameter.exists().unsubscribe();
            parameter.value().unsubscribe();
            parameter.unsubscribe();
         }
      }

      super.doDeactivate();
   }

   private void deactivateEveryLayers()
   {
      mShiftLayer.deactivate();
      mDrumPadsLayer.deactivate();
      mMainActionsLayer.deactivate();
      mSceneAndPerfsLayer.deactivate();
      mSoundDataLayer.deactivate();
      mMixDataLayer.deactivate();
   }

   @Override
   protected void setDataMode(final DataMode dataMode)
   {
      super.setDataMode(dataMode);

      deactivateEveryLayers();

      switch (mDataMode)
      {
         case Main ->
         {
            mDrumPadsLayer.activate();
            mMainActionsLayer.activate();
         }
         case MixData -> mMixDataLayer.activate();
         case SoundData -> mSoundDataLayer.activate();
         case MainAlt ->
         {
            mDrumPadsLayer.activate();
            mSceneAndPerfsLayer.activate();
         }
      }
   }

   void drumPadsUp()
   {
      final DrumPadBank drumPads = mDriver.mDrumPadBank;
      final SettableIntegerValue position = drumPads.scrollPosition();

      if (position.get() == 0)
         position.set(4);
      else
         position.set(Math.min(116, position.get() + 16));
      updateDrumPadsBankPosition();
   }

   void drumpPadsDown()
   {
      final DrumPadBank drumPads = mDriver.mDrumPadBank;
      final SettableIntegerValue position = drumPads.scrollPosition();

      position.set(Math.max(0, position.get() - 16));
      updateDrumPadsBankPosition();
   }

   private void updateDrumPadsBankPosition()
   {
      mDriver.updateKeyTranslationTable();
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      if (mDriver.isShiftOn() || (mDataMode != DataMode.Main && mDataMode != DataMode.MainAlt))
         return;

      for (int x = 0; x < 4; ++x)
      {
         for (int y = 0; y < 4; ++y)
         {
            final int tableIndex = 11 + x + y * 10;
            final int note = calculateDrumPadKey(x, y);

            assert note >= 0;
            assert note < 128;

            table[tableIndex] = note;
         }
      }
   }

   private int calculateDrumPadKey(final int x, final int y)
   {
      final DrumPadBank drumPadBank = mDriver.mDrumPadBank;

      if (drumPadBank.exists().get())
         return x + 4 * y + drumPadBank.scrollPosition().get();
      return x + 4 * y + 36;
   }

   private void onMixDataPressed(final int x, final int y)
   {
      final Clip clip = mDriver.mCursorClip;
      final List<Button> padsInHoldState = getStepsInPressedOrHoldState();

      for (final Button button : padsInHoldState)
      {
         final int clipStepIndex = calculateClipStepIndex(button.mX - 1, 8 - button.mY);
         final NoteStep noteStep = clip.getStep(0, clipStepIndex, mCurrentPitch);

         switch (y)
         {
            case 0 -> noteStep.setVelocity(x / 7.0);
            case 1 -> noteStep.setDuration(computeDuration(x));
            case 2 -> noteStep.setPan((3 <= x && x <= 4) ? 0 : (x - 3.5) / 3.5);
         }
      }
   }

   private void onSoundDataPressed(final int x, final int y)
   {
      final Clip clip = mDriver.mCursorClip;
      final List<Button> padsInHoldState = getStepsInPressedOrHoldState();

      for (final Button buttonState : padsInHoldState)
      {
         final int clipStepIndex = calculateClipStepIndex(buttonState.mX - 1, 8 - buttonState.mY);
         final NoteStep noteStep = clip.getStep(0, clipStepIndex, mCurrentPitch);

         switch (y)
         {
            case 0 -> noteStep.setTranspose(computeTranspose(x));
            case 1 -> noteStep.setTimbre((3 <= x && x <= 4) ? 0 : (x - 3.5) / 3.5);
            case 2 -> noteStep.setPressure(x / 7.0);
         }
      }
   }

   private void onDrumActionPressed(final int x, final int y, final int velocity)
   {
      if (y == 0 && (x >= 0 && x <= 2))
      {
         if (mDriver.isDeleteOn() && x == 1 && y == 0)
            mDriver.mDrumPadBank.clearMutedPads();
         if (mDriver.isDeleteOn() && x == 2 && y == 0)
            mDriver.mDrumPadBank.clearSoloedPads();
         mDriver.mNoteInput.setKeyTranslationTable(NoteInputUtils.NO_NOTES);
      }
      else if (y == 1)
      {
         final Clip cursorClip = mDriver.mCursorClip;
         final boolean cursorClipExists = cursorClip.exists().get();
         final int sceneIndex = cursorClipExists ? cursorClip.clipLauncherSlot().sceneIndex().get() : 0;
         final Track track = cursorClipExists ? cursorClip.getTrack() : mDriver.mCursorTrack;

         if (x == 0)
            track.createNewLauncherClip(sceneIndex, 8);
         else if (x == 1)
            track.recordNewLauncherClip(sceneIndex);
         else if (x == 2 && cursorClipExists)
            cursorClip.launch();
         else if (x == 3)
            track.stop();
      }
      else if (y == 2 || y == 3)
      {
         final double period;

         if (y == 3 || x != 0)
            period = calculateNoteRepeatPeriodForPad(x, y);
         else
            period = calculateAutoNoteRepeatPeriod(velocity);

         // Configure the Arp as a note repeat
         final Arpeggiator arpeggiator = mDriver.mArpeggiator;
         arpeggiator.rate().set(period);
         arpeggiator.usePressureToVelocity().set(true);
         arpeggiator.shuffle().set(true);
         arpeggiator.mode().set("all"); // that's the note repeat way
         arpeggiator.octaves().set(0);
         arpeggiator.isEnabled().set(true);
         arpeggiator.humanize().set(0);
         arpeggiator.isFreeRunning().set(false);

         mNoteRepeatStack.add(new Coord(x, y));
      }
   }

   private double calculateNoteRepeatPeriodForPad(final int x, final int y)
   {
      if (y == 3)
         return 1.0 / 8.0 * (1 << x);
      if (y == 2 && x > 0)
         return 1.0 / 6.0 * (1 << (x - 1));
      return 0.25;
   }

   private void onAutoNoteRepeatPressure(final int pressure)
   {
      final Arpeggiator arpeggiator = mDriver.mArpeggiator;
      arpeggiator.rate().set(calculateAutoNoteRepeatPeriod(pressure));
   }

   private double calculateAutoNoteRepeatPeriod(final int pressure)
   {
      if (pressure < 32)
         return 1.0;
      if (pressure < 64)
         return 0.5;
      if (pressure < 96)
         return 0.25;
      return 0.125;
   }

   private void onDrumActionReleased(final int x, final int y)
   {
      if (y == 0 && (!isDrumPadSoloOn() && !isDrumPadMuteOn() && !isDrumPadSelectOn()))
         mDriver.updateKeyTranslationTable();
      else if (y == 2 || y == 3)
      {
         final Arpeggiator arpeggiator = mDriver.mArpeggiator;

         mNoteRepeatStack.removeIf(coord -> coord.x == x && coord.y == y);

         if (!mNoteRepeatStack.isEmpty())
         {
            final Coord coord = mNoteRepeatStack.get(mNoteRepeatStack.size() - 1);
            if (coord.y != 2 || coord.x != 0)
            {
               final double periodForPad = calculateNoteRepeatPeriodForPad(coord.x, coord.y);
               arpeggiator.rate().set(periodForPad);
            }
         }

         arpeggiator.isEnabled().set(hasNoteRepeatPadPressed());
      }
   }

   private boolean hasNoteRepeatPadPressed()
   {
      for (int x = 0; x < 4; ++x)
         for (int y = 0; y < 2; ++y)
            if (isActionOn(4 + x, y + 2))
               return true;
      return false;
   }

   private void onDrumPerfReleased(final int x, final int y)
   {
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.mDrumPerfsRemoteControls;
      final RemoteControl parameter = drumPerfsRemoteControls.getParameter(x + 4 * (3 - y));

      if (!mDriver.isShiftOn() && parameter.exists().get())
         parameter.set(0);
   }

   private void onDrumPerfPressure(final int x, final int y, final int pressure)
   {
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.mDrumPerfsRemoteControls;
      final RemoteControl parameter = drumPerfsRemoteControls.getParameter(x + 4 * (3 - y));

      if (!mDriver.isShiftOn() && parameter.exists().get())
         parameter.set(pressure / 127.0);
   }

   private void onDrumPerfPressed(final int x, final int y)
   {

   }

   private void onDrumScenePressed(final int x, final int y)
   {
      final int paramIndex = x + 4 * (1 - y);
      final CursorRemoteControlsPage scenesRemoteControls = mDriver.mDrumScenesRemoteControls;

      final RemoteControl parameter = scenesRemoteControls.getParameter(paramIndex);
      if (parameter.exists().get() && parameter.get() != 0)
      {
         parameter.set(0);
         return;
      }

      if (!mDriver.isShiftOn())
      {
         for (int i = 0; i < 8; ++i)
         {
            if (i == paramIndex)
               continue;

            final RemoteControl otherParam = scenesRemoteControls.getParameter(i);
            if (otherParam.exists().get())
               otherParam.set(0);
         }
      }

      if (parameter.exists().get())
         parameter.set(1);
   }

   private void onStepPressed(final int absoluteStep, final int velocity)
   {
      final Clip cursorClip = mDriver.mCursorClip;

      if (mDriver.isShiftOn())
         setClipLength((absoluteStep + 1) / 4.0);
      else if (mDriver.isDeleteOn())
         cursorClip.clearStep(absoluteStep, mCurrentPitch);
      else
      {
         final NoteStep noteStep = cursorClip.getStep(0, absoluteStep, mCurrentPitch);
         if (noteStep.state() != NoteStep.State.NoteOn)
         {
            cursorClip.setStep(absoluteStep, mCurrentPitch, velocity, 1.0 / 4.0);
            mStepsBeingAdded.add(absoluteStep);
         }
      }
   }

   private void onStepReleased(final int absoluteStep, final boolean wasHeld)
   {
      final Clip cursorClip = mDriver.mCursorClip;

      if (mDriver.isShiftOn() || mDriver.isDeleteOn())
         return;

      final NoteStep noteStep = cursorClip.getStep(0, absoluteStep, mCurrentPitch);
      if (noteStep.state() == NoteStep.State.NoteOn && !wasHeld && !mStepsBeingAdded.contains(absoluteStep))
         cursorClip.clearStep(absoluteStep, mCurrentPitch);

      mStepsBeingAdded.remove(absoluteStep);
   }

   private void onDrumPadPressed(final int x, final int y)
   {
      final int key = calculateDrumPadKey(x, y);

      if (mDriver.isDeleteOn())
         mDriver.mCursorClip.clearStepsAtY(0, key);
      else if (isDrumPadMuteOn())
      {
         final DrumPadBank drumPadBank = mDriver.mDrumPadBank;
         final DrumPad drumPad = drumPadBank.getItemAt(x + 4 * y);
         drumPad.mute().toggle();
      }
      else if (isDrumPadSoloOn())
      {
         final DrumPadBank drumPadBank = mDriver.mDrumPadBank;
         final DrumPad drumPad = drumPadBank.getItemAt(x + 4 * y);
         drumPad.solo().toggle();
      }
      else if (isDrumPadSelectOn() || mDriver.isShiftOn())
      {
         final DrumPadBank drumPadBank = mDriver.mDrumPadBank;
         final DrumPad drumPad = drumPadBank.getItemAt(x + 4 * y);
         drumPad.selectInEditor();
         mCurrentPitch = key;
      }
      else
         mCurrentPitch = key;
   }

   private boolean isActionOn(final int x, final int y)
   {
      if (mDataMode != DataMode.Main)
         return false;

      final Button button = mDriver.getPadButton(x, y);
      return button.mButton.isPressed().get();
   }

   private boolean isDrumPadSelectOn()
   {
      return isActionOn(4, 0);
   }

   private boolean isDrumPadSoloOn()
   {
      return isActionOn(6, 0);
   }

   private boolean isDrumPadMuteOn()
   {
      return isActionOn(5, 0);
   }

   @Override
   protected NoteStep findStepInfo(final int clipStepIndex)
   {
      return mDriver.mCursorClip.getStep(0, clipStepIndex, mCurrentPitch);
   }

   private LedState computePerfAndScenesLedState(final int x, final int y)
   {
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.mDrumPerfsRemoteControls;
      final CursorRemoteControlsPage drumScenesRemoteControls = mDriver.mDrumScenesRemoteControls;

      if (y > 1)
      {
         final RemoteControl perfParam = drumPerfsRemoteControls.getParameter(x + (3 - y) * 4);
         if (perfParam.exists().get())
            return new LedState(Color.scale(Color.CYAN, (float) (0.95 * perfParam.get() + 0.05)));
      }
      else
      {
         final RemoteControl sceneParam = drumScenesRemoteControls.getParameter(x + (1 - y) * 4);
         if (sceneParam.exists().get())
            return new LedState(Color.scale(Color.YELLOW, (float) (0.9 * sceneParam.get() + 0.1)));
      }
      return LedState.OFF;
   }

   private LedState computeStepSeqLedState(final int x, final int y)
   {
      final Clip clip = mDriver.mCursorClip;
      final int playingStep = clip.playingStep().get();

      final NoteStep noteStep = clip.getStep(0, calculateClipStepIndex(x, y), mCurrentPitch);

      if (playingStep == mPage * 32 + 8 * y + x)
         return new LedState(noteStep.state() == NoteStep.State.NoteOn ? Color.STEP_PLAY : Color.STEP_PLAY_HEAD);
      if (mDriver.getPadButton(x, 7- y).getButtonState() == Button.State.HOLD)
         return new LedState(Color.STEP_HOLD);
      return switch (noteStep.state())
         {
            case NoteOn -> new LedState(Color.STEP_ON);
            case NoteSustain -> new LedState(Color.STEP_SUSTAIN);
            case Empty -> new LedState(Color.STEP_OFF);
         };

   }

   private LedState computeDrumPadLedState(final int x, final int y)
   {
      final Clip clip = mDriver.mCursorClip;
      final PlayingNoteArrayValue playingNotes = mDriver.mCursorTrack.playingNotes();
      final CursorDevice cursorDevice = mDriver.mCursorDevice;
      final boolean hasDrumPads = cursorDevice.hasDrumPads().get();
      final DrumPadBank drumPads = mDriver.mDrumPadBank;

      final int pitch = calculateDrumPadKey(x, y);
      final boolean isPlaying = playingNotes.isNotePlaying(pitch);
      final DrumPad drumPad = drumPads.getItemAt(x + 4 * y);
      final boolean drumPadExists = hasDrumPads & drumPad.exists().get();
      final boolean drumPadIsSolo = drumPadExists & drumPad.solo().get();
      final boolean drumPadIsMuted = drumPadExists & !drumPadIsSolo & (drumPad.mute().get() | drumPad.isMutedBySolo().get());
      final Color color = new Color(drumPad.color());

      if (isPlaying)
         return new LedState(drumPadIsMuted ? Color.GREEN_LOW : Color.GREEN);
      if (mCurrentPitch == pitch)
         return new LedState(drumPadIsMuted ? Color.TRACK_LOW : Color.TRACK);
      if (hasDrumPads)
      {
         if (drumPadExists)
            return new LedState(drumPadIsSolo ? Color.YELLOW : drumPadIsMuted ? Color.scale(color, .1f) : color);
         else
            return new LedState(Color.WHITE_LOW);
      }
      return new LedState(clip.color());
   }

   @Override
   protected String getDataModeDescription(final DataMode dataMode)
   {
      return switch (dataMode)
         {
            case Main -> "Drum Sequencer: Note Repeat, Solo, Mute";
            case MainAlt -> "Drum Sequencer: Perfs, Scenes";
            case MixData -> "Drum Sequencer: Velocity, Note Length, Pan";
            case SoundData -> "Drum Sequencer: Pich Offset, Timbre, Pressure";
            default -> "Error";
         };
   }

   private void invalidateDrumPosition(final int newPosition)
   {
      mCurrentPitch = newPosition;
      updateDrumPadsBankPosition();
   }

   private static class Coord
   {
      Coord(final int x_, final int y_)
      {
         x = x_;
         y = y_;
      }

      final int x;
      final int y;
   }

   private final List<Coord> mNoteRepeatStack = new ArrayList<>();
   private int mCurrentPitch = 36;
   private final LaunchpadLayer mShiftLayer;
   private final LaunchpadLayer mDrumPadsLayer;
   private final LaunchpadLayer mSceneAndPerfsLayer;
   private final LaunchpadLayer mMainActionsLayer;
   private final LaunchpadLayer mMixDataLayer;
   private final LaunchpadLayer mSoundDataLayer;
}
