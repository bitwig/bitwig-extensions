package com.bitwig.extensions.controllers.novation.launchkey_mini;

import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.UserControlBank;

public class LaunchkeyMiniControllerExtension extends ControllerExtension
{
   enum Mode
   {
      DRUM, PLAY, LAUNCH;

      public Mode next()
      {
         final Mode[] values = values();
         return values[(this.ordinal()+1) % values.length];
      }
   }

   Mode mMode;
   private boolean mBlink = true;

   public LaunchkeyMiniControllerExtension(
      final LaunchkeyMiniControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      mHost = getHost();

      mMidiIn1 = mHost.getMidiInPort(0);
      mMidiIn2 = mHost.getMidiInPort(1);
      mMidiOut1 = mHost.getMidiOutPort(0);
      mMidiOut2 = mHost.getMidiOutPort(1);

      mMidiIn1.setMidiCallback(this::onMidiIn1);
      mMidiIn2.setMidiCallback(this::onMidiIn2);

      final NoteInput noteInput = mMidiIn1.createNoteInput("");

      mCursorTrack = mHost.createCursorTrack(0, 0);
      mDeviceBank = mCursorTrack.createDeviceBank(8);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorDevice.exists().markInterested();

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
      mRemoteControls.selectedPageIndex().markInterested();
      mRemoteControls.pageCount().markInterested();
      mTrackBank = mHost.createTrackBank(8, 0, 2);
      mUserControls = mHost.createUserControls(8);

      for (int i = 0; i < 8; i++)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.markInterested();
         parameter.exists().markInterested();

         final Track track = mTrackBank.getItemAt(i);
         track.arm().markInterested();
         final ClipLauncherSlotBank clipLauncherSlotBank = track.clipLauncherSlotBank();
         for (int s = 0; s < 2; s++)
         {
            final ClipLauncherSlot slot = clipLauncherSlotBank.getItemAt(s);
            slot.isPlaying().markInterested();
            slot.hasContent().markInterested();
            slot.isRecording().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.isStopQueued().markInterested();
         }

         mUserControls.getControl(i).markInterested();

         mDeviceBank.itemCount().markInterested();
         final Device device = mDeviceBank.getDevice(i);
         device.exists().markInterested();
         mIsCursorDevice[i] = device.createEqualsValue(mCursorDevice);
         mIsCursorDevice[i].markInterested();
      }

      mPopupBrowser = mHost.createPopupBrowser();
      mPopupBrowser.exists().markInterested();

      setMode(Mode.PLAY);

