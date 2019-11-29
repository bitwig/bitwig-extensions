package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bitwig.extension.controller.api.Clip;
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

      final Track cursorTrack = mDriver.getCursorTrack();
      cursorTrack.subscribe();
      cursorTrack.color().subscribe();

      final PlayingNoteArrayValue playingNotes = cursorTrack.playingNotes();
      playingNotes.subscribe();
   }

   @Override
   protected void doDeactivate()
   {
      final Clip clip = mDriver.getCursorClip();
      clip.color().unsubscribe();

      if (clip.exists().get())
         clearNoteInputRouting();

      final Track cursorTrack = mDriver.getCursorTrack();
      final PlayingNoteArrayValue playingNotes = cursorTrack.playingNotes();
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

      final Track cursorTrack = mDriver.getCursorTrack();
      cursorTrack.addNoteSource(noteInput);
   }

   private void clearNoteInputRouting()
   {
      final NoteInput noteInput = mDriver.getNoteInput();
      noteInput.includeInAllInputs().set(true);

      final Track cursorTrack = mDriver.getCursorTrack();
      cursorTrack.removeNoteSource(noteInput);
   }

   protected void setDataMode(final DataMode dataMode)
   {
      if (mDataMode == dataMode)
         return;

      mDriver.getHost().showPopupNotification(getDataModeDescription(dataMode));
      mDataMode = dataMode;
      mDriver.updateKeyTranslationTable();
   }

   @Override
   protected String getModeDescription()
   {
      return getDataModeDescription(mDataMode);
   }

   protected abstract String getDataModeDescription(final DataMode dataMode);

   protected int calculateAbsoluteStepIndex(final int x, final int y)
   {
      return mPage * 32 + x + 8 * y;
   }

   protected LedState computeMixDataLedState(final int x, final int y)
   {
      // TODO: cache it using the flush iteration
      final List<Button> pads = findStepsInPressedOrHoldState();

      if (pads.isEmpty())
      {
         switch (y)
         {
            case 0: return LedState.OFF;
            case 1: return LedState.PAN_MODE_LOW;
            case 2: return new LedState(Color.CYAN_LOW);
            case 3: return new LedState(Color.WHITE_LOW);
            default: throw new IllegalStateException();
         }
      }

      final Button pad = pads.get(0);
      final int absoluteStepIndex = calculateAbsoluteStepIndex(pad.getX() - 1, 8 - pad.getY());
      final NoteStep noteStep = findStepInfo(absoluteStepIndex);

      final double velocity = noteStep.velocity();
      final double duration = noteStep.duration();
      final double pan = noteStep.pan();

      switch (y)
      {
         case 0:
            return LedState.OFF;
         case 1:
            final double ipan = (x - 3.5) / 3.5;
            if ((pan > 0 && ipan > 0 && ipan <= pan) || (pan < 0 && ipan < 0 && pan <= ipan))
               return LedState.PAN_MODE;
            return LedState.PAN_MODE_LOW;
         case 2:
            return new LedState(computeDuration(x) <= duration ? Color.CYAN : Color.CYAN_LOW);
         case 3:
            return new LedState(x <= velocity * 7 ? Color.WHITE : Color.WHITE_LOW);
         default:
            throw new IllegalStateException();
      }
   }

   protected LedState computeSoundDataLedState(final int x, final int y)
   {
      // TODO: cache it using the flush iteration
      final List<Button> pads = findStepsInPressedOrHoldState();

      if (pads.isEmpty())
      {
         switch (y)
         {
            case 0: return LedState.OFF;
            case 1: return new LedState(Color.BLUE_LOW);
            case 2: return new LedState(Color.YELLOW_LOW);
            case 3: return new LedState(Color.WHITE_LOW);
            default: throw new IllegalStateException();
         }
      }

      final Button pad = pads.get(0);
      final int absoluteStepIndex = calculateAbsoluteStepIndex(pad.getX() - 1, 8 - pad.getY());
      final NoteStep noteStep = findStepInfo(absoluteStepIndex);

      final double transpose = noteStep.transpose();
      final double pressure = noteStep.pressure();
      final double timbre = noteStep.timbre();

      switch (y)
      {
         case 0:
            return LedState.OFF;
         case 1:
            return new LedState(x <= pressure * 7 ? Color.BLUE : Color.BLUE_LOW);
         case 2:
            final double itimbre = (x - 3.5) / 3.5;
            if ((timbre > 0 && itimbre > 0 && itimbre <= timbre) || (timbre < 0 && itimbre < 0 && timbre <= itimbre))
               return new LedState(Color.YELLOW);
            return new LedState(Color.YELLOW_LOW);
         case 3:
            final double itranspose = computeTranspoose(x);
            if ((transpose > 0 && itranspose > 0 && itranspose <= transpose) || (transpose < 0 && itranspose < 0 && transpose <= itranspose))
               return new LedState(Color.WHITE);
            return new LedState(Color.WHITE_LOW);
         default:
            throw new IllegalStateException();
      }
   }

   abstract protected NoteStep findStepInfo(int absoluteStepIndex);

   protected LedState computeDataChoiceLedState(final int y)
   {
      switch (y)
      {
         case 0:
            return new LedState(mDataMode == DataMode.SoundData ? Color.YELLOW : Color.YELLOW_LOW);
         case 1:
            return new LedState(mDataMode == DataMode.MixData ? Color.YELLOW : Color.YELLOW_LOW);
         case 2:
            return new LedState(hasMainAltMode() ? mDataMode == DataMode.MainAlt ? Color.YELLOW : Color.YELLOW_LOW : Color.OFF);
         case 3:
            return new LedState(mDataMode == DataMode.Main ? Color.YELLOW : Color.YELLOW_LOW);
      }
      throw new IllegalStateException();
   }

   protected LedState computePatternOffsetLedState(final int y)
   {
      final Clip clip = mDriver.getCursorClip();

      final SettableBeatTimeValue playStart = clip.getPlayStart();
      final SettableBeatTimeValue playStopTime = clip.getPlayStop();
      final double length = playStopTime.get() - playStart.get();
      final int playingStep = clip.playingStep().get();

      if (playingStep / 32 == y)
         return new LedState(mPage == y ? Color.GREEN : Color.GREEN_LOW);
      else if (8 * y < length)
         return new LedState(mPage == y ? Color.WHITE : Color.WHITE_LOW);
      return LedState.OFF;
   }

   protected LedState computeClipLengthSelectionLedState(final int y)
   {
      final Clip clip = mDriver.getCursorClip();
      final SettableBeatTimeValue loopLength = clip.getLoopLength();
      final double duration = loopLength.get();

      return new LedState((y + 1) * 4 <= duration ? Color.WHITE : Color.WHITE_LOW);
   }

   protected void setClipLength(final double lengthInBars)
   {
      final Clip cursorClip = mDriver.getCursorClip();
      cursorClip.getPlayStart().set(0);
      cursorClip.getPlayStop().set(lengthInBars);
      cursorClip.getLoopStart().set(0);
      cursorClip.getLoopLength().set(lengthInBars);
   }

   protected void setDataMode(final int Y)
   {
      assert 0 <= Y && Y <= 3;
      switch (Y)
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
