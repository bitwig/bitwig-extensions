package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class LaunchControlXlControllerExtension extends ControllerExtension
{
   enum Mode
   {
      Send2Device1(8),
      Send2Pan1(9),
      Send3(10),
      Send1Device2(11),
      ChannelDevice(12),
      None(0);

      Mode(int channel)
      {
         mChannel = channel;
      }

      int getChannel()
      {
         return mChannel;
      }

      private final int mChannel;
   }

   enum TrackControl
   {
      Mute,
      Solo,
      RecordArm
   }

   public LaunchControlXlControllerExtension(
      final LaunchControlXlControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      mHost = getHost();

      mMidiIn = mHost.getMidiInPort(0);
      mMidiOut = mHost.getMidiOutPort(0);

      mMidiIn.setMidiCallback(this::onMidiIn);
      mMidiIn.setSysexCallback(this::onSysex);

      // Load the template Factory/1
      mMidiOut.sendSysex("f000202902117708f7");

      mCursorTrack = mHost.createCursorTrack("cursor-track", "Launch Control XL Track Cursor", 0, 0, false);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
      mRemoteControls.selectedPageIndex().markInterested();
      mRemoteControls.pageCount().markInterested();

      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.markInterested();
         parameter.exists().markInterested();
      }

      mTrackBank = mHost.createMainTrackBank(8, 3, 0);
      mTrackBank.followCursorTrack(mCursorTrack);
      mTrackBank.canScrollBackwards().markInterested();
      mTrackBank.canScrollForwards().markInterested();

      mTrackBank.cursorIndex().markInterested();
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         track.solo().markInterested();
         track.arm().markInterested();
         track.mute().markInterested();
         track.volume().markInterested();
         track.volume().setIndication(true);
         track.exists().markInterested();

         final SendBank sendBank = track.sendBank();
         sendBank.canScrollBackwards().markInterested();
         sendBank.canScrollForwards().markInterested();
         for (int j = 0; j < 3; ++j)
         {
            sendBank.getItemAt(j).exists().markInterested();
         }

         mTrackDeviceCursors[i] = track.createCursorDevice();
         mTrackRemoteControls[i] = mTrackDeviceCursors[i].createCursorRemoteControlsPage(3);
         mTrackRemoteControls[i].setHardwareLayout(HardwareControlType.KNOB, 1);

         for (int j = 0; j < 3; ++j)
         {
            final RemoteControl parameter = mTrackRemoteControls[i].getParameter(j);
            parameter.markInterested();
            parameter.exists().markInterested();
         }
      }
   }

   private void updateIndications(final int numSends, final boolean hasRemoteControl, final boolean hasPan, final int numTrackRemoteControls)
   {
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         sendBank.setSizeOfBank(3);
         for (int j = 0; j < 3; ++j)
            sendBank.getItemAt(j).setIndication(j < numSends);
         sendBank.setSizeOfBank(numSends);
         mRemoteControls.getParameter(i).setIndication(hasRemoteControl);
         track.pan().setIndication(hasPan);

         for (int j = 0; j < 3; ++j)
            mTrackRemoteControls[i].getParameter(j).setIndication(j < numTrackRemoteControls);
      }
   }

   private void onSysex(final String sysex)
   {
      mHost.println("Sysex IN1: " + sysex);

      if (sysex.equals("f000202902117708f7"))
      {
         mHost.showPopupNotification("Switched to 2 Sends and DEVICE Mode");
         mMode = Mode.Send2Device1;
         updateIndications(2, true, false, 0);
      }
      else if (sysex.equals("f000202902117709f7"))
      {
         mHost.showPopupNotification("Switched to 2 Sends and Pan Mode");
         mMode = Mode.Send2Pan1;
         updateIndications(2, false, true, 0);
      }
      else if (sysex.equals("f00020290211770af7"))
      {
         mHost.showPopupNotification("Switched to 3 Sends Mode");
         mMode = Mode.Send3;
         updateIndications(3, false, false, 0);
      }
      else if (sysex.equals("f00020290211770bf7"))
      {
         mHost.showPopupNotification("Switched to 1 Send and 2 Channel DEVICE Controls Mode");
         mMode = Mode.Send1Device2;
         updateIndications(1, false, false, 2);
      }
      else if (sysex.equals("f00020290211770cf7"))
      {
         mHost.showPopupNotification("Switched to Channel DEVICE Controls Mode");
         mMode = Mode.ChannelDevice;
         updateIndications(0, false, false, 3);
      }
      else
      {
         mHost.showPopupNotification("Unsupported Template. We provide Modes for the Factory Template 1 to 5.");
         mMode = Mode.None;
      }
   }

   private void onMidiIn(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xF;
      final int msg = status >> 4;

      mHost.println("MIDI IN1, msg: " + msg + " channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      switch (msg)
      {
         case 11: // CC
            if (13 <= data1 && data1 <= 20)
               onKnob(channel, data1 - 13, 0, data2);
            else if (29 <= data1 && data1 <= 36)
               onKnob(channel, data1 - 29, 1, data2);
            else if (49 <= data1 && data1 <= 56)
               onKnob(channel, data1 - 49, 2, data2);
            else if (77 <= data1 && data1 <= 84)
               onFader(channel, data1 - 77, data2);
            else if (data1 == 104 && data2 == 127)
            {
               for (int i = 0; i < 8; ++i)
                  mTrackBank.getItemAt(i).sendBank().scrollBackwards();
            }
            else if (data1 == 105 && data2 == 127)
            {
               for (int i = 0; i < 8; ++i)
                  mTrackBank.getItemAt(i).sendBank().scrollForwards();
            }
            else if (data1 == 106 && data2 == 127)
               mTrackBank.scrollBackwards();
            else if (data1 == 107 && data2 == 127)
               mTrackBank.scrollForwards();
            break;

         case 9: // NOTE ON
            if (41 <= data1 && data1 <= 44)
               onButton(data1 - 41, 0);
            else if (57 <= data1 && data1 <= 60)
               onButton(4 + data1 - 57, 0);
            else if (73 <= data1 && data1 <= 76)
               onButton(data1 - 73, 1);
            else if (89 <= data1 && data1 <= 92)
               onButton(4 + data1 - 89, 1);
            else if (data1 == 105)
               mIsDeviceOn = true;
            else if (data1 == 106)
               mTrackControl = TrackControl.Mute;
            else if (data1 == 107)
               mTrackControl = TrackControl.Solo;
            else if (data1 == 108)
               mTrackControl = TrackControl.RecordArm;
            break;

         case 8: // NOTE OFF
            if (data1 == 105)
               mIsDeviceOn = false;
            break;
      }
   }

   private void onButton(final int column, final int row)
   {
      if (row == 0)
         selectChannel(column);
      else
      {
         if (mIsDeviceOn)
            selectRemoteControlPage(column);
         else
            trackControl(column);
      }
   }

   private void selectRemoteControlPage(final int column)
   {
      mRemoteControls.selectedPageIndex().set(column);
   }

   private void trackControl(final int column)
   {
      switch (mTrackControl)
      {
         case Mute:
            mTrackBank.getItemAt(column).mute().toggle();
            break;

         case Solo:
            mTrackBank.getItemAt(column).solo().toggle();
            break;

         case RecordArm:
            mTrackBank.getItemAt(column).arm().toggle();
            break;
      }
   }

   private void selectChannel(final int column)
   {
      mCursorTrack.selectChannel(mTrackBank.getItemAt(column));
   }

   private void onFader(final int channel, final int column, final int value)
   {
      setTrackVolume(column, value);
   }

   private void setTrackVolume(final int column, final int value)
   {
      mTrackBank.getItemAt(column).volume().set(value, 128);
   }

   private void onKnob(final int channel, final int column, final int row, final int value)
   {
      assert 0 <= row;
      assert row < 3;

      assert 0 <= column;
      assert column < 8;

      assert 0 <= value;
      assert value < 128;

      switch (channel)
      {
         case 8:
            if (row == 0 || row == 1)
               setSend(column, row, value);
            else if (row == 2)
            {
               mRemoteControls.getParameter(column).set(value, 128);
            }
            break;

         case 9:
            if (row == 0 || row == 1)
               setSend(column, row, value);
            if (row == 2)
               mTrackBank.getItemAt(column).pan().set(value, 128);
            break;

         case 10:
            setSend(column, row, value);
            break;

         case 11:
            if (row == 0)
               setSend(column, row, value);
            else
               mTrackRemoteControls[column].getParameter(row - 1).set(value, 128);
            break;

         case 12:
            mTrackRemoteControls[column].getParameter(row).set(value, 128);
            break;
      }
   }

   private void setSend(final int column, final int row, final int value)
   {
      mTrackBank.getItemAt(column).sendBank().getItemAt(row).set(value, 128);
   }

   @Override
   public void exit()
   {

   }

   @Override
   public void flush()
   {
      paintRightButtons();
      paintKnobs();
      paintBottomButtons();

      final StringBuilder sb = new StringBuilder();

      mDeviceLed.flush(sb);
      mMuteLed.flush(sb);
      mSoloLed.flush(sb);
      mRecordArmLed.flush(sb);
      mUpButtonLed.flush(sb);
      mDownButtonLed.flush(sb);
      mLeftButtonLed.flush(sb);
      mRightButtonLed.flush(sb);

      for (SimpleLed simpleLed : mKnobsLed)
         simpleLed.flush(sb);

      for (SimpleLed simpleLed : mBottomButtonsLed)
         simpleLed.flush(sb);

      if (!sb.toString().isEmpty())
      {
         final String sysex = "F0 00 20 29 02 11 78 0" + Integer.toHexString(mMode.getChannel()) + sb + " F7";
         mMidiOut.sendSysex(sysex);
      }
   }

   protected void paintBottomButtons()
   {
      final int selectedTrack = mTrackBank.cursorIndex().get();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final boolean trackExists = track.exists().get();

         if (trackExists)
            mBottomButtonsLed[i].setColor(selectedTrack == i ? SimpleLedColor.Amber.value() : SimpleLedColor.AmberLow.value());
         else
            mBottomButtonsLed[i].setColor(SimpleLedColor.Off.value());

         if (mIsDeviceOn)
         {
            SimpleLedColor color = SimpleLedColor.Off;

            if (mRemoteControls.selectedPageIndex().get() == i)
               color = SimpleLedColor.Amber;
            else if (i < mRemoteControls.pageCount().get())
               color = SimpleLedColor.AmberLow;

            mBottomButtonsLed[8 + i].setColor(color.value());
         }
         else if (trackExists)
         {
            switch (mTrackControl)
            {
               case Mute:
                  mBottomButtonsLed[8 + i].setColor(track.mute().get() ? SimpleLedColor.Green.value() : SimpleLedColor.GreenLow.value());
                  break;

               case Solo:
                  mBottomButtonsLed[8 + i].setColor(track.solo().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.AmberLow.value());
                  break;

               case RecordArm:
                  mBottomButtonsLed[8 + i].setColor(track.arm().get() ? SimpleLedColor.Red.value() : SimpleLedColor.RedLow.value());
                  break;
            }
         }
         else
         {
            mBottomButtonsLed[8 + i].setColor(SimpleLedColor.Off.value());
         }
      }
   }

   protected void paintKnobs()
   {
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();

         switch (mMode)
         {
            case Send2Device1:
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[16 + i].setColor(mRemoteControls.getParameter(i).exists().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.Off.value());
               break;

            case Send2Pan1:
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[16 + i].setColor(track.exists().get() ? SimpleLedColor.Red.value() : SimpleLedColor.Off.value());
               break;

            case Send3:
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[16 + i].setColor(sendBank.getItemAt(2).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               break;

            case Send1Device2:
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? SimpleLedColor.Green.value() : SimpleLedColor.Off.value());
               mKnobsLed[8 + i].setColor(mTrackRemoteControls[i].getParameter(0).exists().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.Off.value());
               mKnobsLed[16 + i].setColor(mTrackRemoteControls[i].getParameter(1).exists().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.Off.value());
               break;

            case ChannelDevice:
               mKnobsLed[i].setColor(mTrackRemoteControls[i].getParameter(0).exists().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.Off.value());
               mKnobsLed[8 + i].setColor(mTrackRemoteControls[i].getParameter(1).exists().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.Off.value());
               mKnobsLed[16 + i].setColor(mTrackRemoteControls[i].getParameter(2).exists().get() ? SimpleLedColor.Amber.value() : SimpleLedColor.Off.value());
               break;
         }
      }
   }

   protected void paintRightButtons()
   {
      mDeviceLed.setColor(mIsDeviceOn ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());
      mMuteLed.setColor(mTrackControl == TrackControl.Mute ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());
      mSoloLed.setColor(mTrackControl == TrackControl.Solo ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());
      mRecordArmLed.setColor(mTrackControl == TrackControl.RecordArm ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());


      final SendBank sendBank = mTrackBank.getItemAt(0).sendBank();
      mUpButtonLed.setColor(sendBank.canScrollBackwards().get() ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());
      mDownButtonLed.setColor(sendBank.canScrollForwards().get() ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());

      mLeftButtonLed.setColor(mTrackBank.canScrollBackwards().get() ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());
      mRightButtonLed.setColor(mTrackBank.canScrollForwards().get() ? SimpleLedColor.Yellow.value() : SimpleLedColor.Off.value());
   }

   private ControllerHost mHost;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private TrackBank mTrackBank;
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private boolean mIsDeviceOn = false;
   private TrackControl mTrackControl = TrackControl.Mute;
   private Mode mMode = Mode.Send2Device1;

   private SimpleLed[] mKnobsLed = new SimpleLed[] {
      new SimpleLed(0x90, 0),
      new SimpleLed(0x90, 1),
      new SimpleLed(0x90, 2),
      new SimpleLed(0x90, 3),
      new SimpleLed(0x90, 4),
      new SimpleLed(0x90, 5),
      new SimpleLed(0x90, 6),
      new SimpleLed(0x90, 7),

      new SimpleLed(0x90, 8),
      new SimpleLed(0x90, 9),
      new SimpleLed(0x90, 10),
      new SimpleLed(0x90, 11),
      new SimpleLed(0x90, 12),
      new SimpleLed(0x90, 13),
      new SimpleLed(0x90, 14),
      new SimpleLed(0x90, 15),

      new SimpleLed(0x90, 16),
      new SimpleLed(0x90, 17),
      new SimpleLed(0x90, 18),
      new SimpleLed(0x90, 19),
      new SimpleLed(0x90, 20),
      new SimpleLed(0x90, 21),
      new SimpleLed(0x90, 22),
      new SimpleLed(0x90, 23),
   };

   private SimpleLed[] mBottomButtonsLed = new SimpleLed[] {
      new SimpleLed(0x90, 24),
      new SimpleLed(0x90, 25),
      new SimpleLed(0x90, 26),
      new SimpleLed(0x90, 27),
      new SimpleLed(0x90, 28),
      new SimpleLed(0x90, 29),
      new SimpleLed(0x90, 30),
      new SimpleLed(0x90, 31),

      new SimpleLed(0x90, 32),
      new SimpleLed(0x90, 33),
      new SimpleLed(0x90, 34),
      new SimpleLed(0x90, 35),
      new SimpleLed(0x90, 36),
      new SimpleLed(0x90, 37),
      new SimpleLed(0x90, 38),
      new SimpleLed(0x90, 39),
   };

   private SimpleLed mDeviceLed = new SimpleLed(0x90, 40);
   private SimpleLed mMuteLed = new SimpleLed(0x90, 41);
   private SimpleLed mSoloLed = new SimpleLed(0x90, 42);
   private SimpleLed mRecordArmLed = new SimpleLed(0x90, 43);
   private SimpleLed mUpButtonLed = new SimpleLed(0x90, 44);
   private SimpleLed mDownButtonLed = new SimpleLed(0x90, 45);
   private SimpleLed mLeftButtonLed = new SimpleLed(0x90, 46);
   private SimpleLed mRightButtonLed = new SimpleLed(0x90, 47);
   private CursorDevice[] mTrackDeviceCursors = new CursorDevice[8];
   private CursorRemoteControlsPage[] mTrackRemoteControls = new CursorRemoteControlsPage[8];
}
