package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.CursorTrack;

public final class KeyboardMode extends Mode
{
   public KeyboardMode(final LaunchpadProControllerExtension driver)
   {
      super(driver, "keyboard");

      final CursorTrack cursorTrack = driver.getCursorTrack();

      mKeyboardLayer = new KeyboardLayer(driver, "keyboard", 0, 0, 8, 8, () -> new Color(mDriver.getCursorTrack().color()));

      bindPressed(driver.getRightButton(), cursorTrack.selectNextAction());
      bindPressed(driver.getLeftButton(), cursorTrack.selectPreviousAction());
      bindPressed(driver.getUpButton(), () -> {
         mKeyboardLayer.octaveUp();
         mDriver.updateKeyTranslationTable();
      });
      bindPressed(driver.getDownButton(), () -> {
         mKeyboardLayer.octaveDown();
         mDriver.updateKeyTranslationTable();
      });

      bindLightState(LedState.PLAY_MODE, driver.getNoteButton());
      bindLightState(() -> cursorTrack.hasNext().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getRightButton());
      bindLightState(() -> cursorTrack.hasPrevious().get() ? LedState.TRACK : LedState.TRACK_LOW, driver.getLeftButton());
      bindLightState(() -> mKeyboardLayer.canOctaveDown() ? LedState.PITCH : LedState.PITCH_LOW, driver.getDownButton());
      bindLightState(() -> mKeyboardLayer.canOctaveUp() ? LedState.PITCH : LedState.PITCH_LOW, driver.getUpButton());
   }

   @Override
   protected String getModeDescription()
   {
      switch (mDriver.getKeyboardLayout())
      {
         case GUITAR:
            return "Play: Guitar Layout";
         case LINE_3:
            return "Play: Line/3 Layout";
         case LINE_7:
            return "Play: Line/7 Layout";
         case PIANO:
            return "Play: Piano Layout";
      }
      return "Play ???";
   }

   @Override
   public void doActivate()
   {
      mKeyboardLayer.activate();
   }

   @Override
   protected void doDeactivate()
   {
      mKeyboardLayer.deactivate();
      mDriver.getNoteInput().setKeyTranslationTable(LaunchpadProControllerExtension.FILTER_ALL_NOTE_MAP);
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      mKeyboardLayer.updateKeyTranslationTable(table);
   }

   public void invalidate()
   {
      if (!isActive())
         return;

      mDriver.updateKeyTranslationTable();
   }

   private final KeyboardLayer mKeyboardLayer;
}
