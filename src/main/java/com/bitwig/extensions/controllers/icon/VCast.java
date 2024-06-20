package com.bitwig.extensions.controllers.icon;

import java.util.UUID;
import java.util.function.Consumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extension.controller.api.SpecificPluginDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class VCast extends ControllerExtension
{
   protected VCast(
      final ControllerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      mTransport = host.createTransport();
      mCursorTrack = host.createCursorTrack(2, 0);

      mHardwareSurface = host.createHardwareSurface();
      mHardwareSurface.setPhysicalSize(165, 205);

      initButtons();
      initEncoders();
      initDisplays();
      initFader();
      initVuMeter();
      doLayout();

      initLayers();
   }

   @Override
   public void exit()
   {

   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();

      mPlatformDisplay.flush();

      if (mFaderPosition != mFaderPositionBefore)
      {
         final int data1 = mFaderPosition & 0x7f;
         final int data2 = (mFaderPosition & 0x3f80) >> 7;
         mMidiOut.sendMidi(0xE0, data1, data2);
         mFaderPositionBefore = mFaderPosition;
      }
   }

   private void initButtons()
   {
      addButton("<<");
      addButton("|<");
      addButton("Start/Stop");
      addButton(">|");
      addButton(">>");

      mDynamicsButton = addCCButton("Dynamics", "Dynamics", 1, 0x30);
      mPitchShiftButton = addCCButton("Pitch Shift", "Pitch Shift", 1, 0x31);
      mAutoTuneButton = addCCButton("Auto Tune", "Auto Tune", 1, 0x32);

      mUpButton = addNoteOnButton("Up", "Up", 0, 44);
      mDownButton = addNoteOnButton("Down", "Down", 0, 45);
      mUpDownButton = addNoteOnButton("Up+Down", "Up+Down", 0, 66);

      for (int i = 0; i < 16; ++i)
      {
         final String id = "Clip " + (i + 1);
         mClipButtons[i] = addCCButton(id, id, 1, 32 + i);
      }

      mMuteButton = addNoteOnButton("Mute", "Mute", 0, 16);
      mMasterButton = addNoteOnButton("Master", "Master", 0, 112);

      for (int i = 0; i < N_CHANNELS; ++i)
      {
         final String id = "Channel " + i;
         mChannelButtons[i] = addNoteOnButton(id, id, i / 8, 24 + (i % 8));
      }

      mRewindButton = addNoteOnButton("Rewind", "Rewind", 0, 91);
      mFastForwardButton = addNoteOnButton("FastForward", "FastForward", 0, 92);
      mLoopButton = addNoteOnButton("Loop", "Loop", 0, 86);
      mStopButton = addNoteOnButton("Stop", "Stop", 0, 93);
      mPlayButton = addNoteOnButton("Play", "Play", 0, 94);
      mRecButton = addNoteOnButton("Rec", "Rec", 0, 95);
   }

   private void initEncoders()
   {
      final int valueAmountForOneFullRotation = 40;

      for (int i = 0; i < 8; ++i)
      {
         mTopKnobs[i] = mHardwareSurface.createRelativeHardwareKnob("topKnob" + i);
         mTopKnobs[i].setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0,
            16 + i,
            valueAmountForOneFullRotation));
         mTopKnobButtons[i] = mHardwareSurface.createHardwareButton("topKnobButton" + i);
         mTopKnobButtons[i].pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 32 + i));
      }

      final int[] turnControlNumbers = {24, 28, 29};
      final int[] pressNotes = {3, 64, 65};
      for (int i = 0; i < 3; ++i)
      {
         mLeftKnobs[i] = mHardwareSurface.createRelativeHardwareKnob("leftKnob" + i);
         mLeftKnobs[i].setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0,
            turnControlNumbers[i],
            valueAmountForOneFullRotation));
         mLeftKnobButtons[i] = mHardwareSurface.createHardwareButton("leftKnobButton" + i);
         mLeftKnobButtons[i].pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, pressNotes[i]));
      }
   }

   private void initDisplays()
   {
      mPlatformDisplay = new PlatformD3Display(mMidiOut, mHardwareSurface);
      mLeftDisplay = new VCastDisplay(getHost(), mHardwareSurface);
   }

   private void initFader()
   {
      mSlider = mHardwareSurface.createHardwareSlider("slider");
      mSlider.setAdjustValueMatcher(mMidiIn.createAbsolutePitchBendValueMatcher(0));
      mSlider.disableTakeOver();
      mSlider.value().markInterested();
   }

   private void initVuMeter()
   {
      mVuMeterLight = mHardwareSurface.createMultiStateHardwareLight("vuMeter");
      mVuMeterLight.state().setValue(new VuMeterState());
      mVuMeterLight.state().onUpdateHardware((state) -> {
         if (state instanceof final VuMeterState s)
            mMidiOut.sendMidi(0xD0, 0x30 + s.value(), 0x00);
      });
   }

   private void doLayout()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("<<").setBounds(65.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId("|<").setBounds(85.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId("Start/Stop").setBounds(105.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId(">|").setBounds(125.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId(">>").setBounds(145.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId("Dynamics").setBounds(45.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId("Pitch Shift").setBounds(45.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("Auto Tune").setBounds(45.0, 105.0, 15.0, 15.0);
      surface.hardwareElementWithId("Up").setBounds(25.0, 105.0, 15.0, 10.0);
      surface.hardwareElementWithId("Down").setBounds(25.0, 117.5, 15.0, 10.0);
      surface.hardwareElementWithId("Up+Down").setBounds(25.0, 130.0, 15.0, 10.0);
      surface.hardwareElementWithId("Clip 1").setBounds(65.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 2").setBounds(85.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 3").setBounds(105.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 4").setBounds(125.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 5").setBounds(145.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 6").setBounds(65.0, 105.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 7").setBounds(85.0, 105.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 8").setBounds(105.0, 105.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 9").setBounds(125.0, 105.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 10").setBounds(145.0, 105.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 11").setBounds(45.0, 125.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 12").setBounds(65.0, 125.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 13").setBounds(85.0, 125.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 14").setBounds(105.0, 125.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 15").setBounds(125.0, 125.0, 15.0, 15.0);
      surface.hardwareElementWithId("Clip 16").setBounds(145.0, 125.0, 15.0, 15.0);
      surface.hardwareElementWithId("Mute").setBounds(25.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Master").setBounds(25.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 0").setBounds(25.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 1").setBounds(45.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 2").setBounds(65.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 3").setBounds(85.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 4").setBounds(105.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 5").setBounds(125.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 6").setBounds(145.0, 145.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 7").setBounds(45.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 8").setBounds(65.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 9").setBounds(85.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 10").setBounds(105.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 11").setBounds(125.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Channel 12").setBounds(145.0, 165.0, 15.0, 15.0);
      surface.hardwareElementWithId("Rewind").setBounds(45.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("FastForward").setBounds(65.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("Loop").setBounds(85.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("Stop").setBounds(105.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("Play").setBounds(125.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("Rec").setBounds(145.0, 185.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnob0").setBounds(5.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton0").setBounds(5.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob1").setBounds(25.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton1").setBounds(25.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob2").setBounds(45.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton2").setBounds(45.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob3").setBounds(65.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton3").setBounds(65.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob4").setBounds(85.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton4").setBounds(85.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob5").setBounds(105.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton5").setBounds(105.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob6").setBounds(125.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton6").setBounds(125.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("topKnob7").setBounds(145.0, 25.0, 15.0, 15.0);
      surface.hardwareElementWithId("topKnobButton7").setBounds(145.0, 25.0, 3.0, 3.0);
      surface.hardwareElementWithId("leftKnob0").setBounds(25.0, 45.0, 15.0, 15.0);
      surface.hardwareElementWithId("leftKnobButton0").setBounds(25.0, 45.0, 3.0, 3.0);
      surface.hardwareElementWithId("leftKnob1").setBounds(25.0, 65.0, 15.0, 15.0);
      surface.hardwareElementWithId("leftKnobButton1").setBounds(25.0, 65.0, 3.0, 3.0);
      surface.hardwareElementWithId("leftKnob2").setBounds(25.0, 85.0, 15.0, 15.0);
      surface.hardwareElementWithId("leftKnobButton2").setBounds(25.0, 85.0, 3.0, 3.0);
      surface.hardwareElementWithId("slider").setBounds(5.0, 85.0, 6.0, 115.0);
      surface.hardwareElementWithId("topDisplay0").setBounds(5.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay1").setBounds(25.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay2").setBounds(45.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay3").setBounds(65.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay4").setBounds(85.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay5").setBounds(105.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay6").setBounds(125.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("topDisplay7").setBounds(145.0, 5.0, 155.0, 15.0);
      surface.hardwareElementWithId("leftDisplay").setBounds(5.0, 45.0, 15.0, 35.0);
      surface.hardwareElementWithId("vuMeter").setBounds(16.0, 85.0, 6.75, 115.0);
   }

   private void initLayers()
   {
      mLayers = new Layers(this);
      mBaseLayer = new Layer(mLayers, "Base");
      mPitchShiftLayer = new Layer(mLayers, "Pitch Shift");
      mAutoTuneLayer = new Layer(mLayers, "Auto Tune");
      mDeviceSelectionLayer = new Layer(mLayers, "Device Selection");

      initBaseLayer();
      initPitchShiftLayer();
      initAutoTuneLayer();
      initDeviceSelectionLayer();

      mBaseLayer.activate();
      mPitchShiftLayer.activate();
   }

   private void initBaseLayer()
   {
      final ControllerHost host = getHost();

      mCursorTrack.volume().value().addValueObserver((value) -> mFaderPosition = (int) (value * 16383));

      final PinnableCursorDevice cursorDevice = mCursorTrack.createCursorDevice();
      final CursorRemoteControlsPage remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);

      // Top Section (displays and encoders)

      for (int i = 0; i < mTopKnobs.length; ++i)
      {
         final RemoteControl parameter = remoteControlsPage.getParameter(i);

         mBaseLayer.bind(parameter.value().displayedValue(), mPlatformDisplay.display(i), 0);
         mBaseLayer.bind(parameter.name(), mPlatformDisplay.display(i), 1);

         mBaseLayer.bind(mTopKnobs[i], remoteControlsPage.getParameter(i).value());
         remoteControlsPage.getParameter(i).value().markInterested();

         mBaseLayer.bindPressed(mTopKnobButtons[i], remoteControlsPage.getParameter(i)::reset);
      }


      // Left section (display, encoders, fader)

      mBaseLayer.bindToggle(mPitchShiftButton, () -> {
         mPitchShiftLayer.activate();
         mAutoTuneLayer.deactivate();
      }, mPitchShiftLayer::isActive);
      mBaseLayer.bindToggle(mAutoTuneButton, () -> {
         mPitchShiftLayer.deactivate();
         mAutoTuneLayer.activate();
      }, mAutoTuneLayer::isActive);

      mBaseLayer.bind(mCursorTrack.name(), mLeftDisplay.display(), 0);
      mCursorTrack.volume().value().displayedValue().markInterested();
      mBaseLayer.bind(() -> {
         final String s = mCursorTrack.volume().value().displayedValue().get();
         return s.length() < 8 ? s : s.replace(" ", "");
      }, mLeftDisplay.display(), 1);
      for (int i = 0; i < 2; ++i)
      {
         final Send send = mCursorTrack.sendBank().getItemAt(i);

         // Although we are only interested in `displayedValue`, we mark ourselves as interested in `value` as well,
         // because otherwise the display sometimes does not update correctly. This is probably a framework bug, but
         // the workaround is easy enough for now.
         send.value().markInterested();

         send.displayedValue().markInterested();
         mBaseLayer.bind(() -> send.displayedValue().get().replace(" dB", ""), mLeftDisplay.display(), 3 + i);
      }

      for (int i = 0; i < 2; ++i)
      {
         final Send send1 = mCursorTrack.sendBank().getItemAt(i);
         mBaseLayer.bind(mLeftKnobs[i + 1], send1);
         mBaseLayer.bindPressed(mLeftKnobButtons[i + 1], send1::reset);
      }

      mBaseLayer.bind(mSlider, mCursorTrack.volume());

      // TODO should this be tied to the layer?!
      mCursorTrack.addVuMeterObserver(VuMeterState.MAX_VALUE + 1,
         -1,
         false,
         (level) -> mVuMeterLight.state().setValue(new VuMeterState(level)));


      // Buttons

      // Top transport section
      // TODO specs pending

      // Dynamics button
      // Note: pitch shift and auto tune are not part of the base layer
      bindDeviceToggle(mDynamicsButton, "dynamicsCursor", "Dynamics", host.createBitwigDeviceMatcher(dynamicsDeviceID));

      // Clip launchers
      final CursorTrack clipLauncherCursorTrack = host.createCursorTrack("clipLauncherCursor", "Clips", 2, 0, true);
      for (int i = 0; i < mClipButtons.length; ++i)
      {
         final int key = 36 + i;
         mBaseLayer.bindPressed(mClipButtons[i], () -> clipLauncherCursorTrack.playNote(key, 127));
      }


      // Channel selection
      final TrackBank channelBank = host.createMainTrackBank(N_CHANNELS, 0, 0);
      mCursorTrack.position().markInterested();
      channelBank.channelCount().markInterested();
      for (int i = 0; i < N_CHANNELS; ++i)
      {
         final int trackIdx = i;
         mBaseLayer.bindToggle(mChannelButtons[i],
            () -> {
               channelBank.scrollIntoView(0);
               channelBank.getItemAt(trackIdx).selectInMixer();
            },
            () -> mCursorTrack.position().get() == trackIdx
               && mCursorTrack.position().get() < channelBank.channelCount().get());
      }

      // Up, down, mute, master
      mBaseLayer.bindPressed(mUpButton, remoteControlsPage.selectPreviousAction());
      mBaseLayer.bindPressed(mDownButton, remoteControlsPage.selectNextAction());
      mBaseLayer.bindToggle(mUpDownButton, mDeviceSelectionLayer);
      mBaseLayer.bindToggle(mMuteButton, () -> mCursorTrack.mute().toggle(), mCursorTrack.mute());
      mCursorTrack.hasNext().markInterested();
      mBaseLayer.bindToggle(mMasterButton, mCursorTrack::selectLast, () -> !mCursorTrack.hasNext().get());

      // Bottom Transport section
      mBaseLayer.bindPressed(mRewindButton, this::rewind);
      mBaseLayer.bind(mRewindButton.isPressed(), (OnOffHardwareLight) mRewindButton.backgroundLight());
      mBaseLayer.bindPressed(mFastForwardButton, this::fastForward);
      mBaseLayer.bind(mFastForwardButton.isPressed(), (OnOffHardwareLight) mFastForwardButton.backgroundLight());
      mBaseLayer.bindToggle(mLoopButton, mTransport.isArrangerLoopEnabled());
      mBaseLayer.bindToggle(mPlayButton, mTransport.isPlaying());
      mBaseLayer.bindToggle(mStopButton, mTransport::stop, () -> !mTransport.isPlaying().get());
      mBaseLayer.bindToggle(mRecButton, mTransport.isArrangerRecordEnabled());
   }


   private void seekWhileButtonIsDown(final HardwareButton button, final Runnable action, final int tick)
   {
      if (!button.isPressed().get())
      {
         return;
      }

      // To get a nicer feel, seek once upon initial press, then do not seek for 300 ms, then seek once every 10 ms
      final int tickPeriodInMs = 10;
      if (tick == 0 || tick >= 30)
      {
         action.run();
      }

      getHost().scheduleTask(() -> seekWhileButtonIsDown(button, action, tick + 1), tickPeriodInMs);
   }

   private void rewind()
   {
      seekWhileButtonIsDown(mRewindButton, mTransport::rewind, 0);
   }

   private void fastForward()
   {
      seekWhileButtonIsDown(mFastForwardButton, mTransport::fastForward, 0);

   }

   private void initPitchShiftLayer()
   {
      final ControllerHost host = getHost();
      final CursorTrack cursorTrack = host.createCursorTrack("pitchShifterCursor", "Pitch Shifter", 2, 0, true);
      final DeviceBank deviceBank = cursorTrack.createDeviceBank(8);
      deviceBank.setDeviceMatcher(host.createBitwigDeviceMatcher(pitchShifterDeviceID));
      final SpecificBitwigDevice device = deviceBank.getItemAt(0).createSpecificBitwigDevice(pitchShifterDeviceID);
      final Parameter parameter = device.createParameter("PITCH");
      mPitchShiftLayer.bind(mLeftKnobs[0], parameter);
      mPitchShiftLayer.bindPressed(mLeftKnobButtons[0], parameter::reset);
      parameter.value().markInterested(); // needed to make parameter.displayedValue() work in all cases
      mPitchShiftLayer.bind(parameter.displayedValue(), mLeftDisplay.display(), 2);
   }

   private void initAutoTuneLayer()
   {
      final ControllerHost host = getHost();
      final CursorTrack cursorTrack = host.createCursorTrack("autoTuneCursor", "Auto Tune", 2, 0, true);
      final DeviceBank deviceBank = cursorTrack.createDeviceBank(8);
      deviceBank.setDeviceMatcher(host.createVST3DeviceMatcher(autoTuneDeviceID));
      final SpecificPluginDevice device = deviceBank.getItemAt(0).createSpecificVst3Device(autoTuneDeviceID);
      final Parameter parameter = device.createParameter(autoTuneParameterID);
      mAutoTuneLayer.bind(mLeftKnobs[0], parameter);
      mAutoTuneLayer.bindPressed(mLeftKnobButtons[0], parameter::reset);
      parameter.value().markInterested(); // needed to make parameter.displayedValue() work in all cases
      mAutoTuneLayer.bind(parameter.displayedValue(), mLeftDisplay.display(), 2);
   }

   private void initDeviceSelectionLayer()
   {
      final DeviceBank deviceBank = mCursorTrack.createDeviceBank(8);

      for (int i = 0; i < 8; ++i)
      {
         final Device device = deviceBank.getItemAt(i);
         mDeviceSelectionLayer.bind(device.name(), mPlatformDisplay.display(i), 0);
         mDeviceSelectionLayer.bind(() -> "", mPlatformDisplay.display(i), 1);
         mDeviceSelectionLayer.bind(mTopKnobs[i], (value) -> {
         });
         mDeviceSelectionLayer.bindPressed(mTopKnobButtons[i], () -> {
            device.selectInEditor();
            mDeviceSelectionLayer.deactivate();
         });
      }
   }

   protected HardwareButton addButton(
      final String id,
      final String label,
      final HardwareActionMatcher pressedMatcher,
      final HardwareActionMatcher releasedMatcher,
      final Consumer<Boolean> sendValueConsumer)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton(id);
      button.setLabel(label);
      button.pressedAction().setActionMatcher(pressedMatcher);
      button.releasedAction().setActionMatcher(releasedMatcher);

      final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight(id + "_light");
      if (sendValueConsumer != null)
      {
         light.isOn().onUpdateHardware(sendValueConsumer);
      }
      button.setBackgroundLight(light);

      return button;
   }

   protected HardwareButton addButton(final String id)
   {
      return addButton(id, id, null, null, null);
   }

   protected HardwareButton addNoteOnButton(final String id, final String label, final int channel, final int note)
   {
      return addButton(id,
         label,
         mMidiIn.createNoteOnActionMatcher(channel, note),
         mMidiIn.createNoteOffActionMatcher(channel, note),
         (value) -> mMidiOut.sendMidi(0x90 + channel, note, value ? 0x7f : 0x00));
   }

   protected HardwareButton addCCButton(final String id, final String label, final int channel, final int controlNumber)
   {
      return addButton(id,
         label,
         mMidiIn.createCCActionMatcher(channel, controlNumber, 127),
         mMidiIn.createCCActionMatcher(channel, controlNumber, 0),
         (value) -> mMidiOut.sendMidi(0xB0 + channel, controlNumber, value ? 0x7f : 0x00));
   }

   private void bindDeviceToggle(
      final HardwareButton button, final String cursorId, final String cursorName, final DeviceMatcher deviceMatcher)
   {
      final CursorTrack cursorTrack = getHost().createCursorTrack(cursorId, cursorName, 2, 0, true);
      final DeviceBank deviceBank = cursorTrack.createDeviceBank(1);
      deviceBank.setDeviceMatcher(deviceMatcher);
      deviceBank.getDevice(0).isEnabled().markInterested();
      mBaseLayer.bindToggle(button, deviceBank.getDevice(0).isEnabled()::toggle, deviceBank.getDevice(0).isEnabled());
   }

   private static class VuMeterState extends InternalHardwareLightState
   {
      public VuMeterState()
      {
      }

      public VuMeterState(final int value)
      {
         setValue(value);
      }

      @Override
      public HardwareLightVisualState getVisualState()
      {
         final double blend = (double) mValue / MAX_VALUE;
         final Color color = Color.mix(Color.whiteColor(), Color.blackColor(), blend);
         return HardwareLightVisualState.createForColor(color);
      }

      @Override
      public boolean equals(final Object obj)
      {
         if (!(obj instanceof VuMeterState))
         {
            return false;
         }
         final VuMeterState other = (VuMeterState) obj;
         return other.value() == value();
      }

      public int value()
      {
         return mValue;
      }

      public void setValue(final int value)
      {
         if (0 <= value && value <= MAX_VALUE)
         {
            mValue = value;
         }
      }

      public static final int MAX_VALUE = 12;
      private int mValue = 0;
   }

   private static final int N_CHANNELS = 13;

   private static final UUID dynamicsDeviceID = UUID.fromString("22e785a2-a187-41e9-a0f2-66343694014c");
   private static final UUID pitchShifterDeviceID = UUID.fromString("384fe469-6023-4f69-9560-e0c2eec2da49");
   private static final String autoTuneDeviceID = "00C2B33FE2034457BA45E1BED447B693";
   private static final int autoTuneParameterID = 2;

   protected MidiIn mMidiIn;
   private MidiOut mMidiOut;

   protected Transport mTransport;
   private CursorTrack mCursorTrack;

   protected HardwareSurface mHardwareSurface;
   private PlatformD3Display mPlatformDisplay;
   private VCastDisplay mLeftDisplay;
   private final RelativeHardwareKnob[] mTopKnobs = new RelativeHardwareKnob[8];
   private final HardwareButton[] mTopKnobButtons = new HardwareButton[8];
   private final RelativeHardwareKnob[] mLeftKnobs = new RelativeHardwareKnob[3];
   private final HardwareButton[] mLeftKnobButtons = new HardwareButton[3];
   private HardwareButton mDynamicsButton, mPitchShiftButton, mAutoTuneButton;
   private HardwareButton mUpButton, mDownButton, mUpDownButton;
   private final HardwareButton[] mClipButtons = new HardwareButton[16];
   private final HardwareButton[] mChannelButtons = new HardwareButton[N_CHANNELS];
   private HardwareButton mMuteButton, mMasterButton;
   private HardwareButton mRewindButton, mFastForwardButton, mLoopButton, mStopButton, mPlayButton, mRecButton;
   private HardwareSlider mSlider;
   private MultiStateHardwareLight mVuMeterLight;

   protected Layers mLayers;
   private Layer mBaseLayer;
   private Layer mPitchShiftLayer;
   private Layer mAutoTuneLayer;
   private Layer mDeviceSelectionLayer;

   // Hardware output cache

   private int mFaderPosition = 0, mFaderPositionBefore = -1;
}
