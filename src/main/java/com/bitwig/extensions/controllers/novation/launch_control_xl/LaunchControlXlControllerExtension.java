package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
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
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class LaunchControlXlControllerExtension extends ControllerExtension
{
   // Identify possible modes
   enum Mode
   {
      Send2FullDevice(8, "Switched to 2 Sends and Selected DEVICE Controls Mode"),
      Send2Device1(9, "Switched to 2 Sends and 1 per Channel DEVICE Control Mode"),
      Send2Pan1(10, "Switched to 2 Sends and Pan Mode"),
      Send3(11, "Switched to 3 Sends Mode"),
      Send1Device2(12, "Switched to 1 Send and 2 per Channel DEVICE Controls Mode"),
      Device3(13, "Switched to per Channel DEVICE Controls Mode"),
      Track3(13, "Switched to per Channel TRACK Controls Mode"),
      None(0, "Unsupported Template. We provide Modes for the Factory Template 1 to 7.");

      Mode(final int channel, final String notification)
      {
         mChannel = channel;
         mNotification = notification;
      }

      int getChannel()
      {
         return mChannel;
      }

      public String getNotification()
      {
         return mNotification;
      }

      private final int mChannel;
      private final String mNotification;
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

      mMidiIn.setSysexCallback(this::onSysex);

      // Load the template Factory/1
      mMidiOut.sendSysex("f000202902117708f7");

      mCursorTrack = mHost.createCursorTrack("cursor-track", "Launch Control XL Track Cursor", 0, 0, true);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorDevice.hasNext().markInterested();
      mCursorDevice.hasPrevious().markInterested();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
      mRemoteControls.selectedPageIndex().markInterested();
      mRemoteControls.pageCount().markInterested();

      for (int i = 0; i < 8; ++i)
         markParameterInterested(mRemoteControls.getParameter(i));

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
         track.exists().markInterested();

         final SendBank sendBank = track.sendBank();
         sendBank.canScrollBackwards().markInterested();
         sendBank.canScrollForwards().markInterested();
         for (int j = 0; j < 3; ++j)
         {
            sendBank.getItemAt(j).exists().markInterested();
         }

         mTrackDeviceCursors[i] = track.createCursorDevice();
         mTrackCursorDeviceRemoteControls[i] = mTrackDeviceCursors[i].createCursorRemoteControlsPage(3);
         mTrackCursorDeviceRemoteControls[i].setHardwareLayout(HardwareControlType.KNOB, 1);

         mTrackRemoteControls[i] = track.createCursorRemoteControlsPage(3);

         for (int j = 0; j < 3; ++j)
         {
            markParameterInterested(mTrackCursorDeviceRemoteControls[i].getParameter(j));
            markParameterInterested(mTrackRemoteControls[i].getParameter(j));
         }
      }

      createHardwareSurface();
      createLayers();

      mMainLayer.activate();
      selectMode(Mode.Send2FullDevice);
      setTrackControl(TrackControl.Mute);
      setDeviceOn(false);
   }

   private static void markParameterInterested(final RemoteControl parameter)
   {
      parameter.markInterested();
      parameter.exists().markInterested();
   }

   private void createHardwareSurface()
   {
      mHardwareSurface = mHost.createHardwareSurface();
      final int[] knobOffsets = { 13, 29, 49 };
      for (int i = 0; i < 8; ++i)
      {
         for (int j = 0; j < 3; ++j)
         {
            final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("knob-" + i + "-" + j);
            knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(knobOffsets[j] + i));
            knob.setIndexInGroup(i);
            mHardwareKnobs[8 * j + i] = knob;
         }
         mHardwareSliders[i] = mHardwareSurface.createHardwareSlider("slider-" + i);
         mHardwareSliders[i].setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(77 + i));
         mHardwareSliders[i].setIndexInGroup(i);
      }

      mBtSendUp = mHardwareSurface.createHardwareButton("bt-send-up");
      mBtSendUp.pressedAction().setActionMatcher(mMidiIn.createActionMatcher(
         "(status & 0xF0) == 0xB0 && data1 == 104 && data2 == 127"));

      mBtSendDown = mHardwareSurface.createHardwareButton("bt-send-down");
      mBtSendDown.pressedAction().setActionMatcher(mMidiIn.createActionMatcher(
         "(status & 0xF0) == 0xB0 && data1 == 105 && data2 == 127"));

      mBtTrackLeft = mHardwareSurface.createHardwareButton("bt-track-left");
      mBtTrackLeft.pressedAction().setActionMatcher(mMidiIn.createActionMatcher(
         "(status & 0xF0) == 0xB0 && data1 == 106 && data2 == 127"));

      mBtTrackRight = mHardwareSurface.createHardwareButton("bt-track-right");
      mBtTrackRight.pressedAction().setActionMatcher(mMidiIn.createActionMatcher(
         "(status & 0xF0) == 0xB0 && data1 == 107 && data2 == 127"));

      mBtTrackFocus[0] = createHardwareButtonWithNote("bt-track-focus-0", 41, 0);
      mBtTrackFocus[1] = createHardwareButtonWithNote("bt-track-focus-1", 42, 1);
      mBtTrackFocus[2] = createHardwareButtonWithNote("bt-track-focus-2", 43, 2);
      mBtTrackFocus[3] = createHardwareButtonWithNote("bt-track-focus-3", 44, 3);
      mBtTrackFocus[4] = createHardwareButtonWithNote("bt-track-focus-4", 57, 4);
      mBtTrackFocus[5] = createHardwareButtonWithNote("bt-track-focus-5", 58, 5);
      mBtTrackFocus[6] = createHardwareButtonWithNote("bt-track-focus-6", 59, 6);
      mBtTrackFocus[7] = createHardwareButtonWithNote("bt-track-focus-7", 60, 7);

      mBtTrackControl[0] = createHardwareButtonWithNote("bt-track-control-0", 73, 0);
      mBtTrackControl[1] = createHardwareButtonWithNote("bt-track-control-1", 74, 1);
      mBtTrackControl[2] = createHardwareButtonWithNote("bt-track-control-2", 75, 2);
      mBtTrackControl[3] = createHardwareButtonWithNote("bt-track-control-3", 76, 3);
      mBtTrackControl[4] = createHardwareButtonWithNote("bt-track-control-4", 89, 4);
      mBtTrackControl[5] = createHardwareButtonWithNote("bt-track-control-5", 90, 5);
      mBtTrackControl[6] = createHardwareButtonWithNote("bt-track-control-6", 91, 6);
      mBtTrackControl[7] = createHardwareButtonWithNote("bt-track-control-7", 92, 7);

      mBtDevice = createHardwareButtonWithNote("bt-device", 105, 0);
      mBtMute = createHardwareButtonWithNote("bt-mute", 106, 0);
      mBtSolo = createHardwareButtonWithNote("bt-solo", 107, 0);
      mBtRecordArm = createHardwareButtonWithNote("bt-record-arm", 108, 0);
   }

   private HardwareButton createHardwareButtonWithNote(final String id, final int note, final int indexInGroup)
   {
      final HardwareButton bt = mHardwareSurface.createHardwareButton(id);
      bt.setIndexInGroup(indexInGroup);
      bt.pressedAction().setActionMatcher(mMidiIn.createActionMatcher(
      "(status & 0xF0) == 0x90 && data1 == " + note
      ));
      bt.releasedAction().setActionMatcher(mMidiIn.createActionMatcher(
         "(status & 0xF0) == 0x80 && data1 == " + note
      ));
      return bt;
   }

   private void createLayers()
   {
      final Layers layers = new Layers(this);
      mMainLayer = new Layer(layers, "Main");

      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         mMainLayer.bind(mHardwareSliders[i], track.volume());
         mMainLayer.bindPressed(mBtTrackFocus[i], () -> mCursorTrack.selectChannel(track));
      }

      mMainLayer.bindPressed(mBtSendUp, () -> {
         for (int i = 0; i < 8; ++i)
            mTrackBank.getItemAt(i).sendBank().scrollBackwards();
      });
      mMainLayer.bindPressed(mBtSendDown, () -> {
         for (int i = 0; i < 8; ++i)
            mTrackBank.getItemAt(i).sendBank().scrollForwards();
      });
      mMainLayer.bindPressed(mBtTrackLeft, mTrackBank.scrollBackwardsAction());
      mMainLayer.bindPressed(mBtTrackRight, mTrackBank.scrollForwardsAction());
      mMainLayer.bindPressed(mBtDevice, () -> setDeviceOn(true));
      mMainLayer.bindReleased(mBtDevice, () -> setDeviceOn(false));
      mMainLayer.bindPressed(mBtMute, () -> setTrackControl(TrackControl.Mute));
      mMainLayer.bindPressed(mBtSolo, () -> setTrackControl(TrackControl.Solo));
      mMainLayer.bindPressed(mBtRecordArm, () -> setTrackControl(TrackControl.RecordArm));

      createModeLayers(layers);
      createTrackControlsLayers(layers);
      createDeviceLayer(layers);
   }

   private void createDeviceLayer(final Layers layers)
   {
      mDeviceLayer = new Layer(layers, "Device");
      mDeviceLayer.bindPressed(mBtTrackLeft, mCursorDevice.selectPreviousAction());
      mDeviceLayer.bindPressed(mBtTrackRight, mCursorDevice.selectNextAction());

      for (int i = 0; i < 8; ++i)
      {
         final int I = i;
         mDeviceLayer.bindPressed(mBtTrackControl[i], () -> mRemoteControls.selectedPageIndex().set(I));
      }
   }

   private void createModeLayers(final Layers layers)
   {
      mSend2FullDeviceLayer = new Layer(layers, "2 Sends Full Device");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend2FullDeviceLayer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2FullDeviceLayer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2FullDeviceLayer.bind(mHardwareKnobs[16 + i], mRemoteControls.getParameter(i));
      }

      mSend2Device1Layer = new Layer(layers, "2 Sends 1 Device");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend2Device1Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2Device1Layer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2Device1Layer.bind(mHardwareKnobs[16 + i], mTrackCursorDeviceRemoteControls[i].getParameter(0));
      }

      mSend1Device2Layer = new Layer(layers, "1 Sends 2 Device");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend1Device2Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend1Device2Layer.bind(mHardwareKnobs[8 + i], mTrackCursorDeviceRemoteControls[i].getParameter(0));
         mSend1Device2Layer.bind(mHardwareKnobs[16 + i], mTrackCursorDeviceRemoteControls[i].getParameter(1));
      }

      mDevice3Layer = new Layer(layers, "3 Device");
      for (int i = 0; i < 8; ++i)
      {
         mDevice3Layer.bind(mHardwareKnobs[i], mTrackCursorDeviceRemoteControls[i].getParameter(0));
         mDevice3Layer.bind(mHardwareKnobs[8 + i], mTrackCursorDeviceRemoteControls[i].getParameter(1));
         mDevice3Layer.bind(mHardwareKnobs[16 + i], mTrackCursorDeviceRemoteControls[i].getParameter(2));
      }

      mSend2Pan1Layer = new Layer(layers, "2 Sends 1 Pan");
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         mSend2Pan1Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2Pan1Layer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2Pan1Layer.bind(mHardwareKnobs[16 + i], track.pan());
      }

      mSend3Layer = new Layer(layers, "3 Sends");
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         mSend3Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend3Layer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend3Layer.bind(mHardwareKnobs[16 + i], sendBank.getItemAt(2));
      }

      mTrack3layer = new Layer(layers, "3 Track Remotes");
      for (int i = 0; i < 8; ++i)
      {
         final CursorRemoteControlsPage remoteControlPage = mTrackRemoteControls[i];
         mTrack3layer.bind(mHardwareKnobs[i], remoteControlPage.getParameter(0));
         mTrack3layer.bind(mHardwareKnobs[8 + i], remoteControlPage.getParameter(1));
         mTrack3layer.bind(mHardwareKnobs[16 + i], remoteControlPage.getParameter(2));
      }
   }

   private void createTrackControlsLayers(final Layers layers)
   {
      mMuteLayer = new Layer(layers, "Mute");
      for (int i = 0; i < 8; ++i)
         mMuteLayer.bindToggle(mBtTrackControl[i], mTrackBank.getItemAt(i).mute());

      mSoloLayer = new Layer(layers, "Solo");
      for (int i = 0; i < 8; ++i)
         mSoloLayer.bindToggle(mBtTrackControl[i], mTrackBank.getItemAt(i).solo());

      mRecordArmLayer = new Layer(layers, "Record Arm");
      for (int i = 0; i < 8; ++i)
         mRecordArmLayer.bindToggle(mBtTrackControl[i], mTrackBank.getItemAt(i).arm());
   }

   private void setTrackControl(final TrackControl trackControl)
   {
      mTrackControl = trackControl;
      mMuteLayer.setIsActive(trackControl == TrackControl.Mute);
      mSoloLayer.setIsActive(trackControl == TrackControl.Solo);
      mRecordArmLayer.setIsActive(trackControl == TrackControl.RecordArm);
   }

   private void setDeviceOn(final boolean isDeviceOn)
   {
      mIsDeviceOn = isDeviceOn;
      mDeviceLayer.setIsActive(isDeviceOn);
   }

   private void selectMode(final Mode mode)
   {
      mMode = mode;
      mSend2Device1Layer.setIsActive(mode == Mode.Send2Device1);
      mSend2Pan1Layer.setIsActive(mode == Mode.Send2Pan1);
      mSend3Layer.setIsActive(mode == Mode.Send3);
      mSend1Device2Layer.setIsActive(mode == Mode.Send1Device2);
      mDevice3Layer.setIsActive(mode == Mode.Device3);
      mTrack3layer.setIsActive(mode == Mode.Track3);
      mSend2FullDeviceLayer.setIsActive(mode == Mode.Send2FullDevice);

      mHost.showPopupNotification(mode.getNotification());
   }

   private void onSysex(final String sysex)
   {
      // mHost.println("Sysex IN1: " + sysex);

      switch (sysex)
      {
         case "f000202902117708f7" -> selectMode(Mode.Send2FullDevice);
         case "f000202902117709f7" -> selectMode(Mode.Send2Device1);
         case "f00020290211770af7" -> selectMode(Mode.Send2Pan1);
         case "f00020290211770bf7" -> selectMode(Mode.Send3);
         case "f00020290211770cf7" -> selectMode(Mode.Send1Device2);
         case "f00020290211770df7" -> selectMode(Mode.Device3);
         case "f00020290211770ef7" -> selectMode(Mode.Track3);
         default -> selectMode(Mode.None);
      }
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

      for (final SimpleLed simpleLed : mKnobsLed)
         simpleLed.flush(sb);

      for (final SimpleLed simpleLed : mBottomButtonsLed)
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
               case Mute -> mBottomButtonsLed[8 + i].setColor(track.mute().get()
                  ? SimpleLedColor.Green.value()
                  : SimpleLedColor.GreenLow.value());
               case Solo -> mBottomButtonsLed[8 + i].setColor(track.solo().get()
                  ? SimpleLedColor.Amber.value()
                  : SimpleLedColor.AmberLow.value());
               case RecordArm -> mBottomButtonsLed[8 + i].setColor(track.arm().get()
                  ? SimpleLedColor.Red.value()
                  : SimpleLedColor.RedLow.value());
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

         final int green = SimpleLedColor.Green.value();
         final int off = SimpleLedColor.Off.value();
         final int amber = SimpleLedColor.Amber.value();
         final int red = SimpleLedColor.Red.value();

         switch (mMode)
         {
            case Send2Device1 ->
            {
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? green : off);
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? green : off);
               mKnobsLed[16 + i].setColor(mTrackCursorDeviceRemoteControls[i].getParameter(0).exists().get() ? amber : off);
            }
            case Send2Pan1 ->
            {
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? green : off);
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? green : off);
               mKnobsLed[16 + i].setColor(track.exists().get() ? red : off);
            }
            case Send3 ->
            {
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? green : off);
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? green : off);
               mKnobsLed[16 + i].setColor(sendBank.getItemAt(2).exists().get() ? green : off);
            }
            case Send1Device2 ->
            {
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? green : off);
               mKnobsLed[8 + i].setColor(mTrackCursorDeviceRemoteControls[i].getParameter(0).exists().get() ? amber : off);
               mKnobsLed[16 + i].setColor(mTrackCursorDeviceRemoteControls[i].getParameter(1).exists().get() ? amber : off);
            }
            case Device3 ->
            {
               mKnobsLed[i].setColor(mTrackCursorDeviceRemoteControls[i].getParameter(0).exists().get() ? amber : off);
               mKnobsLed[8 + i].setColor(mTrackCursorDeviceRemoteControls[i].getParameter(1).exists().get() ? amber : off);
               mKnobsLed[16 + i].setColor(mTrackCursorDeviceRemoteControls[i].getParameter(2).exists().get() ? amber : off);
            }
            case Track3 ->
            {
               mKnobsLed[i].setColor(mTrackRemoteControls[i].getParameter(0).exists().get() ? amber : off);
               mKnobsLed[8 + i].setColor(mTrackRemoteControls[i].getParameter(1).exists().get() ? amber : off);
               mKnobsLed[16 + i].setColor(mTrackRemoteControls[i].getParameter(2).exists().get() ? amber : off);
            }
            case Send2FullDevice ->
            {
               mKnobsLed[i].setColor(sendBank.getItemAt(0).exists().get() ? green : off);
               mKnobsLed[8 + i].setColor(sendBank.getItemAt(1).exists().get() ? green : off);
               mKnobsLed[16 + i].setColor(mRemoteControls.getParameter(i).exists().get() ? amber : off);
            }
            case None ->
            {
               mKnobsLed[i].setColor(off);
               mKnobsLed[8 + i].setColor(off);
               mKnobsLed[16 + i].setColor(off);
            }
         }
      }
   }

   protected void paintRightButtons()
   {
      final int yellow = SimpleLedColor.Yellow.value();
      final int off = SimpleLedColor.Off.value();

      mDeviceLed.setColor(mIsDeviceOn ? yellow : off);
      mMuteLed.setColor(mTrackControl == TrackControl.Mute ? yellow : off);
      mSoloLed.setColor(mTrackControl == TrackControl.Solo ? yellow : off);
      mRecordArmLed.setColor(mTrackControl == TrackControl.RecordArm ? yellow : off);

      final SendBank sendBank = mTrackBank.getItemAt(0).sendBank();
      mUpButtonLed.setColor(sendBank.canScrollBackwards().get() ? yellow : off);
      mDownButtonLed.setColor(sendBank.canScrollForwards().get() ? yellow : off);

      if (mIsDeviceOn)
      {
         mLeftButtonLed.setColor(mCursorDevice.hasPrevious().get() ? yellow : off);
         mRightButtonLed.setColor(mCursorDevice.hasNext().get() ? yellow : off);
      }
      else
      {
         mLeftButtonLed.setColor(mTrackBank.canScrollBackwards().get() ? yellow : off);
         mRightButtonLed.setColor(mTrackBank.canScrollForwards().get() ? yellow : off);
      }
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

   private final SimpleLed[] mKnobsLed = new SimpleLed[] {
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

   private final SimpleLed[] mBottomButtonsLed = new SimpleLed[] {
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

   private final SimpleLed mDeviceLed = new SimpleLed(0x90, 40);
   private final SimpleLed mMuteLed = new SimpleLed(0x90, 41);
   private final SimpleLed mSoloLed = new SimpleLed(0x90, 42);
   private final SimpleLed mRecordArmLed = new SimpleLed(0x90, 43);
   private final SimpleLed mUpButtonLed = new SimpleLed(0x90, 44);
   private final SimpleLed mDownButtonLed = new SimpleLed(0x90, 45);
   private final SimpleLed mLeftButtonLed = new SimpleLed(0x90, 46);
   private final SimpleLed mRightButtonLed = new SimpleLed(0x90, 47);
   private final CursorDevice[] mTrackDeviceCursors = new CursorDevice[8];
   private final CursorRemoteControlsPage[] mTrackCursorDeviceRemoteControls = new CursorRemoteControlsPage[8];
   private final CursorRemoteControlsPage[] mTrackRemoteControls = new CursorRemoteControlsPage[8];

   private HardwareSurface mHardwareSurface;
   private final AbsoluteHardwareKnob[] mHardwareKnobs = new AbsoluteHardwareKnob[3 * 8];
   private final HardwareSlider[] mHardwareSliders = new HardwareSlider[8];
   private HardwareButton mBtSendUp;
   private HardwareButton mBtSendDown;
   private HardwareButton mBtTrackLeft;
   private HardwareButton mBtTrackRight;
   private final HardwareButton[] mBtTrackFocus = new HardwareButton[8];
   private final HardwareButton[] mBtTrackControl = new HardwareButton[8];
   private HardwareButton mBtDevice;
   private HardwareButton mBtMute;
   private HardwareButton mBtSolo;
   private HardwareButton mBtRecordArm;

   private Layer mSend2Device1Layer;
   private Layer mSend2Pan1Layer;
   private Layer mSend3Layer;
   private Layer mSend1Device2Layer;
   private Layer mDevice3Layer;
   private Layer mTrack3layer;
   private Layer mSend2FullDeviceLayer;
   private Layer mMuteLayer;
   private Layer mSoloLayer;
   private Layer mRecordArmLayer;
   private Layer mMainLayer;
   private Layer mDeviceLayer;
}
