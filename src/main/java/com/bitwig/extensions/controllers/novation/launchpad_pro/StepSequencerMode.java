package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Track;

import java.util.List;

final class StepSequencerMode extends AbstractSequencerMode {
   StepSequencerMode(final LaunchpadProControllerExtension driver) {
      super(driver, "step-sequencer");

      final PinnableCursorClip cursorClip = driver.mCursorClip;

      mKeyboardLayer = new KeyboardLayer(driver, "step-sequencer-keyboard", 0, 0, 8, 4,
         () -> new Color(mDriver.mCursorClip.color()), this::isKeyOn, this::onKeyDataPressed);
      mMixDataLayer = new LaunchpadLayer(driver, "drum-seq-mix-data");
      mSoundDataLayer = new LaunchpadLayer(driver, "drum-seq-sound-data");

      bindLightState(LedState.STEP_SEQ_MODE, driver.mUserButton);

      // Step sequencer
      for (int y = 0; y < 4; ++y) {
         for (int x = 0; x < 8; ++x) {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, y + 4);
            final int clipStepIndex = calculateClipStepIndex(x, 3 - y);
            bindPressed(bt, v -> {
               bt.onButtonPressed(driver.getHost());
               onStepPressed(clipStepIndex);
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
            cursorClip.scrollToStep(page * 32);
         });
         bindLightState(() -> computePatternOffsetLedState(3 - Y), sceneButton);

         final Button dataChoiceBt = driver.mSceneButtons[y];
         bindPressed(dataChoiceBt, () -> setDataMode(Y));
         bindLightState(() -> computeDataChoiceLedState(Y), dataChoiceBt);
      }

      // Scene buttons in shift layer
      for (int y = 0; y < 8; ++y) {
         final Button sceneButton = driver.mSceneButtons[y];
         final int page = 8 - y;
         final int Y = y;
         mShiftLayer.bindPressed(sceneButton, () -> setClipLength(page * 4));
         mShiftLayer.bindLightState(() -> computeClipLengthSelectionLedState(7 - Y), sceneButton);
      }

      mShiftLayer.bindPressed(driver.mUpButton, () -> {
         mKeyboardLayer.octaveUp();
         mDriver.updateKeyTranslationTable();
      });
      mShiftLayer.bindPressed(driver.mDownButton, () -> {
         mKeyboardLayer.octaveDown();
         mDriver.updateKeyTranslationTable();
      });
      mShiftLayer.bindLightState(() -> mKeyboardLayer.canOctaveUp() ? LedState.PITCH : LedState.PITCH_LOW,
         driver.mUpButton);
      mShiftLayer.bindLightState(() -> mKeyboardLayer.canOctaveDown() ? LedState.PITCH : LedState.PITCH_LOW,
         driver.mDownButton);

      // Step Data
      for (int x = 0; x < 8; ++x) {
         for (int y = 0; y < 4; ++y) {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, y);

            mMixDataLayer.bindPressed(bt, () -> onMixDataPressed(X, 3 - Y));
            mMixDataLayer.bindLightState(() -> computeMixDataLedState(X, Y), bt);

            mSoundDataLayer.bindPressed(bt, () -> onSoundDataPressed(X, 3 - Y));
            mSoundDataLayer.bindLightState(() -> computeSoundDataLedState(X, Y), bt);
         }
      }
   }

   @Override
   protected void doActivate() {
      super.doActivate();

      mDriver.mCursorClip.subscribe();
      mDriver.mCursorClip.color().subscribe();

      final Track track = mDriver.mCursorClip.getTrack();
      track.subscribe();
      track.playingNotes().subscribe();

      mKeyboardLayer.activate();

      mStepPressedCount = 0;
   }

   @Override
   protected void doDeactivate() {
      deactivateEveryLayers();

      final Track track = mDriver.mCursorClip.getTrack();
      track.playingNotes().unsubscribe();
      track.unsubscribe();

      mDriver.mCursorClip.color().unsubscribe();
      mDriver.mCursorClip.unsubscribe();

      mStepPressedCount = 0;

      super.doDeactivate();
   }

   private void deactivateEveryLayers() {
      mShiftLayer.deactivate();
      mKeyboardLayer.deactivate();
      mMixDataLayer.deactivate();
      mSoundDataLayer.deactivate();
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table) {
      if (mDataMode == DataMode.Main && mStepPressedCount == 0) mKeyboardLayer.updateKeyTranslationTable(table);

      mDriver.mNoteInput.setKeyTranslationTable(table);
   }

   @Override
   protected NoteStep findStepInfo(final int clipStepIndex) {
      final Clip cursorClip = mDriver.mCursorClip;
      for (int key = 0; key < 128; ++key) {
         final NoteStep noteStep = cursorClip.getStep(0, clipStepIndex, key);
         if (noteStep.state() == NoteStep.State.NoteOn) return noteStep;
      }
      return cursorClip.getStep(0, clipStepIndex, 0);
   }

   private boolean isKeyOn(final int key) {
      assert key >= 0 && key < 127;

      if (mDriver.mCursorClip.getTrack().playingNotes().isNotePlaying(key)) return true;

      final Clip cursorClip = mDriver.mCursorClip;
      final List<Button> stepsInHoldState = getStepsInHoldState();
      for (final Button button : stepsInHoldState) {
         final int clipStepIndex = calculateClipStepIndex(button.mX - 1, 8 - button.mY);

         final NoteStep noteStep = cursorClip.getStep(0, clipStepIndex, key);
         return noteStep.state() == NoteStep.State.NoteOn;
      }

      return false;
   }

   private LedState computeStepSeqLedState(final int x, final int y) {
      final Clip clip = mDriver.mCursorClip;
      final int playingStep = clip.playingStep().get();

      final int clipStepIndex = calculateClipStepIndex(x, y);
      final NoteStep noteStep = computeVerticalStepState(clipStepIndex);

      if (playingStep == mPage * 32 + 8 * y + x) return LedState.STEP_PLAY_HEAD;
      if (mDriver.getPadButton(x, 7 - y).getButtonState() == Button.State.HOLD) return LedState.STEP_HOLD;
      switch (noteStep.state()) {
         case NoteOn:
            return LedState.STEP_ON;
         case NoteSustain:
            return LedState.STEP_SUSTAIN;
         case Empty:
            return LedState.STEP_OFF;
         default:
            throw new IllegalStateException();
      }
   }

   private NoteStep computeVerticalStepState(final int absoluteStepIndex) {
      final Clip clip = mDriver.mCursorClip;
      NoteStep value = clip.getStep(0, absoluteStepIndex, 0);

      for (int i = 0; i < 128; ++i) {
         final NoteStep noteStep = clip.getStep(0, absoluteStepIndex, i);
         switch (noteStep.state()) {
            case NoteOn:
               return noteStep;
            case NoteSustain:
               value = noteStep;
               break;
            case Empty:
               /* Nothing to do */
         }
      }
      return value;
   }

   void invalidate() {
      if (!isActive()) return;

      mDriver.updateKeyTranslationTable();
   }

   private void onMixDataPressed(final int x, final int y) {
      final Clip clip = mDriver.mCursorClip;
      final List<Button> padsInHoldState = mDriver.findPadsInHoldState();

      for (final Button buttonState : padsInHoldState) {
         final int clipStepIndex = calculateClipStepIndex(buttonState.mX - 1, 8 - buttonState.mY);

         for (int key = 0; key < 128; ++key) {
            final NoteStep noteStep = clip.getStep(0, clipStepIndex, key);
            if (noteStep.state() != NoteStep.State.NoteOn) continue;

            switch (y) {
               case 0:
                  noteStep.setVelocity(x / 7.0);
                  break;

               case 1:
                  noteStep.setDuration(computeDuration(x));
                  break;

               case 2:
                  noteStep.setPan((3 <= x && x <= 4) ? 0 : (x - 3.5) / 3.5);
                  break;

               case 3:
                  noteStep.setGain(x / 7.0);
                  break;
            }
         }
      }
   }

   private void onSoundDataPressed(final int x, final int y) {
      final Clip clip = mDriver.mCursorClip;
      final List<Button> padsInHoldState = mDriver.findPadsInHoldState();

      for (final Button buttonState : padsInHoldState) {
         final int clipStepIndex = calculateClipStepIndex(buttonState.mX - 1, 8 - buttonState.mY);

         for (int key = 0; key < 128; ++key) {
            final NoteStep noteStep = clip.getStep(0, clipStepIndex, key);
            if (noteStep.state() != NoteStep.State.NoteOn) continue;

            switch (y) {
               case 0:
                  noteStep.setTranspose(computeTranspoose(x));
                  break;

               case 1:
                  noteStep.setTimbre((3 <= x && x <= 4) ? 0 : (x - 3.5) / 3.5);
                  break;

               case 2:
                  noteStep.setPressure(x / 7.0);
                  break;
            }
         }
      }
   }

   private void onKeyDataPressed(final int key, final double velocity) {
      assert 0 <= key && key < 128;

      final Clip cursorClip = mDriver.mCursorClip;
      for (final Button buttonState : getStepsInPressedOrHoldState()) {
         final int clipStepIndex = calculateClipStepIndex(buttonState.mX - 1, 8 - buttonState.mY);
         cursorClip.toggleStep(clipStepIndex, key, (int) (127 * velocity));
      }
   }


   private void onStepPressed(final int absoluteStep) {
      final Clip cursorClip = mDriver.mCursorClip;

      if (mDriver.isShiftOn()) setClipLength((absoluteStep + 1) / 4.0);
      else if (mDriver.isDeleteOn()) cursorClip.clearStepsAtX(0, absoluteStep);

      ++mStepPressedCount;
      if (mStepPressedCount == 1) invalidate();
   }

   private void onStepReleased(final int absoluteStep, final boolean wasHeld) {
      final Clip cursorClip = mDriver.mCursorClip;

      --mStepPressedCount;
      if (mStepPressedCount == 1) invalidate();

      if (mDriver.isShiftOn() || mDriver.isDeleteOn()) return;

      final NoteStep noteStep = computeVerticalStepState(absoluteStep);
      if (noteStep.state() == NoteStep.State.NoteOn && !wasHeld) cursorClip.clearStepsAtX(0, absoluteStep);
   }

   @Override
   protected String getDataModeDescription(final DataMode dataMode) {
      switch (dataMode) {
         case Main:
         case MainAlt:
            return "Step Sequencer: Keys";
         case MixData:
            return "Step Sequencer: Velocity, Note Length, Pan";
         case SoundData:
            return "Step Sequencer: Pich Offset, Timbre, Pressure";
         default:
            return "Error";
      }
   }

   @Override
   protected boolean hasMainAltMode() {
      return false;
   }

   @Override
   protected void setDataMode(final DataMode dataMode) {
      super.setDataMode(dataMode);

      deactivateEveryLayers();

      switch (mDataMode) {
         case Main:
         case MainAlt:
            mKeyboardLayer.activate();
            break;
         case MixData:
            mMixDataLayer.activate();
            break;
         case SoundData:
            mSoundDataLayer.activate();
            break;
      }
   }

   private final KeyboardLayer mKeyboardLayer;
   private final LaunchpadLayer mMixDataLayer;
   private final LaunchpadLayer mSoundDataLayer;
   private int mStepPressedCount = 0;
}