      blinkTimer();
   }

   private void blinkTimer()
   {
      int BLINK_RATE = 160;

      mBlink = !mBlink;

      mHost.scheduleTask(this::blinkTimer, BLINK_RATE);
   }

   private void updateIndications()
   {
      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.setIndication(mMode == Mode.PLAY);

         mUserControls.getControl(i).setIndication(mMode != Mode.PLAY);

         final Track track = mTrackBank.getItemAt(i);
         final ClipLauncherSlotBank clipLauncherSlotBank = track.clipLauncherSlotBank();
         clipLauncherSlotBank.setIndication(mMode == Mode.LAUNCH);
      }
   }

   private void setMode(final Mode mode)
   {
      boolean inControl = mode != Mode.DRUM;

      if (inControl)
      {
         mMidiOut2.sendMidi(0x90, 12, 127);  // set to Extended mode
         mMidiOut2.sendMidi(0x90, 15, 127);  // turn incontrol on
      }

      mMode = mode;

      updateIndications();
   }

   private void nextMode()
   {
      setMode(mMode.next());

      mHost.showPopupNotification(mMode.toString() + " Mode");
   }

   private void onMidiIn1(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xF;
      final int msg = status >> 4;

      //mHost.println("MIDI IN1, msg: " + msg + " channel: " + channel + ", data1: " + data1 + ", data2: " + data2);
   }

   private void onMidiIn2(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xF;
      final int msg = status >> 4;

      if (status == 176 && data1 == 106 && data2 == 127)
      {
         prevTrack();
      }
      else if (status == 176 && data1 == 107 && data2 == 127)
      {
         nextTrack();
      }
      else if (status == 176 && data1 == 104 && data2 == 127)
      {
         prevScene();
      }
      else if (status == 176 && data1 == 105 && data2 == 127)
      {
         nextScene();
      }
      else if (status == 144 && data1 == 104)
      {
         onRoundPad(0, data2);
      }
      else if (status == 144 && data1 == 120)
      {
         onRoundPad(1, data2);
      }
      else if (status == 176 && data1 >= 21 && data1 <= 28)
      {
         int index = data1 - 21;
         onKnob(index, data2);
      }
      else if (status == 144 && data1 == 10)
      {
         nextMode();
      }
      else if (status == 144 && data1 >= 96 && data1 < 104)
      {
         onSquarePad(data1 - 96, 0, data2);
      }
      else if (status == 144 && data1 >= 112 && data1 < 120)
      {
         onSquarePad(data1 - 112, 1, data2);
      }
      //mHost.println("MIDI IN1, msg: " + msg + " channel: " + channel + ", data1: " + data1 + ", data2: " + data2);
   }

   private void prevScene()
   {
      if (mPopupBrowser.exists().get())
      {
         mPopupBrowser.selectPreviousFile();
      }
      else if (mMode == Mode.LAUNCH)
      {
         mTrackBank.sceneBank().scrollBackwards();
      }
   }

   private void nextScene()
   {
      if (mPopupBrowser.exists().get())
      {
         mPopupBrowser.selectNextFile();
      }
      else if (mMode == Mode.LAUNCH)
      {
         mTrackBank.sceneBank().scrollForwards();
      }
   }

   private void onKnob(final int index, final int value)
   {
      if (mMode == Mode.PLAY)
      {
         mRemoteControls.getParameter(index).set(value, 128);
      }
      else
      {
         mUserControls.getControl(index).set(value, 128);
      }
   }

   private void prevTrack()
   {
      if (mMode == Mode.LAUNCH)
      {
         mTrackBank.scrollPageBackwards();
      }
      else
      {
         mCursorTrack.selectPrevious();
      }
   }

   private void nextTrack()
   {
      if (mMode == Mode.LAUNCH)
      {
         mTrackBank.scrollPageForwards();
      }
      else
      {
         mCursorTrack.selectNext();
      }
   }

   private void onRoundPad(final int pad, final int value)
   {

      if (mPopupBrowser.exists().get() && value > 0)
      {
         if (pad == 0)
         {
            mPopupBrowser.cancel();
         }
         else
         {
            mPopupBrowser.commit();
         }
      }
      else if (mMode == Mode.LAUNCH && value > 0)
      {
         mTrackBank.sceneBank().getScene(pad).launch();
      }
      else if (mCursorDevice.exists().get())
      {
         mCursorDevice.replaceDeviceInsertionPoint().browse();
      }
      else
      {
         mCursorDevice.beforeDeviceInsertionPoint().browse();
      }
   }

   private void onSquarePad(final int column, final int row, final int value)
   {
      if (mMode == Mode.PLAY)
      {
         if (row == 0)
         {
            mRemoteControls.selectedPageIndex().set(column);
         }
         else if (row == 1)
         {
            final Device device = mDeviceBank.getDevice(column);

            if (device.exists().get())
            {
               mCursorDevice.selectDevice(device);
            }
            else
            {
               device.deviceChain().endOfDeviceChainInsertionPoint().browse();
            }
         }
      }
      else if (mMode == Mode.LAUNCH)
      {
         final ClipLauncherSlot slot = mTrackBank.getItemAt(column).clipLauncherSlotBank().getItemAt(row);
         slot.launch();
      }
   }

   @Override
   public void exit()
   {
      mMidiOut2.sendMidi(0x90, 12, 0);
   }

   @Override
   public void flush()
   {
      for (SimpleLed sceneLed : mSceneLeds)
      {
         sceneLed.setColor(SimpleLedColor.Off);
      }

      if (mMode == Mode.PLAY)
      {
         final int pages = mRemoteControls.pageCount().get();
         final int selectedPage = mRemoteControls.selectedPageIndex().get();

         for(int p=0; p<8; p++)
         {
            if (p == selectedPage)
            {
               mPadLeds[p].setColor(SimpleLedColor.Green);
            }
            else if (p < pages)
            {
               mPadLeds[p].setColor(SimpleLedColor.YellowLow);
            }
            else
            {
               mPadLeds[p].setColor(SimpleLedColor.Off);
            }
         }

         final int devices = mDeviceBank.itemCount().get();

         for(int p=0; p<8; p++)
         {
            if (mIsCursorDevice[p].get())
            {
               mPadLeds[p + 8].setColor(SimpleLedColor.Yellow);
            }
            else
            {
               mPadLeds[p + 8].setColor(p < devices ? SimpleLedColor.RedLow : SimpleLedColor.Off);
            }
         }
      }
      else if (mMode == Mode.LAUNCH)
      {
         for(int p=0; p<16; p++)
         {
            int column = p & 0x7;
            int row = p >> 3;
            final Track track = mTrackBank.getItemAt(column);
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(row);

            if (slot.isRecording().get())
            {
               mPadLeds[p].setColor(SimpleLedColor.Red);
            }
            else if (slot.isPlaying().get())
            {
               mPadLeds[p].setColor(SimpleLedColor.Green);
            }
            else if (slot.hasContent().get())
            {
               mPadLeds[p].setColor(SimpleLedColor.Yellow);
            }
            else if (track.arm().get())
            {
               mPadLeds[p].setColor(SimpleLedColor.RedLow);
            }
            else
            {
               mPadLeds[p].setColor(SimpleLedColor.Off);
            }

            if (mBlink)
            {
               if (slot.isStopQueued().get())
               {
                  mPadLeds[p].setColor(track.arm().get() ? SimpleLedColor.RedLow : SimpleLedColor.Off);
               }
               if (slot.isPlaybackQueued().get())
               {
                  mPadLeds[p].setColor(slot.isPlaying().get() ? SimpleLedColor.GreenLow : SimpleLedColor.Green);
               }
               if (slot.isRecordingQueued().get())
               {
                  mPadLeds[p].setColor(slot.isRecording().get() ? SimpleLedColor.RedLow : SimpleLedColor.Red);
               }
            }
         }

         for(int s=0; s<2; s++)
         {
            final Scene scene = mTrackBank.sceneBank().getScene(s);
         }
      }

      if (mPopupBrowser.exists().get())
      {
         mSceneLeds[0].setColor(SimpleLedColor.Red);
         mSceneLeds[1].setColor(SimpleLedColor.Green);
      }

      for (SimpleLed padLed : mPadLeds)
      {
         padLed.flush(mMidiOut2, 0);
      }

      for (SimpleLed sceneLed : mSceneLeds)
      {
         sceneLed.flush(mMidiOut2, 0);
      }
   }

   private ControllerHost mHost;

   private MidiIn mMidiIn1;
   private MidiIn mMidiIn2;
   private MidiOut mMidiOut1;
   private MidiOut mMidiOut2;

   private SimpleLed[] mPadLeds = new SimpleLed[]
   {
      new SimpleLed(0x90, 96),
      new SimpleLed(0x90, 97),
      new SimpleLed(0x90, 98),
      new SimpleLed(0x90, 99),
      new SimpleLed(0x90, 100),
      new SimpleLed(0x90, 101),
      new SimpleLed(0x90, 102),
      new SimpleLed(0x90, 103),

      new SimpleLed(0x90, 112),
      new SimpleLed(0x90, 113),
      new SimpleLed(0x90, 114),
      new SimpleLed(0x90, 115),
      new SimpleLed(0x90, 116),
      new SimpleLed(0x90, 117),
      new SimpleLed(0x90, 118),
      new SimpleLed(0x90, 119),
   };

   private SimpleLed[] mSceneLeds = new SimpleLed[]
   {
      new SimpleLed(0x90, 104),
      new SimpleLed(0x90, 120),
   };

   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private TrackBank mTrackBank;
   private DeviceBank mDeviceBank;
   private BooleanValue[] mIsCursorDevice = new BooleanValue[8];
   private UserControlBank mUserControls;
   private PopupBrowser mPopupBrowser;
}
