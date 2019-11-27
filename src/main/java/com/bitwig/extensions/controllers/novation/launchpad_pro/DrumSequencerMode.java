package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Track;

final class DrumSequencerMode extends AbstractSequencerMode
{
   DrumSequencerMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "drum-sequencer");

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
   protected void doActivate()
   {
      super.doActivate();

      final Track track = mDriver.getCursorClipTrack();
      track.subscribe();

      final SettableColorValue trackColor = track.color();
      trackColor.subscribe();

      mDriver.getCursorClipTrack().selectInMixer();
      final DrumPadBank cursorClipDrumPads = mDriver.getCursorClipDrumPads();
      cursorClipDrumPads.setIndication(true);

      updateDrumPadsBankPosition();
   }

   @Override
   protected void doDeactivate()
   {
      final Track track = mDriver.getCursorClipTrack();
      track.unsubscribe();

      final SettableColorValue trackColor = track.color();
      trackColor.unsubscribe();

      final DrumPadBank cursorClipDrumPads = mDriver.getCursorClipDrumPads();
      cursorClipDrumPads.setIndication(false);

      super.doDeactivate();
   }

   @Override
   public void onArrowUpPressed()
   {
      final DrumPadBank drumPads = mDriver.getCursorClipDrumPads();
      final SettableIntegerValue position = drumPads.scrollPosition();

      if (position.get() == 0)
         position.set(4);
      else
         position.set(Math.min(116, position.get() + 16));
      updateDrumPadsBankPosition();
   }

   @Override
   public void onArrowDownPressed()
   {
      final DrumPadBank drumPads = mDriver.getCursorClipDrumPads();
      final SettableIntegerValue position = drumPads.scrollPosition();

      position.set(Math.max(0, position.get() - 16));
      updateDrumPadsBankPosition();
   }

   private void updateDrumPadsBankPosition()
   {
      mDriver.updateKeyTranslationTable();
   }

   @Override
   void onShiftPressed()
   {
      mDriver.updateKeyTranslationTable();
   }

   @Override
   void onShiftReleased()
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
      final DrumPadBank cursorClipDrumPads = mDriver.getCursorClipDrumPads();

      if (cursorClipDrumPads.exists().get())
         return x + 4 * y + cursorClipDrumPads.scrollPosition().get();
      return x + 4 * y + 36;
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      if (y >= 4)
      {
         onStepPressed(calculateAbsoluteStepIndex(x, 7 - y), velocity);
         return;
      }

      switch (mDataMode)
      {
         case Main:
            if (x < 4)
               onDrumPadPressed(x, y);
            else
               onDrumActionPressed(x - 4, y, velocity);
            break;
         case MainAlt:
            if (x < 4)
               onDrumPadPressed(x, y);
            else if (y < 2)
               onDrumScenePressed(x, y);
            else
               onDrumPerfPressed(x, y - 2);
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
      final List<Button> padsInHoldState = findStepsInPressedOrHoldState();

      for (Button button : padsInHoldState)
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(button.getX() - 1, 8 - button.getY());
         final NoteStep noteStep = clip.getStep(0, absoluteStepIndex, mCurrentPitch);

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

   private void onSoundDataPressed(final int x, final int y)
   {
      final Clip clip = mDriver.getCursorClip();
      final List<Button> padsInHoldState = findStepsInPressedOrHoldState();

      for (Button buttonState : padsInHoldState)
      {
         final int absoluteStepIndex = calculateAbsoluteStepIndex(buttonState.getX() - 1, 8 - buttonState.getY());
         final NoteStep noteStep = clip.getStep(0, absoluteStepIndex, mCurrentPitch);

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

   @Override
   void onPadPressure(final int x, final int y, final int pressure)
   {
      if (y >= 4)
         return;

      switch (mDataMode)
      {
         case Main:
            if (x == 4 && y == 2)
               onAutoNoteRepeatPressure(pressure);
            break;
         case MainAlt:
            if (x >= 4 && y >= 2)
               onDrumPerfPressure(x, y, pressure);
            break;
         case MixData:
            break;
         case SoundData:
            break;
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
            if (x >= 4)
               onDrumActionReleased(x - 4, y);
            break;
         case MixData:
            if (x >= 4 && y >= 2)
               onDrumPerfReleased(x, y);
            break;
         case SoundData:
            break;
         case MainAlt:
            break;
      }
   }

   private void onDrumActionPressed(final int x, final int y, final int velocity)
   {
      if (y == 0 && (x >= 0 && x <= 2))
      {
         if (mDriver.isDeleteOn() && x == 1 && y == 0)
            mDriver.getCursorClipDrumPads().clearMutedPads();
         if (mDriver.isDeleteOn() && x == 2 && y == 0)
            mDriver.getCursorClipDrumPads().clearSoloedPads();
         mDriver.getNoteInput().setKeyTranslationTable(LaunchpadProControllerExtension.FILTER_ALL_NOTE_MAP);
      }
      else if (y == 1)
      {
         final Clip cursorClip = mDriver.getCursorClip();
         final boolean cursorClipExists = cursorClip.exists().get();
         final int sceneIndex = cursorClipExists ? cursorClip.clipLauncherSlot().sceneIndex().get() : 0;
         final Track track = cursorClipExists ? cursorClip.getTrack() : mDriver.getCursorTrack();

         if (x == 1)
            track.createNewClip(sceneIndex, 8);
         else if (x == 2)
            track.recordNewClip(sceneIndex);
         else if (x == 3 && cursorClipExists)
            cursorClip.launch();
      }
      else if (y == 2 || y == 3)
      {
         final double period;

         if (y == 3 || x != 0)
            period = calculateNoteRepeatPeriodForPad(x, y);
         else
            period = calculateAutoNoteRepeatPeriod(velocity);

         final Arpeggiator arpeggiator = mDriver.getArpeggiator();
         arpeggiator.period().set(period);
         arpeggiator.usePressureToVelocity().set(true);
         arpeggiator.shuffle().set(true);
         arpeggiator.mode().set("all"); // that's the note repeat way
         arpeggiator.isEnabled().set(true);

         mNoteRepeatStack.add(new Coord(x, y));
      }
   }

   private double calculateNoteRepeatPeriodForPad(int x, int y)
   {
      if (y == 3)
         return 1.0 / 8.0 * (1 << x);
      if (y == 2 && x > 0)
         return 1.0 / 6.0 * (1 << (x - 1));
      return 0.25;
   }

   private void onAutoNoteRepeatPressure(final int pressure)
   {
      final Arpeggiator arpeggiator = mDriver.getArpeggiator();
      arpeggiator.period().set(calculateAutoNoteRepeatPeriod(pressure));
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
         final Arpeggiator arpeggiator = mDriver.getArpeggiator();

         mNoteRepeatStack.removeIf(coord -> coord.x == x && coord.y == y);

         if (!mNoteRepeatStack.isEmpty())
         {
            final Coord coord = mNoteRepeatStack.get(mNoteRepeatStack.size() - 1);
            if (coord.y != 2 || coord.x != 0)
            {
               final double periodForPad = calculateNoteRepeatPeriodForPad(coord.x, coord.y);
               arpeggiator.period().set(periodForPad);
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
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.getDrumPerfsRemoteControls();
      final RemoteControl parameter = drumPerfsRemoteControls.getParameter(x - 4 + 4 * (3 - y));

      if (!mDriver.isShiftOn() && parameter.exists().get())
         parameter.set(0);
   }

   private void onDrumPerfPressure(final int x, final int y, final int pressure)
   {
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.getDrumPerfsRemoteControls();
      final RemoteControl parameter = drumPerfsRemoteControls.getParameter(x - 4 + 4 * (3 - y));

      if (!mDriver.isShiftOn() && parameter.exists().get())
         parameter.set(pressure / 127.0);
   }

   private void onDrumPerfPressed(final int x, final int y)
   {

   }

   private void onDrumScenePressed(final int x, final int y)
   {
      final int paramIndex = x - 4 + 4 * (1 - y);
      final CursorRemoteControlsPage scenesRemoteControls = mDriver.getDrumScenesRemoteControls();

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
      final Clip cursorClip = mDriver.getCursorClip();

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
      final Clip cursorClip = mDriver.getCursorClip();

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
         mDriver.getCursorClip().clearSteps(key);
      else if (isDrumPadMuteOn())
      {
         final DrumPadBank cursorClipDrumPads = mDriver.getCursorClipDrumPads();
         final DrumPad drumPad = cursorClipDrumPads.getItemAt(x + 4 * y);
         drumPad.mute().toggle();
      }
      else if (isDrumPadSoloOn())
      {
         final DrumPadBank cursorClipDrumPads = mDriver.getCursorClipDrumPads();
         final DrumPad drumPad = cursorClipDrumPads.getItemAt(x + 4 * y);
         drumPad.solo().toggle();
      }
      else if (isDrumPadSelectOn())
      {
         final DrumPadBank cursorClipDrumPads = mDriver.getCursorClipDrumPads();
         final DrumPad drumPad = cursorClipDrumPads.getItemAt(x + 4 * y);
         drumPad.selectInEditor();
         mCurrentPitch = key;
      }
      else
         mCurrentPitch = key;
   }

   private boolean isActionOn(int x, int y)
   {
      if (mDataMode != DataMode.Main)
         return false;

      final Button button = mDriver.getPadButton(x, y);
      return button.isPressed();
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
   public void paint()
   {
      super.paint();

      paintArrows();
      paintSteps();
      paintScenes();

      switch (mDataMode)
      {
         case Main:
            paintDrumPads();
            paintMainActions();
            break;
         case MainAlt:
            paintDrumPads();
            paintPerfAndScenes();
            break;
         case MixData:
            paintMixData();
            break;
         case SoundData:
            paintSoundData();
            break;
      }
   }

   private void paintMainActions()
   {
      final boolean isDeleteOn = mDriver.isDeleteOn();

      mDriver.getPadButton(4, 0).setColor(isDrumPadSelectOn() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getPadButton(5, 0).setColor(isDrumPadMuteOn() || isDeleteOn ? Color.MUTE : Color.MUTE_LOW);
      mDriver.getPadButton(6, 0).setColor(isDrumPadSoloOn() || isDeleteOn  ? Color.SOLO : Color.SOLO_LOW);
      mDriver.getPadButton(7, 0).setColor(Color.OFF);

      mDriver.getPadButton(4, 1).setColor(Color.OFF);
      mDriver.getPadButton(5, 1).setColor(isActionOn(5, 1) ? Color.BLUE : Color.BLUE_LOW);
      mDriver.getPadButton(6, 1).setColor(isActionOn(6, 1) ? Color.RED : Color.RED_LOW);
      mDriver.getPadButton(7, 1).setColor(isActionOn(7, 1) ? Color.GREEN : Color.GREEN_LOW);

      mDriver.getPadButton(4, 2).setColor(isActionOn(4, 2) ? Color.PURPLE : Color.PURPLE_LOW);
      mDriver.getPadButton(5, 2).setColor(isActionOn(5, 2) ? Color.ORANGE : Color.ORANGE_LOW);
      mDriver.getPadButton(6, 2).setColor(isActionOn(6, 2) ? Color.ORANGE : Color.ORANGE_LOW);
      mDriver.getPadButton(7, 2).setColor(isActionOn(7, 2) ? Color.ORANGE : Color.ORANGE_LOW);

      mDriver.getPadButton(4, 3).setColor(isActionOn(4, 3) ? Color.YELLOW : Color.YELLOW_LOW);
      mDriver.getPadButton(5, 3).setColor(isActionOn(5, 3) ? Color.YELLOW : Color.YELLOW_LOW);
      mDriver.getPadButton(6, 3).setColor(isActionOn(6, 3) ? Color.YELLOW : Color.YELLOW_LOW);
      mDriver.getPadButton(7, 3).setColor(isActionOn(7, 3) ? Color.YELLOW : Color.YELLOW_LOW);
   }

   @Override
   protected NoteStep findStepInfo(final int absoluteStepIndex)
   {
      return mDriver.getCursorClip().getStep(0, absoluteStepIndex, mCurrentPitch);
   }

   private void paintPerfAndScenes()
   {
      final CursorRemoteControlsPage drumPerfsRemoteControls = mDriver.getDrumPerfsRemoteControls();
      final CursorRemoteControlsPage drumScenesRemoteControls = mDriver.getDrumScenesRemoteControls();
      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl perfParam = drumPerfsRemoteControls.getParameter(i);
         final Button perfButton = mDriver.getPadButton(4 + (i % 4), 3 - i / 4);

         final RemoteControl sceneParam = drumScenesRemoteControls.getParameter(i);
         final Button sceneButton = mDriver.getPadButton(4 + (i % 4), 1 - i / 4);

         if (perfParam.exists().get())
            perfButton.setColor(Color.scale(Color.CYAN, (float) (0.95 * perfParam.get() + 0.05)));
         else
            perfButton.setColor(Color.OFF);

         if (sceneParam.exists().get())
            sceneButton.setColor(Color.scale(Color.YELLOW, (float) (0.9 * sceneParam.get() + 0.1)));
         else
            sceneButton.setColor(Color.OFF);
      }
   }

   private void paintArrows()
   {
      final DrumPadBank drumPads = mDriver.getCursorClipDrumPads();
      final int pos = drumPads.scrollPosition().get();

      mDriver.getButtonOnTheTop(0).setColor(pos < 116 ? Color.PITCH : Color.PITCH_LOW);
      mDriver.getButtonOnTheTop(1).setColor(pos > 0 ? Color.PITCH : Color.PITCH_LOW);
      mDriver.getButtonOnTheTop(2).setColor(Color.OFF);
      mDriver.getButtonOnTheTop(3).setColor(Color.OFF);
   }

   private void paintSteps()
   {
      final Clip clip = mDriver.getCursorClip();
      final int playingStep = clip.playingStep().get();

      for (int x = 0; x < 8; ++x)
      {
         for (int y = 0; y < 4; ++y)
         {
            final NoteStep noteStep = clip.getStep(0, calculateAbsoluteStepIndex(x, y), mCurrentPitch);
            final Button button = mDriver.getPadButton(x, 7 - y);

            if (playingStep == mPage * 32 + 8 * y + x)
               button.setColor(noteStep.state() == NoteStep.State.NoteOn ? Color.STEP_PLAY : Color.STEP_PLAY_HEAD);
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

   private void paintDrumPads()
   {
      final Clip clip = mDriver.getCursorClip();
      final boolean clipExists = clip.exists().get();
      final PlayingNoteArrayValue playingNotes = (clipExists ? mDriver.getCursorClipTrack() :mDriver.getCursorTrack()).playingNotes();
      final CursorDevice cursorDevice = clipExists ? mDriver.getCursorClipDevice() : mDriver.getCursorDevice();
      final boolean hasDrumPads = cursorDevice.hasDrumPads().get();
      final DrumPadBank drumPads = clipExists ? mDriver.getCursorClipDrumPads() : mDriver.getCursorTrackDrumPads();

      for (int i = 0; i < 4; ++i)
      {
         for (int j = 0; j < 4; ++j)
         {
            final int pitch = calculateDrumPadKey(i, j);
            final boolean isPlaying = playingNotes.isNotePlaying(pitch);
            final Button button = mDriver.getPadButton(i, j);
            final DrumPad drumPad = drumPads.getItemAt(i + 4 * j);
            final boolean drumPadExists = hasDrumPads & drumPad.exists().get();
            final boolean drumPadIsSolo = drumPadExists & drumPad.solo().get();
            final boolean drumPadIsMuted = drumPadExists & !drumPadIsSolo & (drumPad.mute().get() | drumPad.isMutedBySolo().get());
            final Color color = new Color(drumPad.color());

            if (isPlaying)
               button.setColor(drumPadIsMuted ? Color.GREEN_LOW : Color.GREEN);
            else if (mCurrentPitch == pitch)
               button.setColor(drumPadIsMuted ? Color.TRACK_LOW : Color.TRACK);
            else if (hasDrumPads)
            {
               if (drumPadExists)
                  button.setColor(drumPadIsSolo ? Color.YELLOW : drumPadIsMuted ? Color.scale(color, .1f) : color);
               else
                  button.setColor(Color.WHITE_LOW);
            }
            else
               button.setColor(clip.color());
         }
      }
   }

   @Override
   void paintModeButton()
   {
      mDriver.getButtonOnTheTop(6).setColor(isActive() ? MODE_COLOR : MODE_COLOR_LOW);
   }

   @Override
   protected String getDataModeDescription(final DataMode dataMode)
   {
      switch (dataMode)
      {
         case Main:
            return "Drum Sequencer: Note Repeat, Solo, Mute";
         case MainAlt:
            return "Drum Sequencer: Perfs, Scenes";
         case MixData:
            return "Drum Sequencer: Velocity, Note Length, Pan";
         case SoundData:
            return "Drum Sequencer: Pich Offset, Timbre, Pressure";
         default:
            return "Error";
      }
   }

   public void invalidateDrumPosition(final int newPosition)
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

   private List<Coord> mNoteRepeatStack = new ArrayList<>();
   private int mCurrentPitch = 36;
}
