package com.bitwig.extensions.controllers.novation.launchpad_pro;

class MultiplexerMode extends Mode
{
   MultiplexerMode(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);

      for (int i = 0; i < 8; ++i)
      {
         final int I = i;
         final Button button = driver.getSceneButton(7 - i);
         bindPressed(button, () -> selectMinorMode(I));
         bindLightState(() -> {
            if (mModes[I] == null)
               return LedState.OFF;
            return mSelectedIndex == I ? mModesLedOn[I] : mModesLedOff[I];
         }, button);
      }
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

   void setMode(final int index, final Mode mode, final Runnable action, final LedState ledOn, final LedState ledOff)
   {
      assert 0 <= index && index < 8;

      mModes[index] = mode;
      mModesAction[index] = action;
      mModesLedOn[index] = ledOn == null ? LedState.SHIFT_ON : ledOn;
      mModesLedOff[index] = ledOff == null ? LedState.SHIFT_OFF : ledOn;
   }

   void setMode(final int index, final Mode mode, final Runnable action)
   {
      setMode(index, mode, action, null, null);
   }

   void setMode(final int index, final Mode mode)
   {
      setMode(index, mode, null, null, null);
   }

   public void selectMinorMode(final int index)
   {
      if ((index < 0 && 8 < index) || index == mSelectedIndex || mModes[index] == null)
         return;

      if (mModes[mSelectedIndex] != null)
         mModes[mSelectedIndex].deactivate();

      mSelectedIndex = index;
      if (mModesAction[index] != null)
         mModesAction[index].run();
      mModes[index].activate();

      mDriver.updateKeyTranslationTable();
      mDriver.scheduleFlush();
   }

   @Override
   void updateKeyTranslationTable(final Integer[] table)
   {
      mModes[mSelectedIndex].updateKeyTranslationTable(table);
   }

   private int mSelectedIndex = 0;

   private final Mode[] mModes = {null, null, null, null, null, null, null, null};
   private final Runnable[] mModesAction = {null, null, null, null, null, null, null, null};
   private LedState[] mModesLedOn = {null, null, null, null, null, null, null, null};
   private LedState[] mModesLedOff = {null, null, null, null, null, null, null, null};
}
