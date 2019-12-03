package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteLatch;
import com.bitwig.extension.controller.api.SettableIntegerValue;

public class NoteLatchAndArpeggiatorConfigLayer extends LaunchpadLayer
{
   public NoteLatchAndArpeggiatorConfigLayer(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver, name);

      final NoteInput noteInput = mDriver.getNoteInput();
      final NoteLatch noteLatch = noteInput.noteLatch();
      final Arpeggiator arpeggiator = noteInput.arpeggiator();

      // NoteLatch enable
      bindToggle(driver.getPadButton(0, 7), noteLatch.isEnabled());
      bindLightState(() -> noteLatch.isEnabled().get() ? LedState.PLAY_MODE : LedState.PLAY_MODE_OFF, driver.getPadButton(0, 7));

      // NoteLatch mono
      bindToggle(driver.getPadButton(1, 7), noteLatch.mono());
      bindLightState(() -> noteLatch.mono().get() ? LedState.SHIFT_ON : LedState.SHIFT_OFF, driver.getPadButton(1, 7));

      // Arp enable
      bindToggle(driver.getPadButton(2, 7), arpeggiator.isEnabled());
      bindLightState(() -> arpeggiator.isEnabled().get() ? LedState.PLAY_MODE : LedState.PLAY_MODE_OFF, driver.getPadButton(2, 7));

      // Arp octave
      bindPressed(driver.getPadButton(3, 7), this::selectNextArpOctave);
      bindLightState(LedState.PITCH, driver.getPadButton(3, 7));

      // Arp gate length
      bindPressed(driver.getPadButton(4, 7), this::selectNextArpGateLen);
      bindLightState(LedState.VOLUME_MODE, driver.getPadButton(4, 7));

      // Arp speed
      bindPressed(driver.getPadButton(5, 7), this::selectNextArpSpeed);
      bindLightState(LedState.STEP_PLAY, driver.getPadButton(5, 7));
      bindPressed(driver.getPadButton(6, 7), this::selectPreviousArpSpeed);
      bindLightState(LedState.STEP_PLAY, driver.getPadButton(6, 7));

      // Panic
      bindPressed(driver.getPadButton(7, 7), this::panic);
      bindLightState(LedState.REC_ON, driver.getPadButton(7, 7));
   }

   private void selectPreviousArpSpeed()
   {
   }

   private void selectNextArpSpeed()
   {

   }

   private void panic()
   {
      mDriver.getNoteLatch().releaseNotes();
      mDriver.getArpeggiator().releaseNotes();
   }

   @Override
   protected void onActivate()
   {
      final NoteInput noteInput = mDriver.getNoteInput();
      final NoteLatch noteLatch = noteInput.noteLatch();
      final Arpeggiator arpeggiator = noteInput.arpeggiator();

      noteLatch.subscribe();
      noteLatch.isEnabled().subscribe();
      noteLatch.mono().subscribe();

      arpeggiator.subscribe();
      arpeggiator.octaves().subscribe();
      arpeggiator.mode().subscribe();
      arpeggiator.isEnabled().subscribe();
      arpeggiator.period().subscribe();
      arpeggiator.gateLength().subscribe();

      arpeggiator.mode().set("flow");

      mDriver.updateKeyTranslationTable();
   }

   @Override
   protected void onDeactivate()
   {
      final NoteInput noteInput = mDriver.getNoteInput();
      final NoteLatch noteLatch = noteInput.noteLatch();
      final Arpeggiator arpeggiator = noteInput.arpeggiator();

      noteLatch.unsubscribe();
      noteLatch.isEnabled().unsubscribe();
      noteLatch.mono().unsubscribe();

      arpeggiator.unsubscribe();
      arpeggiator.octaves().unsubscribe();
      arpeggiator.mode().unsubscribe();
      arpeggiator.isEnabled().unsubscribe();
      arpeggiator.period().unsubscribe();
      arpeggiator.gateLength().unsubscribe();

      mDriver.updateKeyTranslationTable();
   }

   public void updateKeyTranslationTable(final Integer[] table)
   {
      for (int x = 0; x < 8; ++x)
         for (int y = 0; y < 1; ++y)
            table[10 * (y + 8) + x + 1] = -1;
   }

   private void selectNextArpGateLen()
   {
   }

   private void selectPreviousArpGateLen()
   {
   }

   private void selectNextArpeggiatorMode()
   {
      mDriver.getArpeggiator().mode().set("up");
   }

   private void selectPreviousArpeggiatorMode()
   {
      mDriver.getArpeggiator().mode().set("down");
   }

   private void selectNextArpOctave()
   {
      final SettableIntegerValue octaves = mDriver.getArpeggiator().octaves();
      octaves.set(Math.min(octaves.get() + 1, 4));
   }

   private void selectPreviousArpOctave()
   {
      final SettableIntegerValue octaves = mDriver.getArpeggiator().octaves();
      octaves.set(Math.max(octaves.get() - 1, 0));
   }
}
