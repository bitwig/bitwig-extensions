package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.EnumDefinition;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteLatch;
import com.bitwig.extension.controller.api.SettableDoubleValue;
import com.bitwig.extension.controller.api.SettableEnumValue;

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
      bindToggle(driver.getPadButton(0, 6), noteLatch.mono());
      bindLightState(() -> noteLatch.mono().get() ? LedState.SHIFT_ON : LedState.SHIFT_OFF, driver.getPadButton(0, 6));

      // Arp enable
      bindToggle(driver.getPadButton(1, 7), arpeggiator.isEnabled());
      bindLightState(() -> arpeggiator.isEnabled().get() ? LedState.PLAY_MODE : LedState.PLAY_MODE_OFF, driver.getPadButton(1, 7));

      // Panic
      bindPressed(driver.getPadButton(1, 6), this::panic);
      bindLightState(LedState.REC_ON, driver.getPadButton(1, 6));

      // Arp Mode
      bindPressed(driver.getPadButton(2, 7), () -> setArpeggiatorMode(mArpModeIndex + 1));
      bindLightState(LedState.STEP_PLAY, driver.getPadButton(2, 7));
      bindPressed(driver.getPadButton(2, 6), () -> setArpeggiatorMode(mArpModeIndex - 1));
      bindLightState(LedState.STEP_PLAY, driver.getPadButton(2, 6));

      // Arp octave
      bindPressed(driver.getPadButton(3, 7), () -> setArpOctaves(mArpOctave + 1));
      bindLightState(LedState.PITCH, driver.getPadButton(3, 7));
      bindPressed(driver.getPadButton(3, 6), () -> setArpOctaves(mArpOctave - 1));
      bindLightState(LedState.PITCH, driver.getPadButton(3, 6));

      // Arp gate length
      bindPressed(driver.getPadButton(4, 7), () -> setArpGateLen(mArpGateLengthIndex + 1));
      bindLightState(LedState.VOLUME_MODE, driver.getPadButton(4, 7));
      bindPressed(driver.getPadButton(4, 6), () -> setArpGateLen(mArpGateLengthIndex - 1));
      bindLightState(LedState.VOLUME_MODE, driver.getPadButton(4, 6));

      // Arp speed
      bindPressed(driver.getPadButton(5, 7), () -> setArpRate(mArpRateIndex + 1));
      bindLightState(LedState.STEP_PLAY, driver.getPadButton(5, 7));
      bindPressed(driver.getPadButton(5, 6), () -> setArpRate(mArpRateIndex - 1));
      bindLightState(LedState.STEP_PLAY, driver.getPadButton(5, 6));

      bindLightState(LedState.OFF, driver.getPadButton(6, 7));
      bindLightState(LedState.OFF, driver.getPadButton(6, 6));
      bindLightState(LedState.OFF, driver.getPadButton(7, 7));
      bindLightState(LedState.OFF, driver.getPadButton(7, 6));
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
      arpeggiator.rate().subscribe();
      arpeggiator.gateLength().subscribe();

      setArpOctaves(mArpOctave);
      setArpeggiatorMode(mArpModeIndex);
      setArpGateLen(mArpGateLengthIndex);

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
      arpeggiator.rate().unsubscribe();
      arpeggiator.gateLength().unsubscribe();

      mDriver.updateKeyTranslationTable();
   }

   public void updateKeyTranslationTable(final Integer[] table)
   {
      for (int x = 0; x < 8; ++x)
         for (int y = 0; y < 2; ++y)
            table[10 * (y + 7) + x + 1] = -1;
   }

   private void setArpeggiatorMode(int modeIndex)
   {
      final SettableEnumValue mode = mDriver.getArpeggiator().mode();
      final EnumDefinition enumDefinition = mode.enumDefinition();

      final int entryCount = enumDefinition.entryCount();
      mArpModeIndex = (modeIndex + entryCount) % entryCount;

      final String arpMode = enumDefinition.entryValue(mArpModeIndex);
      mode.set(arpMode);
      mDriver.getHost().showPopupNotification("Arpeggiator Mode: " + arpMode);
   }

   private void setArpOctaves(int value)
   {
      mArpOctave = Math.min(Math.max(value, 0), 4);
      mDriver.getArpeggiator().octaves().set(mArpOctave);
      mDriver.getHost().showPopupNotification("Arpeggiator Octave: " + mArpOctave);
   }

   private void setArpGateLen(int index)
   {
      final SettableDoubleValue gateLength = mDriver.getArpeggiator().gateLength();
      mArpGateLengthIndex = Math.max(Math.min(index, GATE_LENGTHS.length - 1), 0);
      final double length = GATE_LENGTHS[mArpGateLengthIndex];
      gateLength.set(length);
      mDriver.getHost().showPopupNotification("Arpeggiator Gate Length: " + length);
   }

   private void setArpRate(int index)
   {
      final SettableDoubleValue rate = mDriver.getArpeggiator().rate();
      mArpRateIndex = Math.max(Math.min(index, ARP_RATE.length - 1), 0);
      final double value = ARP_RATE[mArpRateIndex];
      rate.set(value);
      mDriver.getHost().showPopupNotification("Arpeggiator Rate: " + value);
   }

   private static final double GATE_LENGTHS[] = new double[]{ 0.125, 0.25, 0.5, 0.75, 1 };
   private static final double ARP_RATE[] = new double[]{ 1./32., 1./16., 1./8., 1./4., 1./2., 1., 2, 4, 8 };

   int mArpOctave = 0;
   int mArpModeIndex = 1;
   int mArpGateLengthIndex = 2;
   int mArpRateIndex = 1;
}
