package com.bitwig.extensions.controllers.novation.launchkey_mk3;

public class LaunchkeyConstants {

   public static final String DEVICE_INQUIRY = "f07e7f0601f7";
   public static final String SYS_EX_DEVICE_INQUIRY_HEAD = "f07e000602002029";
   public static final int NOTE_ON = 0x90;
   public static final int NOTE_OFF = 0x80;
   public static final int DRUM_CHANNEL = 0x9;

   public static final int BUTTON_CC_PADS = 3;
   public static final int BUTTON_CC_FADER = 10;
   public static final int BUTTON_CC_KNOB = 9;

   public static final int KNOB_PARAM_OFFSET = 56;
   public static final int FADER_PARAM_OFFSET = 80;

   private LaunchkeyConstants() {

   }

   public static String getDeviceId(final String sysEx) {
      if (sysEx.startsWith(SYS_EX_DEVICE_INQUIRY_HEAD)) {
         final int start = SYS_EX_DEVICE_INQUIRY_HEAD.length();
         return sysEx.substring(start, start + 2);
      }
      return "??";
   }

   public static String getAppId(final String sysEx) {
      if (sysEx.startsWith(SYS_EX_DEVICE_INQUIRY_HEAD)) {
         final int start = SYS_EX_DEVICE_INQUIRY_HEAD.length() + 8;
         return sysEx.substring(start, start + 8);
      }
      return "??";
   }

}
