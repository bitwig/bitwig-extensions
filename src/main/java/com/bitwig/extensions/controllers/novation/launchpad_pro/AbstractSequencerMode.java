package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNoteArrayValue;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Track;

abstract class AbstractSequencerMode extends Mode
{
   protected AbstractSequencerMode(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);
   }

   protected enum DataMode
   {
      Main, MixData, SoundData, MainAlt,
   }

   @Override
   protected void doActivate()
   {
      final Clip clip = mDriver.getCursorClip();
      clip.color().subscribe();

      if (clip.exists().get())
         setNoteInputRouting();

      final Track cursorClipTrack = mDriver.getCursorClipTrack();
      cursorClipTrack.subscribe();
      cursorClipTrack.color().subscribe();

      final CursorTrack cursorTrack = mDriver.getCursorTrack();
      cursorTrack.subscribe();
      cursorTrack.color().subscribe();

      final PlayingNoteArrayValue playingNotes = cursorClipTrack.playingNotes();
      playingNotes.subscribe();
   }

   @Override
   protected void doDeactivate()
   {
      final Clip clip = mDriver.getCursorClip();
      clip.color().unsubscribe();

      if (clip.exists().get())
         clearNoteInputRouting();

      final Track cursorClipTrack = mDriver.getCursorClipTrack();
      final PlayingNoteArrayValue playingNotes = cursorClipTrack.playingNotes();
      playingNotes.unsubscribe();
   }

   @Override
   public void onCursorClipExists(final boolean exists)
   {
      if (!exists)
         clearNoteInputRouting();
      else
         setNoteInputRouting();
   }

   private void setNoteInputRouting()
   {
      final NoteInput noteInput = mDriver.getNoteInput();
      noteInput.includeInAllInputs().set(false);

      final Track cursorClipTrack = mDriver.getCursorClipTrack();
      cursorClipTrack.addNoteSource(noteInput);
   }

   private void clearNoteInputRouting()
   {
      final NoteInput noteInput = mDriver.getNoteInput();
      noteInput.includeInAllInputs().set(true);

      final Track cursorClipTrack = mDriver.getCursorClipTrack();
      cursorClipTrack.removeNoteSource(noteInput);
   }

   protected void setDataMode(final DataMode dataMode)
   {
      if (mDataMode == dataMode)
         return;

      mDriver.getHost().showPopupNotification(getDataModeDescription(dataMode));
      mDataMode = dataMode;
      mDriver.updateKeyTranslationTable();
      paint();
   }

   @Override
   protected String getModeDescription()
   {
      return getDataModeDescription(mDataMode);
   }

   protected abstract String getDataModeDescription(final DataMode dataMode);

   protected int calculateAbsoluteStepIndex(int x, int y)
   {
      return mPage * 32 + x + 8 * y;
   }

   protected void paintMixData()
   {
      final List<Button> pads = findStepsInPressedOrHoldState();

      if (pads.isEmpty())
      {
         for (int i = 0; i < 8; ++i)
         {
            mDriver.getPadButton(i, 3).setColor(Color.WHITE_LOW);
            mDriver.getPadButton(i, 2).setColor(Color.CYAN_LOW);
            mDriver.getPadButton(i, 1).setColor(Color.PAN_MODE_LOW);
            mDriver.getPadButton(i, 0).setColor(Color.OFF);
         }
         return;
      }

      final Button pad = pads.get(0);
      final int absoluteStepIndex = calculateAbsoluteStepIndex(pad.getX() - 1, 8 - pad.getY());
      final NoteStep noteStep = findStepInfo(absoluteStepIndex);

      final double velocity = noteStep.velocity();
      final double duration = noteStep.duration();
      final double pan = noteStep.pan();

      for (int i = 0; i < 8; ++i)
      {
         mDriver.getPadButton(i, 3).setColor(i <= velocity * 7 ? Color.WHITE : Color.WHITE_LOW);
         mDriver.getPadButton(i, 2).setColor(computeDuration(i) <= duration ? Color.CYAN : Color.CYAN_LOW);
         final double ipan = (i - 3.5) / 3.5;
         if ((pan > 0 && ipan > 0 && ipan <= pan) || (pan < 0 && ipan < 0 && pan <= ipan))
            mDriver.getPadButton(i, 1).setColor(Color.PAN_MODE);
         else
            mDriver.getPadButton(i, 1).setColor(Color.PAN_MODE_LOW);
         mDriver.getPadButton(i, 0).setColor(Color.OFF);
      }
   }

   protected void paintSoundData()
   {
      final List<Button> padsInHoldState = mDriver.findPadsInHoldState();

      if (padsInHoldState.isEmpty())
      {
         for (int i = 0; i < 8; ++i)
         {
            mDriver.getPadButton(i, 3).setColor(Color.WHITE_LOW);
            mDriver.getPadButton(i, 2).setColor(Color.YELLOW_LOW);
            mDriver.getPadButton(i, 1).setColor(Color.BLUE_LOW);
            mDriver.getPadButton(i, 0).setColor(Color.OFF);
         }
         return;
      }

      final Button pad = padsInHoldState.get(0);
      final int absoluteStepIndex = calculateAbsoluteStepIndex(pad.getX() - 1, 8 - pad.getY());
      final NoteStep noteStep = findStepInfo(absoluteStepIndex);

      final double transpose = noteStep.transpose();
      final double pressure = noteStep.pressure();
      final double timbre = noteStep.timbre();

      for (int i = 0; i < 8; ++i)
      {
         final double itranspose = computeTranspoose(i);

         if ((transpose > 0 && itranspose > 0 && itranspose <= transpose) || (transpose < 0 && itranspose < 0 && transpose <= itranspose))
            mDriver.getPadButton(i, 3).setColor(Color.WHITE);
         else
            mDriver.getPadButton(i, 3).setColor(Color.WHITE_LOW);

         final double itimbre = (i - 3.5) / 3.5;
         if ((timbre > 0 && itimbre > 0 && itimbre <= timbre) || (timbre < 0 && itimbre < 0 && timbre <= itimbre))
            mDriver.getPadButton(i, 2).setColor(Color.YELLOW);
         else
            mDriver.getPadButton(i, 2).setColor(Color.YELLOW_LOW);

         mDriver.getPadButton(i, 1).setColor(i <= pressure * 7 ? Color.BLUE : Color.BLUE_LOW);
         mDriver.getPadButton(i, 0).setColor(Color.OFF);
      }
   }

   abstract protected NoteStep findStepInfo(int absoluteStepIndex);

   protected void paintDataChoice()
   {
      mDriver.getButtonOnTheRight(0).setColor(mDataMode == DataMode.SoundData ? Color.YELLOW : Color.YELLOW_LOW);
      mDriver.getButtonOnTheRight(1).setColor(mDataMode == DataMode.MixData ? Color.YELLOW : Color.YELLOW_LOW);
      mDriver.getButtonOnTheRight(2).setColor(hasMainAltMode() ? mDataMode == DataMode.MainAlt ? Color.YELLOW : Color.YELLOW_LOW : Color.OFF);
      mDriver.getButtonOnTheRight(3).setColor(mDataMode == DataMode.Main ? Color.YELLOW : Color.YELLOW_LOW);
   }

   protected void paintScenes()
   {
      if (mDriver.isShiftOn())
         paintClipLengthSelection();
      else
      {
         paintPatternOffset();
         paintDataChoice();
      }
   }

   protected void paintPatternOffset()
   {
      final Clip clip = mDriver.getCursorClip();

      final SettableBeatTimeValue playStart = clip.getPlayStart();
      final SettableBeatTimeValue playStopTime = clip.getPlayStop();
      final double length = playStopTime.get() - playStart.get();

      final int playingStep = clip.playingStep().get();

      for (int i = 0; i < 4; ++i)
      {
         final Button button = mDriver.getButtonOnTheRight(7 - i);

         if (playingStep / 32 == i)
            button.setColor(mPage == i ? Color.GREEN : Color.GREEN_LOW);
         else if (8 * i < length)
            button.setColor(mPage == i ? Color.WHITE : Color.WHITE_LOW);
         else
            button.setColor(Color.OFF);
      }
   }

   protected void paintClipLengthSelection()
   {
      final Clip clip = mDriver.getCursorClip();
      final SettableBeatTimeValue loopLength = clip.getLoopLength();
      final double duration = loopLength.get();

      for (int i = 0; i < 8; ++i)
      {
         final Button button = mDriver.getButtonOnTheRight(7 - i);
         button.setColor((i + 1) * 4 <= duration ? Color.WHITE : Color.WHITE_LOW);
      }
   }

   protected void setClipLength(final double lengthInBars)
   {
      final Clip cursorClip = mDriver.getCursorClip();
      cursorClip.getPlayStart().set(0);
      cursorClip.getPlayStop().set(lengthInBars);
      cursorClip.getLoopStart().set(0);
      cursorClip.getLoopLength().set(lengthInBars);
   }

   @Override
   public void onSceneButtonPressed(final int column)
   {
      if (mDriver.isShiftOn())
         setClipLength((8 - column) * 4);
      else if (4 <= column && column <= 7)
         mPage = 7 - column;
      else
      {
         assert 0 <= column && column <= 3;
         switch (column)
         {
            case 0:
               setDataMode(DataMode.SoundData);
               break;

            case 1:
               setDataMode(DataMode.MixData);
               break;

            case 2:
               if (hasMainAltMode())
                  setDataMode(DataMode.MainAlt);
               break;

            case 3:
               setDataMode(DataMode.Main);
               break;
         }
      }
   }

   List<Button> findStepsInHoldState()
   {
      final ArrayList<Button> list = new ArrayList<>();
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 4; y < 8; ++y)
         {
            final Button padState = mDriver.getPadButton(x, y);
            if (padState.getButtonState() == Button.State.HOLD)
               list.add(padState);
         }
      }
      return list;
   }

   List<Button> findStepsInPressedOrHoldState()
   {
      final ArrayList<Button> list = new ArrayList<>();
      for (int x = 0; x < 8; ++x)
      {
         for (int y = 4; y < 8; ++y)
         {
            final Button bt = mDriver.getPadButton(x, y);
            if (bt.getButtonState() == Button.State.HOLD || bt.getButtonState() == Button.State.PRESSED)
               list.add(bt);
         }
      }
      return list;
   }

   protected double computeTranspoose(final int x)
   {
      switch (x)
      {
         case 0:
            return -0.5;
         case 1:
            return -0.25;
         case 2:
            return -0.125;
         case 5:
            return 0.125;
         case 6:
            return 0.25;
         case 7:
            return 0.5;
         case 3:
         case 4:
         default:
            return 0;
      }
   }

   protected double computeDuration(final int x)
   {
      return 1.0 / 16.0 * (1 << x);
   }

   @Override
   void onDeletePressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getCursorClip().clearSteps();
   }

   @Override
   void onQuantizePressed()
   {
      mDriver.getCursorClip().quantize(1);
   }

   protected boolean hasMainAltMode()
   {
      return true;
   }

   protected final Color MODE_COLOR = Color.fromRgb255(255, 183, 0);
   protected final Color MODE_COLOR_LOW = new Color(MODE_COLOR, .1f);

   protected DataMode mDataMode = DataMode.Main;
   protected int mPage = 0;
   protected Set<Integer> mStepsBeingAdded = new HashSet<>();
}
