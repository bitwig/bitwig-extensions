package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.Track;

public class StepSequencerMode extends AbstractSequencerMode
{
   StepSequencerMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "step-sequencer");

      mKeyboardLayer = new KeyboardLayer(driver, "step-sequencer-keyboard", 0, 0, 8, 4, () -> {
         final boolean cursorClipExists = mDriver.getCursorClip().exists().get();
         final ColorValue trackColor = (cursorClipExists ? mDriver.getCursorClipTrack() : mDriver.getCursorTrack()).color();
         return new Color(trackColor);
      });

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 4; ++y)
         {
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
         }
      }
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      if (mDataMode == DataMode.Main)
         mKeyboardLayer.updateKeyTranslationTable(table);

      mDriver.getNoteInput().setKeyTranslationTable(table);
   }

   @Override
   protected void doActivate()
   {
      super.doActivate();

      final Track track = mDriver.getCursorClipTrack();
      track.subscribe();

      final SettableColorValue trackColor = track.color();
      trackColor.subscribe();

      track.selectInMixer();
      mKeyboardLayer.activate();
   }

   @Override
   protected void doDeactivate()
   {
      final Track track = mDriver.getCursorClipTrack();
      track.unsubscribe();

      final SettableColorValue trackColor = track.color();
      trackColor.unsubscribe();

      mKeyboardLayer.deactivate();

      super.doDeactivate();
   }

   @Override
   public void paint()
   {
      super.paint();

      paintScenes();
      paintSteps();
      paintArrows();

      switch (mDataMode)
      {
         case Main:
         case MainAlt:
         {
            final boolean cursorClipExists = mDriver.getCursorClip().exists().get();
            final SettableColorValue trackColor =
               (cursorClipExists ? mDriver.getCursorClipTrack() : mDriver.getCursorTrack()).color();
            //mKeyboardLayer.paint(trackColor);
            paintCurrentKeysOnStep();
            break;
         }
         case MixData:
            paintMixData();
            break;
         case SoundData:
            paintSoundData();
            break;
      }
   }

   private void paintArrows()
   {
      mDriver.getButtonOnTheTop(0).setColor(mKeyboardLayer.canOctaveUp() ? Color.PITCH : Color.PITCH_LOW);
      mDriver.getButtonOnTheTop(1).setColor(mKeyboardLayer.canOctaveDown() ? Color.PITCH : Color.PITCH_LOW);
      mDriver.getButtonOnTheTop(2).setColor(Color.OFF);
      mDriver.getButtonOnTheTop(3).setColor(Color.OFF);
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

   private void paintCurrentKeysOnStep()
   {
      final Clip cursorClip = mDriver.getCursorClip();
      final List<Button> stepsInHoldState = findStepsInHoldState();
      for (Button button : stepsInHoldState)
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(button.getX() - 1, 8 - button.getY());

         for (int x = 0; x < 8; ++x)
         {
            for (int y = 0; y < 4; ++y)
            {
               final int key = mKeyboardLayer.calculateKeyForPosition(x, y);
               if (key == -1)
                  continue;

               assert key >= 0;
               assert key < 127;

               final NoteStep noteStep = cursorClip.getStep(0, absoluteStepIndex, key);
               if (noteStep.state() == NoteStep.State.NoteOn)
                  mDriver.getPadButton(x, y).setColor(Color.GREEN);
            }
         }
      }
   }

   private void paintSteps()
   {
      final Clip clip = mDriver.getCursorClip();
      final int playingStep = clip.playingStep().get();

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 4; ++y)
         {
            final int absoluteStepIndex = calculateAbsoluteStepIndex(x, y);
            final NoteStep noteStep = computeVerticalStepState(absoluteStepIndex);
            final Button button = mDriver.getPadButton(x, 7 - y);

            if (playingStep == mPage * 32 + 8 * y + x)
               button.setColor(Color.GREEN);
            else if (mDriver.getPadButton(x, 7- y).getButtonState() == Button.State.HOLD)
               button.setColor(Color.STEP_HOLD);
            else switch (noteStep.state())
            {
               case NoteOn:
                  button.setColor(Color.STEP_ON);
                  break;
               case NoteSustain:
                  button.setColor(Color.STEP_SUSTAIN);
                  break;
               case Empty:
                  button.setColor(Color.STEP_OFF);
                  break;
            }
         }
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
         }
      }
      return value;
   }

   @Override
   void paintModeButton()
   {
      mDriver.getButtonOnTheTop(7).setColor(isActive() ? MODE_COLOR : MODE_COLOR_LOW);
   }

   void invalidate()
   {
      if (!isActive())
         return;

      mDriver.updateKeyTranslationTable();
      paint();
   }

   @Override
   void onPadPressed(final int x, final int y, final int velocity)
   {
      if (y >= 4)
      {
         onStepPressed(calculateAbsoluteStepIndex(x, 7 - y), velocity);
         return;
      }

      switch (mDataMode)
      {
         case Main:
         case MainAlt:
            onKeyDataPressed(x, y, velocity);
            break;
         case MixData:
            onMixDataPressed(x, 3 - y);
            break;
         case SoundData:
            onSoundDataPressed(x, 3 - y);
            break;
      }
   }

   private void onMixDataPressed(final int x, final int y)
   {
      final Clip clip = mDriver.getCursorClip();
      final List<Button> padsInHoldState = mDriver.findPadsInHoldState();

      for (Button buttonState : padsInHoldState)
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

      for (Button buttonState : padsInHoldState)
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

   private void onKeyDataPressed(final int x, final int y, final int velocity)
   {
      final int key = mKeyboardLayer.calculateKeyForPosition(x, y);
      if (key == -1)
         return;

      final Clip cursorClip = mDriver.getCursorClip();
      for (Button buttonState : findStepsInPressedOrHoldState())
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(buttonState.getX() - 1, 8 - buttonState.getY());
         cursorClip.toggleStep(absoluteStepIndex, key, velocity);
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
}
