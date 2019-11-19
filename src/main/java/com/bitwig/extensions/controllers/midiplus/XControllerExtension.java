package com.bitwig.extensions.controllers.midiplus;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class XControllerExtension extends ControllerExtension
{
   static final int REQUIRED_API_VERSION = 10;

   public XControllerExtension(
      final ControllerExtensionDefinition definition,
      final ControllerHost host,
      final int numPads,
      final int numKnobs,
      final byte[] initSysex,
      final byte[] deinitSysex)
   {
      super(definition, host);

      mKeyboardInputName = definition.getHardwareModel() + (numPads > 0 ? " Keys" : "");
      mPadsInputName = definition.getHardwareModel() + " Pads";
      mNumPads = numPads;
      mNumKnobs = numKnobs;
      mInitSysex = initSysex;
      mDeinitSysex = deinitSysex;
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);
      mMidiIn.createNoteInput(mKeyboardInputName, "80????", "90????", "b001??", "e0????", "b040??").setShouldConsumeEvents(true);
      if (mNumPads > 0)
         mMidiIn.createNoteInput(mPadsInputName, "89????", "99????").setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);
      mMidiOut.sendSysex(mInitSysex);

      mCursorTrack = host.createCursorTrack("X2mini-track-cursor", "X2mini", 0, 0, true);

      if (mNumKnobs == 9)
         mCursorTrack.volume().setIndication(true);

      mCursorDevice =
         mCursorTrack.createCursorDevice("X2mini-device-cursor", "X2mini", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      final int numRemoteControls = Math.min(mNumKnobs, 8);
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(numRemoteControls);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, numRemoteControls);
      for (int i = 0; i < numRemoteControls; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();

      mTrackBank = host.createTrackBank(8, 0, 0, true);

      createHardwareControls();
      createLayers();
   }

   private void createLayers()
   {
      mLayers = new Layers(this);
      createMainLayer();
   }

   private void createHardwareControls()
   {
      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(400, 160);

      createKnobs();
      createCurrentTrackVolumeControl();
      createTransportControls();
      createTrackSelectControls();

      setGeometry();
   }

   private void setGeometry()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("Rewind").setBounds(10.0, 40.0, 20.0, 8.0);
      surface.hardwareElementWithId("Forward").setBounds(40.0, 40.0, 20.0, 8.0);
      surface.hardwareElementWithId("Stop").setBounds(70.0, 40.0, 20.0, 8.0);
      surface.hardwareElementWithId("Play").setBounds(100.0, 40.0, 20.0, 8.0);
      surface.hardwareElementWithId("Loop").setBounds(130.0, 40.0, 20.0, 8.0);
      surface.hardwareElementWithId("Record").setBounds(160.0, 40.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-0").setBounds(10.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-1").setBounds(40.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-2").setBounds(70.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-3").setBounds(100.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-4").setBounds(130.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-5").setBounds(160.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-6").setBounds(190.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("TrackSelect-7").setBounds(220.0, 60.0, 20.0, 8.0);
      surface.hardwareElementWithId("CurstorTrackVolumeKnob").setBounds(324.5, 12.25, 20.75, 18.5);
   }

   private void createMainLayer()
   {
      mMainLayer = new Layer(mLayers, "Main");

      mMainLayer.bind(mCursorTrackVolumeKnob, mCursorTrack.volume());

      mMainLayer.bindPressed(mRewindButton, mTransport.rewindAction());
      mMainLayer.bindPressed(mForwardButton, mTransport.fastForwardAction());
      mMainLayer.bindPressed(mStopButton, mTransport.stopAction());
      mMainLayer.bindPressed(mPlayButton, mTransport.playAction());
      mMainLayer.bindPressed(mLoopButton, mTransport.isArrangerLoopEnabled().toggleAction());
      mMainLayer.bindPressed(mRecordButton, mTransport.recordAction());

      for (int i = 0; i < mNumKnobs; ++i)
         mMainLayer.bind(mKnobs[i], mRemoteControls.getParameter(i));

      for (int i = 0; i < 8; ++i)
      {
         final int j = i;
         final Track channelToSelect = mTrackBank.getItemAt(i);
         final HardwareActionBindable action = getHost().createAction(
            () -> mCursorTrack.selectChannel(channelToSelect),
            () -> "Selects the track " + j + " from the track bank.");
         mMainLayer.bindPressed(mTrackSelectButtons[i], action);
      }

      mMainLayer.activate();
   }

   private void createCurrentTrackVolumeControl()
   {
      mCursorTrackVolumeKnob = mHardwareSurface.createAbsoluteHardwareKnob("CurstorTrackVolumeKnob");
      mCursorTrackVolumeKnob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, 0x07));
   }

   private void createTrackSelectControls()
   {
      mTrackSelectButtons = new HardwareButton[8];
      for (int i = 0; i < 8; ++i)
      {
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackSelect-" + i);
         bt.pressedAction().setActionMatcher(mMidiIn.createCCActionMatcher(0, 0x18 + i));
         mTrackSelectButtons[i] = bt;
      }
   }

   private void createTransportControls()
   {
      mRewindButton = mHardwareSurface.createHardwareButton("Rewind");
      mRewindButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 0x5A));

      mForwardButton = mHardwareSurface.createHardwareButton("Forward");
      mForwardButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 0x5B));

      mStopButton = mHardwareSurface.createHardwareButton("Stop");
      mStopButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 0x5C));

      mPlayButton = mHardwareSurface.createHardwareButton("Play");
      mPlayButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 0x5D));

      mLoopButton = mHardwareSurface.createHardwareButton("Loop");
      mLoopButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 0x5E));

      mRecordButton = mHardwareSurface.createHardwareButton("Record");
      mRecordButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, 0x5F));
   }

   private void createKnobs()
   {
      mKnobs = new AbsoluteHardwareKnob[mNumKnobs];
      for (int i = 0; i < mNumKnobs; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("Knob-" + i);
         knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(0, 0x10 + i));
         knob.setLabel("T" + (i + 1));
         knob.setBounds(10 + i * (20 + 25), 10, 20, 20);
         mKnobs[i] = knob;
      }
   }

   @Override
   public void exit()
   {
      // Restore the controller in the factory setting
      mMidiOut.sendSysex(mDeinitSysex);
   }

   @Override
   public void flush()
   {
   }

   /* Configuration */
   private final String mKeyboardInputName;
   private final String mPadsInputName;
   private final int mNumPads;
   private final int mNumKnobs;
   private final byte[] mInitSysex;
   private final byte[] mDeinitSysex;

   /* API Objects */
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private Transport mTransport;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private TrackBank mTrackBank;

   /* Hardware stuff */
   private Layers mLayers;
   private Layer mMainLayer;
   private HardwareSurface mHardwareSurface;
   private HardwareButton mRewindButton;
   private HardwareButton mForwardButton;
   private HardwareButton mStopButton;
   private HardwareButton mPlayButton;
   private HardwareButton mLoopButton;
   private HardwareButton mRecordButton;
   private AbsoluteHardwareKnob mCursorTrackVolumeKnob;
   private HardwareButton[] mTrackSelectButtons;
   private AbsoluteHardwareKnob[] mKnobs;
}
