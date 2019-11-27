package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.CursorTrack;

public final class KeyboardMode extends Mode
{
   private static final int[] CHROMATIC_NOTE_INDEXES = new int[]{
      0,  2, 4,  5, 7, 9, 11, 12,
      -1, 1, 3, -1, 6, 8, 10, -1,
   };

   private final Color UP_DOWN_ON_COLOR = new Color(0.f, 0.f, 1.f);
   private final Color UP_DOWN_OFF_COLOR = new Color(0.f, 0.f, 0.2f);
   private final Color LEFT_RIGHT_ON_COLOR = new Color(0.f, 1.f, 0.f);
   private final Color LEFT_RIGHT_OFF_COLOR = new Color(0.f, 0.2f, 0.f);

   private final static Color KEYBOARD_ON_COLOR = Color.fromRgb255(11, 100, 63);
   private final static Color KEYBOARD_OFF_COLOR = Color.scale(KEYBOARD_ON_COLOR, 0.2f);
   private final static Color ROOT_KEY_COLOR = Color.fromRgb255(11, 100, 63);
   private final static Color USED_WHITE_KEY_COLOR = Color.fromRgb255(255, 240, 240);
   private final static Color UNUSED_KEY_COLOR = Color.fromRgb255(50, 50, 55);
   private final static Color USED_BLACK_KEY_COLOR = Color.fromRgb255(120, 85, 42);
   private final static Color PLAYING_KEY_COLOR = Color.fromRgb255(0, 255, 0);
   private final static Color INVALID_KEY_COLOR = Color.RED_LOW;

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

      bindLightState(LedState.PLAY_MODE, driver.getDeviceButton());
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
      mDriver.getNoteInput().setKeyTranslationTable(LaunchpadProControllerExtension.FILTER_ALL_NOTE_MAP);
      mKeyboardLayer.deactivate();
   }

   @Override
   public void paintModeButton()
   {
      final Button button = mDriver.getButtonOnTheTop(5);
      button.setColor(isActive() ? KEYBOARD_ON_COLOR : KEYBOARD_OFF_COLOR);
   }

   @Override
   public void paint()
   {
      super.paint();

      final CursorTrack cursorTrack = mDriver.getCursorTrack();

      mDriver.getButtonOnTheRight(7).setColor(mDriver.getKeyboardLayout() == KeyboardLayout.GUITAR ? KEYBOARD_ON_COLOR : KEYBOARD_OFF_COLOR);
      mDriver.getButtonOnTheRight(6).setColor(mDriver.getKeyboardLayout() == KeyboardLayout.LINE_3 ? KEYBOARD_ON_COLOR : KEYBOARD_OFF_COLOR);
      mDriver.getButtonOnTheRight(5).setColor(mDriver.getKeyboardLayout() == KeyboardLayout.LINE_7 ? KEYBOARD_ON_COLOR : KEYBOARD_OFF_COLOR);
      mDriver.getButtonOnTheRight(4).setColor(mDriver.getKeyboardLayout() == KeyboardLayout.PIANO ? KEYBOARD_ON_COLOR : KEYBOARD_OFF_COLOR);
      for (int i = 0; i < 4; ++i)
         mDriver.getButtonOnTheRight(i).clear();

      mDriver.getButtonOnTheTop(0).setColor(mKeyboardLayer.canOctaveUp() ? Color.PITCH : Color.PITCH_LOW);
      mDriver.getButtonOnTheTop(1).setColor(mKeyboardLayer.canOctaveDown() ? Color.PITCH : Color.PITCH_LOW);
      mDriver.getButtonOnTheTop(2).setColor(cursorTrack.hasPrevious().get() ? Color.TRACK : Color.TRACK_LOW);
      mDriver.getButtonOnTheTop(3).setColor(cursorTrack.hasNext().get() ? Color.TRACK : Color.TRACK_LOW);
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
      paint();
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
   public void onArrowLeftPressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getCursorTrack().selectFirst();
      else
         mDriver.getCursorTrack().selectPrevious();
   }

   @Override
   public void onArrowRightPressed()
   {
      if (mDriver.isShiftOn())
         mDriver.getCursorTrack().selectLast();
      else
         mDriver.getCursorTrack().selectNext();
   }

   private final KeyboardLayer mKeyboardLayer;
}
