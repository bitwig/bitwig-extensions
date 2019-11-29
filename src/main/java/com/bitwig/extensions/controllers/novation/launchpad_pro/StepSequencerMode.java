package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.Track;

public class StepSequencerMode extends AbstractSequencerMode
{
   StepSequencerMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "step-sequencer");

      final CursorTrack cursorTrack = driver.getCursorTrack();
      final PinnableCursorClip cursorClip = driver.getCursorClip();

      mKeyboardLayer = new KeyboardLayer(driver, "step-sequencer-keyboard", 0, 0, 8, 4, () -> new Color(mDriver.getCursorTrack().color()),
         this::isKeyOn, (this::onKeyDataPressed));
      mShiftLayer = new LaunchpadLayer(driver, "drum-sequencer-shift");
      mMixDataLayer = new LaunchpadLayer(driver, "drum-seq-mix-data");
      mSoundDataLayer = new LaunchpadLayer(driver, "drum-seq-sound-data");

      bindLightState(LedState.STEP_SEQ_MODE, driver.getUserButton());

      // Step sequencer
      for (int y = 0; y < 4; ++y)
      {
         for (int x = 0; x < 8; ++x)
         {
            final int X = x;
            final int Y = y;
            final Button bt = driver.getPadButton(x, y + 4);
            final int absoluteStepIndex = calculateAbsoluteStepIndex(x, 3 - y);
            bindPressed(bt, v -> {
               bt.onButtonPressed(driver.getHost());
               onStepPressed(absoluteStepIndex, (int) (v * 127.0));
            });
            bindReleased(bt, () -> {
               final boolean wasHeld = bt.getButtonState() == Button.State.HOLD;
               bt.onButtonReleased();
               onStepReleased(absoluteStepIndex, wasHeld);
            });
            bindLightState(() -> computeStepSeqLedState(X, 3 - Y), bt);
         }

         final Button sceneButton = driver.getSceneButton(y + 4);
         final int page = 3 - y;
         final int Y = y;
         bindPressed(sceneButton, () -> mPage = page);
         bindLightState(() -> computePatternOffsetLedState(3 - Y), sceneButton);

         final Button dataChoiceBt = driver.getSceneButton(y);
         bindPressed(dataChoiceBt, () -> setDataMode(Y));
         bindLightState(() -> computeDataChoiceLedState(Y), dataChoiceBt);
      }

      // Scene buttons in shift layer
      for (int y = 0; y < 8; ++y)
      {
         final Button sceneButton = driver.getSceneButton(y);
         final int page = 8 - y;
         final int Y = y;
         mShiftLayer.bindPressed(sceneButton, () -> setClipLength(page * 4));
         mShiftLayer.bindLightState(() -> computeClipLengthSelectionLedState(7 - Y), sceneButton);
      }

      mShiftLayer.bindPressed(driver.getUpButton(), mKeyboardLayer::octaveUp);
      mShiftLayer.bindPressed(driver.getDownButton(), mKeyboardLayer::octaveDown);
      mShiftLayer.bindLightState(() -> mKeyboardLayer.canOctaveUp() ? LedState.PITCH : LedState.PITCH_LOW, driver.getUpButton());
      mShiftLayer.bindLightState(() -> mKeyboardLayer.canOctaveDown() ? LedState.PITCH : LedState.PITCH_LOW, driver.getDownButton());

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

      bindPressed(driver.getUpButton(), cursorClip.selectPreviousAction());
      bindPressed(driver.getDownButton(), cursorClip.selectNextAction());
      bindPressed(driver.getLeftButton(), cursorTrack.selectPreviousAction());
      bindPressed(driver.getRightButton(), cursorTrack.selectNextAction());

      bindLightState(() -> cursorClip.hasPrevious().get() ? new LedState(cursorTrack.color()) : new LedState(Color.scale(new Color(cursorTrack.color()), .2f)), driver.getUpButton());
      bindLightState(() -> cursorClip.hasNext().get() ? new LedState(cursorTrack.color()) : new LedState(Color.scale(new Color(cursorTrack.color()), .2f)), driver.getDownButton());
      bindLightState(() -> cursorTrack.hasNext().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getRightButton());
      bindLightState(() -> cursorTrack.hasPrevious().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getLeftButton());

      bindLayer(driver.getShiftButton(), mShiftLayer);
   }

   @Override
   protected void doActivate()
   {
      super.doActivate();

      final Track track = mDriver.getCursorTrack();
      track.subscribe();

      final SettableColorValue trackColor = track.color();
      trackColor.subscribe();

      track.selectInMixer();
      mKeyboardLayer.activate();
   }

   @Override
   protected void doDeactivate()
   {
      deactivateEveryLayers();

      final Track track = mDriver.getCursorTrack();
      track.unsubscribe();

      final SettableColorValue trackColor = track.color();
      trackColor.unsubscribe();

      super.doDeactivate();
   }

   private void deactivateEveryLayers()
   {
      mShiftLayer.deactivate();
      mKeyboardLayer.deactivate();
      mMixDataLayer.deactivate();
      mSoundDataLayer.deactivate();
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      if (mDataMode == DataMode.Main)
         mKeyboardLayer.updateKeyTranslationTable(table);

      mDriver.getNoteInput().setKeyTranslationTable(table);
   }

   @Override
   protected NoteStep findStepInfo(final int absoluteStepIndex)
   {
      final Clip cursorClip = mDriver.getCursorClip();
      for (int key = 0; key < 128; ++key)
      {
         final NoteStep noteStep = cursorClip.getStep(0, absoluteStepIndex, key);
         if (noteStep.state() == NoteStep.State.NoteOn)
            return noteStep;
      }
      return cursorClip.getStep(0, absoluteStepIndex, 0);
   }

   private boolean isKeyOn(final int key)
   {
      assert key >= 0 && key < 127;

      if (mDriver.getCursorTrack().playingNotes().isNotePlaying(key))
         return true;

      final Clip cursorClip = mDriver.getCursorClip();
      final List<Button> stepsInHoldState = findStepsInHoldState();
      for (final Button button : stepsInHoldState)
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(button.getX() - 1, 8 - button.getY());

         final NoteStep noteStep = cursorClip.getStep(0, absoluteStepIndex, key);
         return noteStep.state() == NoteStep.State.NoteOn;
      }

      return false;
   }

   private LedState computeStepSeqLedState(final int x, final int y)
   {
      final Clip clip = mDriver.getCursorClip();
      final int playingStep = clip.playingStep().get();

      final int absoluteStepIndex = calculateAbsoluteStepIndex(x, y);
      final NoteStep noteStep = computeVerticalStepState(absoluteStepIndex);
      final Button button = mDriver.getPadButton(x, 7 - y);

      if (playingStep == mPage * 32 + 8 * y + x)
         return new LedState(Color.GREEN);
      if (mDriver.getPadButton(x, 7- y).getButtonState() == Button.State.HOLD)
         return new LedState(Color.STEP_HOLD);
      switch (noteStep.state())
      {
         case NoteOn:
            return new LedState(Color.STEP_ON);
         case NoteSustain:
            return new LedState(Color.STEP_SUSTAIN);
         case Empty:
            return new LedState(Color.STEP_OFF);
         default:
            throw new IllegalStateException();
      }
   }

   private NoteStep computeVerticalStepState(final int absoluteStepIndex)
   {
      final Clip clip = mDriver.getCursorClip();
      NoteStep value = clip.getStep(0, absoluteStepIndex, 0);

      for (int i = 0; i < 128; ++i)
      {
         final NoteStep noteStep = clip.getStep(0, absoluteStepIndex, i);
         switch (noteStep.state())
         {
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

   void invalidate()
   {
      if (!isActive())
         return;

      mDriver.updateKeyTranslationTable();
      paint();
   }

   private void onMixDataPressed(final int x, final int y)
   {
      final Clip clip = mDriver.getCursorClip();
      final List<Button> padsInHoldState = mDriver.findPadsInHoldState();

      for (final Button buttonState : padsInHoldState)
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(buttonState.getX() - 1, 8 - buttonState.getY());

         for (int key = 0; key < 128; ++key)
         {
            final NoteStep noteStep = clip.getStep(0, absoluteStepIndex, key);
            if (noteStep.state() != NoteStep.State.NoteOn)
               continue;

            switch (y)
            {
               case 0:
                  noteStep.setVelocity(x / 7.0);
                  break;

               case 1:
                  noteStep.setDuration(computeDuration(x));
                  break;

               case 2:
                  noteStep.setPan((3 <= x && x <= 4) ? 0 : (x - 3.5) / 3.5);
                  break;
            }
         }
      }
   }

   private void onSoundDataPressed(final int x, final int y)
   {
      final Clip clip = mDriver.getCursorClip();
      final List<Button> padsInHoldState = mDriver.findPadsInHoldState();

      for (final Button buttonState : padsInHoldState)
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(buttonState.getX() - 1, 8 - buttonState.getY());

         for (int key = 0; key < 128; ++key)
         {
            final NoteStep noteStep = clip.getStep(0, absoluteStepIndex, key);
            if (noteStep.state() != NoteStep.State.NoteOn)
               continue;

            switch (y)
            {
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

   private void onKeyDataPressed(final int key, final double velocity)
   {
      assert 0 <= key && key < 128;

      final Clip cursorClip = mDriver.getCursorClip();
      for (final Button buttonState : findStepsInPressedOrHoldState())
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(buttonState.getX() - 1, 8 - buttonState.getY());
         cursorClip.toggleStep(absoluteStepIndex, key, (int) (127 * velocity));
      }
   }

   @Override
   void onPadReleased(final int x, final int y, final int velocity, final boolean wasHeld)
   {
      if (y >= 4)
      {
         onStepReleased(calculateAbsoluteStepIndex(x, 7 - y), wasHeld);
         return;
      }

      switch (mDataMode)
      {
         case Main:
         case MainAlt:
            break;
         case MixData:
            break;
         case SoundData:
            break;
      }
   }

   private void onStepPressed(final int absoluteStep, final int velocity)
   {
      final Clip cursorClip = mDriver.getCursorClip();

      if (mDriver.isShiftOn())
         setClipLength((absoluteStep + 1) / 4.0);
      else if (mDriver.isDeleteOn())
         cursorClip.clearStepsAtX(0, absoluteStep);
   }

   private void onStepReleased(final int absoluteStep, final boolean wasHeld)
   {
      final Clip cursorClip = mDriver.getCursorClip();

      if (mDriver.isShiftOn() || mDriver.isDeleteOn())
         return;

      final NoteStep noteStep = computeVerticalStepState(absoluteStep);
      if (noteStep.state() == NoteStep.State.NoteOn && !wasHeld)
         cursorClip.clearStepsAtX(0, absoluteStep);
   }

   @Override
   public void onArrowDownPressed()
   {
      mKeyboardLayer.octaveDown();
      mDriver.updateKeyTranslationTable();
   }

   @Override
   public void onArrowUpPressed()
   {
      mKeyboardLayer.octaveUp();
      mDriver.updateKeyTranslationTable();
   }

   @Override
   protected String getDataModeDescription(final DataMode dataMode)
   {
      switch (dataMode)
      {
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
   protected boolean hasMainAltMode()
   {
      return false;
   }

   private final KeyboardLayer mKeyboardLayer;
   private final LaunchpadLayer mMixDataLayer;
   private final LaunchpadLayer mSoundDataLayer;
   private final LaunchpadLayer mShiftLayer;
}
