package com.bitwig.extensions.controllers.novation.launchpad_pro;

public class MultiplexerMode extends Mode
{
   private static final Color ACTIVE_MODE_COLOR = Color.fromRgb255(255, 255, 255);
   private static final Color INACTIVE_MODE_COLOR = Color.scale(ACTIVE_MODE_COLOR, 0.2f);

   MultiplexerMode(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);
   }

   @Override
   public void doActivate()
   {
      mModes[mSelectedIndex].activate();
   }

   @Override
   public void doDeactivate()
   {
      mModes[mSelectedIndex].deactivate();
   }

   @Override
   protected String getModeDescription()
   {
      return mModes[mSelectedIndex].getModeDescription();
   }

   void setMode(final int index, final Mode mode, final Runnable action)
   {
      assert 0 <= index && index < 8;

      mModes[index] = mode;
      mModesAction[index] = action;
   }

   void setMode(final int index, final Mode mode)
   {
      setMode(index, mode, null);
   }

   @Override
   public void paint()
   {
      if (mIsInPaint)
         return;

      mIsInPaint = true;
      super.paint();
      mModes[mSelectedIndex].paint();
      mIsInPaint = false;

      for (int i = 0; i < 8; ++i)
      {
         final Button button = mDriver.getRightButton(7 - i);

         if (mModes[i] == null)
            button.clear();
         else if (mSelectedIndex == i)
            button.setColor(ACTIVE_MODE_COLOR);
         else
            button.setColor(INACTIVE_MODE_COLOR);
      }
   }

   @Override
   public void paintModeButton()
   {
      super.paintModeButton();

      mModes[mSelectedIndex].paintModeButton();
   }

   @Override
   public void onPadPressed(final int x, final int y, final int velocity)
   {
      mModes[mSelectedIndex].onPadPressed(x, y, velocity);
   }

   @Override
   public void onPadReleased(final int x, final int y, final int velocity, final boolean wasHeld)
   {
      mModes[mSelectedIndex].onPadReleased(x, y, velocity, wasHeld);
   }

   @Override
   public void onArrowUpReleased()
   {
      mModes[mSelectedIndex].onArrowUpReleased();
   }

   @Override
   public void onArrowUpPressed()
   {
      mModes[mSelectedIndex].onArrowUpPressed();
   }

   @Override
   public void onArrowDownReleased()
   {
      mModes[mSelectedIndex].onArrowDownReleased();
   }

   @Override
   public void onArrowDownPressed()
   {
      mModes[mSelectedIndex].onArrowDownPressed();
   }

   @Override
   public void onArrowRightPressed()
   {
      mModes[mSelectedIndex].onArrowRightPressed();
   }

   @Override
   public void onArrowRightReleased()
   {
      mModes[mSelectedIndex].onArrowRightReleased();
   }

   @Override
   public void onArrowLeftPressed()
   {
      mModes[mSelectedIndex].onArrowLeftPressed();
   }

   @Override
   public void onArrowLeftReleased()
   {
      mModes[mSelectedIndex].onArrowLeftReleased();
   }

   @Override
   public void onSceneButtonPressed(final int column)
   {
      selectMode(7 - column);
   }

   public void selectMode(final int index)
   {
      if ((index < 0 && 7 < index) ||
         index == mSelectedIndex ||
         mModes[index] == null)
         return;

      if (mModes[mSelectedIndex] != null)
         mModes[mSelectedIndex].deactivate();

      if (mModesAction[index] != null)
         mModesAction[index].run();

      mModes[index].activate();
      mSelectedIndex = index;

      mDriver.updateKeyTranslationTable();
      mDriver.scheduleFlush();
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      mModes[mSelectedIndex].updateKeyTranslationTable(table);
   }

   private boolean mIsInPaint = false;
   private int mSelectedIndex = 0;

   private final Mode[] mModes = {null, null, null, null, null, null, null, null};
   private final Runnable[] mModesAction = {null, null, null, null, null, null, null, null};
}
