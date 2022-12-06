package com.bitwig.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class EzCreatorPad extends ControllerExtension
{
   public EzCreatorPad(final EzCreatorPadDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();

      final MidiIn midiIn = host.getMidiInPort(0);
      final NoteInput noteInput = midiIn.createNoteInput("EZ-Creator Pad", "89????", "99????");
      noteInput.setShouldConsumeEvents(true);

      final Integer[] keyTranslationTable = new Integer[128];
      for (int i = 0; i < keyTranslationTable.length; ++i)
      {
         if (0x30 <= i && i <= 0x3B)
            keyTranslationTable[i] = i - 12;
         else
            keyTranslationTable[i] = 0;
      }

      noteInput.setKeyTranslationTable(keyTranslationTable);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(EzCreatorCommon.INIT_SYSEX);

      mCursorTrack = host.createCursorTrack(0, 0);
      final PinnableCursorDevice cursorDevice = mCursorTrack.createCursorDevice();
      final CursorRemoteControlsPage remoteControls = cursorDevice.createCursorRemoteControlsPage(1);
      remoteControls.setHardwareLayout(HardwareControlType.SLIDER, 1);
      mParameter = remoteControls.getParameter(0);
      mParameter.markInterested();

      createHardwareControls();
      createLayers();
   }

   private void createLayers()
   {
      mLayers = new Layers(this);
      mMainLayer = new Layer(mLayers, "Main");

      mMainLayer.bind(mSlider, mParameter);
      mMainLayer.bindPressed(mPlayButton, mTransport.isPlaying().toggleAction());
      mMainLayer.bindPressed(mStopButton, mTransport.stopAction());
      mMainLayer.bindPressed(mRecordButton, mTransport.isArrangerRecordEnabled().toggleAction());
      mMainLayer.bindPressed(mLoopButton, mTransport.isArrangerLoopEnabled().toggleAction());
      mMainLayer.bindPressed(mFastForwardButton, mTransport.fastForwardAction());
      mMainLayer.bindPressed(mRewindButton, mTransport.rewindAction());
      mMainLayer.bindPressed(mNextTrackButton, mCursorTrack.selectNextAction());
      mMainLayer.bindPressed(mPreviousTrackButton, mCursorTrack.selectPreviousAction());
      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      final HardwareSurface surface = getHost().createHardwareSurface();
      surface.setPhysicalSize(300, 80);
      mHardwareSurface = surface;

      final MidiIn midiIn = getMidiInPort(0);

      mSlider = surface.createHardwareSlider("xy");
      mSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x02));

      mPlayButton = surface.createHardwareButton("play");
      mPlayButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x1C, 127));

      mStopButton = surface.createHardwareButton("stop");
      mStopButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x1F, 127));

      mRecordButton = surface.createHardwareButton("record");
      mRecordButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x20, 127));

      mLoopButton = surface.createHardwareButton("loop");
      mLoopButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x1E, 127));

      mFastForwardButton = surface.createHardwareButton("fast-forward");
      mFastForwardButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x1D, 127));

      mRewindButton = surface.createHardwareButton("rewind");
      mRewindButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x1B, 127));

      mNextTrackButton = surface.createHardwareButton("next-track");
      mNextTrackButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 65, 127));

      mPreviousTrackButton = surface.createHardwareButton("previous-track");
      mPreviousTrackButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 64, 127));

      surface.hardwareElementWithId("xy").setBounds(68.75, 6.5, 10.0, 50.0);
      surface.hardwareElementWithId("play").setBounds(50.0, 27.0, 10.0, 5.5);
      surface.hardwareElementWithId("stop").setBounds(38.0, 27.0, 10.0, 5.5);
      surface.hardwareElementWithId("record").setBounds(25.75, 27.0, 10.0, 5.5);
      surface.hardwareElementWithId("loop").setBounds(25.5, 17.25, 10.0, 5.5);
      surface.hardwareElementWithId("fast-forward").setBounds(50.0, 17.25, 10.0, 5.5);
      surface.hardwareElementWithId("rewind").setBounds(38.0, 17.25, 10.0, 5.5);
      surface.hardwareElementWithId("next-track").setBounds(48.25, 62.75, 10.0, 5.5);
      surface.hardwareElementWithId("previous-track").setBounds(60.25, 62.75, 10.0, 5.5);
   }

   @Override
   public void exit()
   {
      mMidiOut.sendSysex(EzCreatorCommon.DEINIT_SYSEX);
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   private Transport mTransport;
   private MidiOut mMidiOut;
   private RemoteControl mParameter;
   private CursorTrack mCursorTrack;
   private HardwareSlider mSlider;
   private HardwareButton mPlayButton;
   private HardwareButton mStopButton;
   private Layers mLayers;
   private Layer mMainLayer;
   private HardwareButton mRecordButton;
   private HardwareButton mLoopButton;
   private HardwareButton mFastForwardButton;
   private HardwareButton mRewindButton;
   private HardwareButton mNextTrackButton;
   private HardwareButton mPreviousTrackButton;
   private HardwareSurface mHardwareSurface;
}
