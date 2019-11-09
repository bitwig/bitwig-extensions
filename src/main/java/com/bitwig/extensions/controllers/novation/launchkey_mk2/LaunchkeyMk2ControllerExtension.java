package com.bitwig.extensions.controllers.novation.launchkey_mk2;

import java.util.Arrays;

import com.bitwig.extensions.controllers.novation.common.DefaultPalette;
import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.UserControlBank;
import static com.bitwig.extension.controller.api.CursorDeviceFollowMode.FIRST_INSTRUMENT;

public class LaunchkeyMk2ControllerExtension extends ControllerExtension
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
   private boolean mIgnoreModeChanges = false;

   public LaunchkeyMk2ControllerExtension(
      final LaunchkeyMk2ControllerExtensionDefinition definition,
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

      final NoteInput noteInput = mMidiIn1.createNoteInput("Keys");
      mPadsInput = mMidiIn2.createNoteInput("Pads");
      mPadsInput.setShouldConsumeEvents(false);

      mTransport = mHost.createTransport();
      mCursorTrack = mHost.createCursorTrack(0, 0);
      mDeviceBank = mCursorTrack.createDeviceBank(8);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mDrumDevice = mCursorTrack.createCursorDevice("inst", "Drum device", 0, FIRST_INSTRUMENT);
      mCursorDevice.exists().markInterested();
      mDrumPadBank = mDrumDevice.createDrumPadBank(16);
      mDrumPadBank.scrollPosition().set(36);
      mCursorTrack.playingNotes().markInterested();
      mMasterTrack = mHost.createMasterTrack(2);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
      mRemoteControls.selectedPageIndex().markInterested();
      mRemoteControls.pageCount().markInterested();
      mDeviceEnvelopes = mCursorDevice.createCursorRemoteControlsPage("envelope", 9, "envelope");
      mDeviceEnvelopes.setHardwareLayout(HardwareControlType.SLIDER, 9);
      mTrackBank = mHost.createTrackBank(8, 0, 2);
      mUserControls = mHost.createUserControls(8);

      mDrumRemoteControls = mDrumDevice.createCursorRemoteControlsPage(8);
      mDrumRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);

      for (int i = 0; i < 8; i++)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.markInterested();
         parameter.exists().markInterested();

         final RemoteControl drumParameter = mDrumRemoteControls.getParameter(i);
         drumParameter.markInterested();

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
            slot.color().markInterested();
         }

         mUserControls.getControl(i).markInterested();

         mDeviceBank.itemCount().markInterested();
         final Device device = mDeviceBank.getDevice(i);
         device.exists().markInterested();
         mIsCursorDevice[i] = device.createEqualsValue(mCursorDevice);
         mIsCursorDevice[i].markInterested();
      }

      for (int i = 0; i < 16; i++)
      {
         mDrumPadBank.getItemAt(i).exists().markInterested();
         mDrumPadBank.getItemAt(i).color().markInterested();
      }

      mPopupBrowser = mHost.createPopupBrowser();
      mPopupBrowser.exists().markInterested();

      mMidiOut2.sendMidi(0x9F, 12, 127);  // set to Extended mode

      setMode(Mode.PLAY, false);

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

         final RemoteControl drumParameter = mDrumRemoteControls.getParameter(i);
         drumParameter.setIndication(mMode == Mode.DRUM);

         mUserControls.getControl(i).setIndication(mMode == Mode.LAUNCH);

         final Track track = mTrackBank.getItemAt(i);
         final ClipLauncherSlotBank clipLauncherSlotBank = track.clipLauncherSlotBank();
         clipLauncherSlotBank.setIndication(mMode == Mode.LAUNCH);
         track.volume().setIndication(mMode != Mode.PLAY);
      }

      for (int i = 0; i < 9; ++i)
      {
         mDeviceEnvelopes.getParameter(i).setIndication(mMode == Mode.PLAY);
      }

      mMasterTrack.volume().setIndication(mMode != Mode.PLAY);
   }

   private void setMode(final Mode mode, final boolean showNotification)
   {
      if (!mIgnoreModeChanges)
      {
         mIgnoreModeChanges = true;

         mMidiOut2.sendMidi(0x9F, 15, 127);
         mMidiOut2.sendMidi(0x9F, 14, 127);
         mMidiOut2.sendMidi(0x9F, 13, 127);

         mMode = mode;

         updateIndications();

         if (showNotification)
         {
            mHost.showPopupNotification(mMode.toString() + " mode");
         }

         mHost.scheduleTask(() ->
         {
            mIgnoreModeChanges = false;
            invalidateLeds();
         }, 150);

         updateDrumPads();
      }
   }

   private void updateDrumPads()
   {
      Integer[] table = new Integer[128];

      if (mMode == Mode.DRUM)
      {
         for(int k=0; k<128; k++)
         {
            table[k] = keyToPadIndex(k);
         }
      }
      else
      {
         Arrays.fill(table, Integer.valueOf(-1));
      }

      mPadsInput.setKeyTranslationTable(table);
   }

   private int keyToPadIndex(final int key)
   {
      if (key >= 112 && key < 116)
      {
         return 36 + key - 112;
      }
      else if (key >= 116 && key < 120)
      {
         return 44 + key - 116;
      }
      else if (key >= 96 && key < 100)
      {
         return 40 + key - 96;
      }
      else if (key >= 100 && key < 104)
      {
         return 48 + key - 100;
      }

      return -1;
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

      if (status == 191 && data1 == 114 && data2 == 127)
      {
         mTransport.stop();
      }
      else if (status == 191 && data1 == 115 && data2 == 127)
      {
         mTransport.play();
      }
      else if (status == 191 && data1 == 116 && data2 == 127)
      {
         mTransport.isArrangerLoopEnabled().toggle();
      }
      else if (status == 191 && data1 == 117 && data2 == 127)
      {
         mTransport.record();
      }
      else if (status == 191 && data1 == 102 && data2 == 127)
      {
         prevTrack();
      }
      else if (status == 191 && data1 == 103 && data2 == 127)
      {
         nextTrack();
      }
      else if (status == 191 && data1 == 112 && data2 == 127)
      {
         prevScene();
      }
      else if (status == 191 && data1 == 113 && data2 == 127)
      {
         nextScene();
      }
      else if (status == 159 && data1 == 104)
      {
         onRoundPad(0, data2);
      }
      else if (status == 159 && data1 == 120)
      {
         onRoundPad(1, data2);
      }
      else if (status == 191 && data1 >= 21 && data1 <= 28)
      {
         int index = data1 - 21;
         onKnob(index, data2);
      }
      else if (status == 191 && data1 >= 41 && data1 <= 48)
      {
         int index = data1 - 41;
         onSlider(index, data2);
      }
      else if (status == 191 && data1 >= 51 && data1 <= 59)
      {
         int index = data1 - 51;
         onSliderButton(index, data2);
      }
      else if (status == 191 && data1 == 7)
      {
         onSlider(8, data2);
      }
      else if (status == 159 && data1 == 16 && data2 == 0)
      {
         setMode(Mode.PLAY, true);
      }
      else if (status == 159 && data1 == 14 && data2 == 0)
      {
         setMode(Mode.LAUNCH, true);
      }
      else if (status == 159 && data1 == 15 && data2 == 0)
      {
         setMode(Mode.DRUM, true);
      }
      else if (status == 159 && data1 >= 96 && data1 < 104)
      {
         onSquarePad(data1 - 96, 0, data2);
      }
      else if (status == 159 && data1 >= 112 && data1 < 120)
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
      else
      {
         mTransport.rewind();
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
      else
      {
         mTransport.fastForward();
      }
   }

   private void onKnob(final int index, final int value)
   {
      if (mMode == Mode.PLAY)
      {
         mRemoteControls.getParameter(index).set(value, 128);
      }
      else if (mMode == Mode.DRUM)
      {
         mDrumRemoteControls.getParameter(index).set(value, 128);
      }
      else
      {
         mUserControls.getControl(index).set(value, 128);
      }
   }

   private void onSlider(final int index, final int value)
   {
      if (mMode == Mode.PLAY)
      {
         mDeviceEnvelopes.getParameter(index).set(value, 128);
      }
      else if (index == 8)
      {
         mMasterTrack.volume().set(value, 128);
      }
      else
      {
         mTrackBank.getItemAt(index).volume().set(value, 128);
      }
   }

   private void onSliderButton(final int index, final int value)
   {
      if (value == 127)
      {
         if (index == 8)
         {
            mSoloMode = !mSoloMode;

            mHost.showPopupNotification(mSoloMode ?
            "1-8 → SOLO" : "1-8 → MUTE");
         }
         else if (mSoloMode)
         {
            mTrackBank.getItemAt(index).solo().toggle(true);
         }
         else
         {
            mTrackBank.getItemAt(index).mute().toggle();
         }
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
      mMidiOut2.sendMidi(0x9F, 12, 0);
   }

   @Override
   public void flush()
   {
      final int offColor = 0;
      final int white = 3;
      final int grey = 117;
      final int device = white;
      final int deviceOff = grey;
      final int redLow = 7;
      final int red = 72;
      final int green = 21;
      final int greenLow = 23;

      final int[] knobColors = { 5, 9, 13, 17, 29, 41, 49, 57};
      final int[] knobColorsOff = { 7, 11, 15, 19, 31, 43, 51, 59};

      for (SimpleLed sceneLed : mSceneLeds)
      {
         sceneLed.setColor(offColor);
      }

      mSoloLed.setColor(mSoloMode ? 127 : 0);

      if (mMode == Mode.PLAY)
      {
         final int pages = mRemoteControls.pageCount().get();
         final int selectedPage = mRemoteControls.selectedPageIndex().get();

         for(int p=0; p<8; p++)
         {
            if (p == selectedPage)
            {
               mPadLeds[p].setColor(knobColors[p]);
            }
            else
            {
               mPadLeds[p].setColor(knobColorsOff[p]);
            }
         }

         final int devices = mDeviceBank.itemCount().get();

         for(int p=0; p<8; p++)
         {
            if (mIsCursorDevice[p].get() && mCursorDevice.exists().get())
            {
               mPadLeds[p + 8].setColor(device);
            }
            else
            {
               mPadLeds[p + 8].setColor(p < devices ? deviceOff : offColor);
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
               mPadLeds[p].setColor(red);
            }
            else if (slot.isPlaying().get())
            {
               mPadLeds[p].setColor(white);
            }
            else if (slot.hasContent().get())
            {
               final int colorIndex = DefaultPalette.getColorIndexClosestToColor(slot.color());
               mPadLeds[p].setColor(colorIndex);
            }
            else if (track.arm().get())
            {
               mPadLeds[p].setColor(redLow);
            }
            else
            {
               mPadLeds[p].setColor(offColor);
            }

            if (mBlink)
            {
               if (slot.isStopQueued().get())
               {
                  mPadLeds[p].setColor(track.arm().get() ? redLow : offColor);
               }
               if (slot.isPlaybackQueued().get())
               {
                  mPadLeds[p].setColor(slot.isPlaying().get() ? grey : white);
               }
               if (slot.isRecordingQueued().get())
               {
                  mPadLeds[p].setColor(slot.isRecording().get() ? redLow : red);
               }
            }
         }

         for(int s=0; s<2; s++)
         {
            final Scene scene = mTrackBank.sceneBank().getScene(s);
         }
      }
      else if (mMode == Mode.DRUM)
      {
         for(int p=0; p<16; p++)
         {
            int key = padToKey(p);
            int pad = key - 36;
            final boolean notePlaying = mCursorTrack.playingNotes().isNotePlaying(key);

            int padColor = offColor;

            final DrumPad drumPad = mDrumPadBank.getItemAt(pad);

            if (drumPad.exists().get())
            {
               padColor = DefaultPalette.getColorIndexClosestToColor(drumPad.color());
            }

            mPadLeds[p].setColor(notePlaying ? white : padColor);
         }
      }

      if (mPopupBrowser.exists().get())
      {
         mSceneLeds[0].setColor(red);
         mSceneLeds[1].setColor(green);
      }

      for (SimpleLed padLed : mPadLeds)
      {
         padLed.flush(mMidiOut2, 0);
      }

      for (SimpleLed sceneLed : mSceneLeds)
      {
         sceneLed.flush(mMidiOut2, 0);
      }

      mSoloLed.flush(mMidiOut2, 0);
   }

   private int padToKey(final int p)
   {
      if (p >= 0 && p < 4)
      {
         return 40 + p;
      }
      else if (p >= 4 && p < 8)
      {
         return 48 + p - 4;
      }
      else if (p >= 8 && p < 12)
      {
         return 36 + p - 8;
      }
      else if (p >= 12 && p < 16)
      {
         return 44 + p - 12;
      }

      return 0;
   }

   private void invalidateLeds()
   {
      for (SimpleLed padLed : mPadLeds)
      {
         padLed.invalidate();
      }

      for (SimpleLed sceneLed : mSceneLeds)
      {
         sceneLed.invalidate();
      }
   }

   private ControllerHost mHost;

   private MidiIn mMidiIn1;
   private MidiIn mMidiIn2;
   private MidiOut mMidiOut1;
   private MidiOut mMidiOut2;

   private SimpleLed[] mPadLeds = new SimpleLed[]
   {
      new SimpleLed(0x9F, 96),
      new SimpleLed(0x9F, 97),
      new SimpleLed(0x9F, 98),
      new SimpleLed(0x9F, 99),
      new SimpleLed(0x9F, 100),
      new SimpleLed(0x9F, 101),
      new SimpleLed(0x9F, 102),
      new SimpleLed(0x9F, 103),

      new SimpleLed(0x9F, 112),
      new SimpleLed(0x9F, 113),
      new SimpleLed(0x9F, 114),
      new SimpleLed(0x9F, 115),
      new SimpleLed(0x9F, 116),
      new SimpleLed(0x9F, 117),
      new SimpleLed(0x9F, 118),
      new SimpleLed(0x9F, 119),
   };

   private SimpleLed[] mSceneLeds = new SimpleLed[]
   {
      new SimpleLed(0x9F, 104),
      new SimpleLed(0x9F, 120),
   };

   private SimpleLed mSoloLed = new SimpleLed(191, 59);

   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private TrackBank mTrackBank;
   private DeviceBank mDeviceBank;
   private BooleanValue[] mIsCursorDevice = new BooleanValue[8];
   private UserControlBank mUserControls;
   private PopupBrowser mPopupBrowser;
   private CursorRemoteControlsPage mDeviceEnvelopes;
   private MasterTrack mMasterTrack;
   private Transport mTransport;
   private NoteInput mPadsInput;
   private DrumPadBank mDrumPadBank;
   private PinnableCursorDevice mDrumDevice;
   private CursorRemoteControlsPage mDrumRemoteControls;
   private boolean mSoloMode = false;
}
