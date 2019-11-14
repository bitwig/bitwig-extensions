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

      final MidiIn midiIn = host.getMidiInPort(0);
      midiIn.createNoteInput(mKeyboardInputName, "80????", "90????", "b001??", "e0????", "b040??").setShouldConsumeEvents(true);
      if (mNumPads > 0)
         midiIn.createNoteInput(mPadsInputName, "89????", "99????").setShouldConsumeEvents(true);

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

      mHardwareSurface = getHost().createHardwareSurface();
      createMainLayer(host, midiIn);
   }

   private void createMainLayer(final ControllerHost host, final MidiIn midiIn)
   {
      mLayers = new Layers(this);
      mMainLayer = new Layer(mLayers, "Main");
      mMainLayer.activate();

      createKnobs(midiIn);
      createCurrentTrackVolumeControl(midiIn);
      createTransportControls(midiIn);
      createTrackSelectControls(host, midiIn);
   }

   private void createCurrentTrackVolumeControl(final MidiIn midiIn)
   {
      final AbsoluteHardwareKnob cursorTrackVolumeKnob =
         mHardwareSurface.createAbsoluteHardwareKnob("CurstorTrackVolumeKnob");
      cursorTrackVolumeKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x07));
      mMainLayer.bind(cursorTrackVolumeKnob, mCursorTrack.volume());
   }

   private void createTrackSelectControls(final ControllerHost host, final MidiIn midiIn)
   {
      for (int i = 0; i < 8; ++i)
      {
         final int j = i;
         final Track channelToSelect = mTrackBank.getItemAt(i);
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackSelect-" + i);
         bt.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x18 + i));
         final HardwareActionBindable action = host.createAction(
            () -> mCursorTrack.selectChannel(channelToSelect),
            () -> "Selects the track " + j + " from the track bank.");
         mMainLayer.bindPressed(bt, action);
      }
   }

   private void createTransportControls(final MidiIn midiIn)
   {
      final HardwareButton rewindButton = mHardwareSurface.createHardwareButton("Rewind");
      rewindButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5A));
      mMainLayer.bindPressed(rewindButton, mTransport.rewindAction());

      final HardwareButton forwardButton = mHardwareSurface.createHardwareButton("Forward");
      forwardButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5B));
      mMainLayer.bindPressed(forwardButton, mTransport.fastForwardAction());

      final HardwareButton stopButton = mHardwareSurface.createHardwareButton("Stop");
      stopButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5C));
      mMainLayer.bindPressed(stopButton, mTransport.stopAction());

      final HardwareButton playButton = mHardwareSurface.createHardwareButton("Play");
      playButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5D));
      mMainLayer.bindPressed(playButton, mTransport.playAction());

      final HardwareButton loopButton = mHardwareSurface.createHardwareButton("Loop");
      loopButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5E));
      mMainLayer.bindPressed(loopButton, mTransport.isArrangerLoopEnabled().toggleAction());

      final HardwareButton recordButton = mHardwareSurface.createHardwareButton("Record");
      recordButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5F));
      mMainLayer.bindPressed(recordButton, mTransport.recordAction());
   }

   private void createKnobs(final MidiIn midiIn)
   {
      for (int i = 0; i < mNumKnobs; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("Knob-" + i);
         knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x10 + i));
         mMainLayer.bind(knob, mRemoteControls.getParameter(i));
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
   private MidiOut mMidiOut;
   private TrackBank mTrackBank;

   /* Hardware stuff */
   private Layers mLayers;
   private Layer mMainLayer;
   private HardwareSurface mHardwareSurface;
}
