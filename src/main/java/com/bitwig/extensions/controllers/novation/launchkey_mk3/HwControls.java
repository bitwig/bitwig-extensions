package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.Button;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;

public class HwControls {
   private final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[8];
   private final HardwareSlider[] sliders = new HardwareSlider[8];
   private final HardwareSlider masterSlider;
   private final RgbNoteButton[] sessionButtons = new RgbNoteButton[16];
   private final RgbCcButton[] trackButtons = new RgbCcButton[8];
   private final RgbCcButton navUpButton;
   private final RgbCcButton navDownButton;
   private final RgbCcButton sceneLaunchButton;
   private final RgbCcButton modeRow2Button;
   private final RgbCcButton deviceLockButton;
   private final Button trackLeftButton;
   private final Button trackRightButton;
   private final Button deviceSelectButton;
   private final RgbCcButton armSelectButton;

   public HwControls(final LaunchkeyMk3Extension driver) {
      final HardwareSurface surface = driver.getSurface();
      final MidiIn midiIn = driver.getMidiIn();
      for (int i = 0; i < 16; i++) {
         final int note = i < 8 ? 96 + i : 104 + i;
         sessionButtons[i] = new RgbNoteButton(driver, "SESSION_BUTTON", i, note);
      }
      for (int i = 0; i < 8; i++) {
         trackButtons[i] = new RgbCcButton(driver, "TRACK_BUTTON", i, 37 + i, 15);
         knobs[i] = surface.createAbsoluteHardwareKnob("KNOB_" + (i + 1));
         knobs[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 21 + i));
         sliders[i] = surface.createHardwareSlider("SLIDER_" + (i + 1));
         sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 53 + i));
      }

      masterSlider = surface.createHardwareSlider("MASTER_SLIDER");
      masterSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 61));

      navUpButton = new RgbCcButton(driver, "NAV_UP", 0, 106, 15);
      navDownButton = new RgbCcButton(driver, "NAV_DOWN", 1, 107, 15);
      sceneLaunchButton = new RgbCcButton(driver, "SCENE_L", 2, 104, 0);
      modeRow2Button = new RgbCcButton(driver, "MROW2", 4, 105, 0);
      trackLeftButton = new Button(driver, "TRACK_LEFT", 103, 15);
      trackRightButton = new Button(driver, "TRACK_RIGHT", 102, 15);

      armSelectButton = new RgbCcButton(driver, "ARM_SELECT", 6, 45, 15);
      deviceLockButton = new RgbCcButton(driver, "DEV_LOCK", 5, 52, 15);
      deviceSelectButton = new Button(driver, "DEVICE_SELECT", 51, 15);
   }

   public HardwareSlider getMasterSlider() {
      return masterSlider;
   }

   public HardwareSlider[] getSliders() {
      return sliders;
   }

   public AbsoluteHardwareKnob[] getKnobs() {
      return knobs;
   }

   public RgbCcButton[] getTrackButtons() {
      return trackButtons;
   }

   public RgbNoteButton[] getSessionButtons() {
      return sessionButtons;
   }

   public RgbCcButton getNavUpButton() {
      return navUpButton;
   }

   public RgbCcButton getNavDownButton() {
      return navDownButton;
   }

   public RgbCcButton getSceneLaunchButton() {
      return sceneLaunchButton;
   }

   public RgbCcButton getDeviceLockButton() {
      return deviceLockButton;
   }

   public Button getTrackLeftButton() {
      return trackLeftButton;
   }

   public Button getTrackRightButton() {
      return trackRightButton;
   }

   public Button getDeviceSelectButton() {
      return deviceSelectButton;
   }

   public RgbCcButton getArmSelectButton() {
      return armSelectButton;
   }

   public RgbCcButton getModeRow2Button() {
      return modeRow2Button;
   }
}
