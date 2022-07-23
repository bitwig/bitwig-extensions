package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.Button;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;

public class HwControls {
   private static final int KNOB_CC_BASE = 21;
   private static final int FADER_CC_BASE = 53;
   private static final int TRACK_BUTTON_CC_BASE = 37;
   private static final int MASTER_SLIDER_CC = 61;

   private static final int NAV_UP_CC = 106;
   private static final int NAV_DOWN_CC = 107;
   private static final int SCENE_LAUNCH_CC = 104;
   private static final int MODE_ROW2_CC = 105;
   private static final int TRACK_LEFT_CC = 103;
   private static final int TRACK_RIGHT_CC = 102;
   private static final int ARM_SELECT_CC = 45;
   private static final int DEVICE_LOCK_CC = 52;
   private static final int DEVICE_SELECT_CC = 51;
   private static final int BASE_CC_CHANNEL = 15;
   public static final int DRUM_PAD_BASE = 36;

   private final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[8];
   private final HardwareSlider[] sliders = new HardwareSlider[8];
   private final HardwareSlider masterSlider;
   private final RgbNoteButton[] sessionButtons = new RgbNoteButton[16];
   private final RgbNoteButton[] deviceButtons = new RgbNoteButton[16];
   private final RgbNoteButton[] drumButtons = new RgbNoteButton[16];
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
         sessionButtons[i] = new RgbNoteButton(driver, "SESSION_BUTTON", i, 0, (i < 8 ? 96 : 112 - 8) + i);
         deviceButtons[i] = new RgbNoteButton(driver, "DEVICE_BUTTON", i, 0, (i < 8 ? 64 : 80 - 8) + i);
         drumButtons[i] = new RgbNoteButton(driver, "DRUM_BUTTON", i, 9, DRUM_PAD_BASE + i);
      }
      for (int i = 0; i < 8; i++) {
         trackButtons[i] = new RgbCcButton(driver, "TRACK_BUTTON", i, BASE_CC_CHANNEL, TRACK_BUTTON_CC_BASE + i);
         knobs[i] = surface.createAbsoluteHardwareKnob("KNOB_" + (i + 1));
         knobs[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CC_CHANNEL, KNOB_CC_BASE + i));
         sliders[i] = surface.createHardwareSlider("SLIDER_" + (i + 1));
         sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CC_CHANNEL, FADER_CC_BASE + i));
      }

      masterSlider = surface.createHardwareSlider("MASTER_SLIDER");
      masterSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CC_CHANNEL, MASTER_SLIDER_CC));

      navUpButton = new RgbCcButton(driver, "NAV_UP", 0, BASE_CC_CHANNEL, NAV_UP_CC);
      navDownButton = new RgbCcButton(driver, "NAV_DOWN", 1, BASE_CC_CHANNEL, NAV_DOWN_CC);
      sceneLaunchButton = new RgbCcButton(driver, "SCENE_L", 2, 0, SCENE_LAUNCH_CC);
      modeRow2Button = new RgbCcButton(driver, "MROW2", 4, 0, MODE_ROW2_CC);
      trackLeftButton = new Button(driver, "TRACK_LEFT", TRACK_LEFT_CC, BASE_CC_CHANNEL);
      trackRightButton = new Button(driver, "TRACK_RIGHT", TRACK_RIGHT_CC, BASE_CC_CHANNEL);

      armSelectButton = new RgbCcButton(driver, "ARM_SELECT", 6, BASE_CC_CHANNEL, ARM_SELECT_CC);
      deviceLockButton = new RgbCcButton(driver, "DEV_LOCK", 5, BASE_CC_CHANNEL, DEVICE_LOCK_CC);
      deviceSelectButton = new Button(driver, "DEVICE_SELECT", DEVICE_SELECT_CC, BASE_CC_CHANNEL);
   }

   public RgbNoteButton[] getDrumButtons() {
      return drumButtons;
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

   public RgbNoteButton[] getDeviceButtons() {
      return deviceButtons;
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
