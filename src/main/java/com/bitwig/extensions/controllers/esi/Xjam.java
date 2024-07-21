package com.bitwig.extensions.controllers.esi;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;

public class Xjam extends ControllerExtension {

   protected Xjam(ControllerExtensionDefinition definition, ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      mHost = getHost();
      mHardwareSurface = mHost.createHardwareSurface();

      mMidiIn = mHost.getMidiInPort(0);

      mMidiOut = mHost.getMidiOutPort(0);

      mNoteInput = mMidiIn.createNoteInput("pad", "9?????", "D?????", "A0????");
      mNoteInput.setShouldConsumeEvents(false);

      mCursorTrack = mHost.createCursorTrack(0, 0);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(mRelativeKnobs.length);

      createRelativeEncoders();
      createAbsoluteEncoders();

      initRemoteControls();

      mMidiIn.setSysexCallback(this::sysexDataReceived);

      sendSysexCommand(0x7f); // Handshake to retrieve version number
   }

   private void initAfterVersionCheck()
   {
      mMidiOut.sendMidi(0xC0, 0x7f, 0x00);

      mHost.showPopupNotification("Xjam initialized!");
   }

   private void reportVersionCheckFail()
   {
      mHost.showPopupNotification("Xjam not initialized: please update the device firmware to 1.55 or newer.");
   }

   private void createRelativeEncoders()
   {
      final int[] controlNumbers = { 3, 9, 12, 13, 14, 15 };

      for (int i = 0; i < 6; i++)
      {
         int controlNumber = controlNumbers[i];
         final RelativeHardwareKnob knob = mHardwareSurface.createRelativeHardwareKnob("enc" + i);
         knob.setLabel(String.valueOf(i + 1));
         knob.setIndexInGroup(i);
         knob.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, controlNumber, 120));

         mRelativeKnobs[i] = knob;
      }
   }

   private void createAbsoluteEncoders()
   {
      createAbsoluteEncoders("Yellow", 6, 1, new int[]{ 1, 5, 7, 8, 70, 71 });
      createAbsoluteEncoders("Red", 12, 2, new int[]{ 72, 73, 74, 80, 81, 91 });
   }

   private void createAbsoluteEncoders(final String color, final int offset, final int channel, final int[] controlNumbers)
   {
      assert controlNumbers.length == 6;

      for (int i = 0; i < 6; i++)
      {
         final int index = i + offset;
         int controlNumber = controlNumbers[i];
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("enc" + index);
         knob.setLabel(color + " " + (i + 1));
         knob.setIndexInGroup(index);
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, controlNumber));
      }
   }

   public void initRemoteControls() {
      for (int i = 0; i < mRelativeKnobs.length; i++)
      {
         final RemoteControl parameter = mCursorRemoteControls.getParameter(i);
         parameter.setIndication(true);
         parameter.addBinding(mRelativeKnobs[i]);
      }
   }

   @Override
   public void exit() {
      if (mSceneIndexToSwitchBackTo >= 0)
         mMidiOut.sendMidi(0xC0, mSceneIndexToSwitchBackTo, 0x00);
   }

   @Override
   public void flush() {
      mHardwareSurface.updateHardware();
   }

   private void sendSysexCommand(final int command)
   {
      assert command < 128;

      final String sysex = String.format("F0 00 20 54 30 %02X F7", command);
      mMidiOut.sendSysex(sysex);
   }

   private void sysexDataReceived(final String data)
   {
      final String expectedPrefix = "f0002054";
      if (!data.startsWith(expectedPrefix))
         return;

      final String expectedSuffix = "f7";
      if (!data.endsWith(expectedSuffix))
         return;

      final String payload = data.substring(expectedPrefix.length(), data.length() - expectedSuffix.length());

      if (payload.startsWith("2901") && payload.length() == 8)
      {
         // Version number
         try
         {
            final int versionMajor = Integer.parseInt(payload.substring(4, 6), 16);
            final int versionMinor = Integer.parseInt(payload.substring(6, 8), 16);
            getHost().println("Version: " + versionMajor + "." + versionMinor);
            if (versionMajor > 1 || (versionMajor == 1 && versionMinor >= 85))
               initAfterVersionCheck();
            else
               reportVersionCheckFail();
         }
         catch (final NumberFormatException e)
         {
            reportVersionCheckFail();
         }

      }
      else if (payload.startsWith("300008") && payload.length() == 8)
      {
         // This message contains the current scene index. We request it when starting the extension and store it so
         // that we can switch back during exit.
         final int sceneIndexToSwitchBackTo = Integer.parseInt(payload.substring(6, 8), 16) - 1;
         if (0 <= sceneIndexToSwitchBackTo && sceneIndexToSwitchBackTo < 32)
         {
            mSceneIndexToSwitchBackTo = (byte) sceneIndexToSwitchBackTo;
            getHost().println("Scene index before: " + (mSceneIndexToSwitchBackTo + 1));
         }
         else
         {
            getHost().println("Warning: invalid scene index (" + sceneIndexToSwitchBackTo + ")");
         }
      }
   }

   // API Elements
   private HardwareSurface mHardwareSurface;
   private ControllerHost mHost;

   private MidiIn mMidiIn;

   private MidiOut mMidiOut;

   private NoteInput mNoteInput;

   private CursorTrack mCursorTrack;

   private CursorDevice mCursorDevice;

   private CursorRemoteControlsPage mCursorRemoteControls;

   // Hardware elements
   private RelativeHardwareKnob[] mRelativeKnobs = new RelativeHardwareKnob[6];

   private byte mSceneIndexToSwitchBackTo = -1;
}
