package com.bitwig.extensions.controllers.midiplus;

import java.util.function.Supplier;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class XControllerExtension extends ControllerExtension
{
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
      mHardwareKnobs = new AbsoluteHardwareKnob[mNumKnobs];
      for (int i = 0; i < mNumKnobs; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("Knob-" + i);
         knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x10 + i));
         knob.setBinding(mRemoteControls.getParameter(0));
         mHardwareKnobs[i] = knob;
      }

      mCursorTrackVolumeKnob = mHardwareSurface.createAbsoluteHardwareKnob("CurstorTrackVolumeKnob");
      mCursorTrackVolumeKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x07));
      mCursorTrackVolumeKnob.setBinding(mCursorTrack.volume());

      mRewindButton = mHardwareSurface.createHardwareButton("Rewind");
      mRewindButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5A));
      mRewindButton.pressedAction().setBinding(mTransport.rewindAction());

      mForwardButton = mHardwareSurface.createHardwareButton("Forward");
      mForwardButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5B));
      mForwardButton.pressedAction().setBinding(mTransport.fastForwardAction());

      mStopButton = mHardwareSurface.createHardwareButton("Stop");
      mStopButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5C));
      mStopButton.pressedAction().setBinding(mTransport.stopAction());

      mPlayButton = mHardwareSurface.createHardwareButton("Play");
      mPlayButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5D));
      mPlayButton.pressedAction().setBinding(mTransport.playAction());

      mLoopButton = mHardwareSurface.createHardwareButton("Loop");
      mLoopButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5E));
      mLoopButton.pressedAction().setBinding(mTransport.isArrangerLoopEnabled().toggleAction());

      mRecordButton = mHardwareSurface.createHardwareButton("Record");
      mRecordButton.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 0x5F));
      mRecordButton.pressedAction().setBinding(mTransport.recordAction());

      mTrackSelectButtons = new HardwareButton[8];
      for (int i = 0; i < 8; ++i)
      {
         final int j = i;
         final Track channelToSelect = mTrackBank.getItemAt(i);
         final HardwareButton bt = mHardwareSurface.createHardwareButton("TrackSelect-" + i);
         bt.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 0x18 + i));
         bt.pressedAction().setBinding(host.createAction(() -> mCursorTrack.selectChannel(channelToSelect),
            () -> "Selects the track " + j + " from the track bank."));
         mTrackSelectButtons[i] = bt;
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
   private HardwareSurface mHardwareSurface;
   private AbsoluteHardwareKnob[] mHardwareKnobs;
   private AbsoluteHardwareKnob mCursorTrackVolumeKnob;
   private HardwareButton mRewindButton;
   private HardwareButton mForwardButton;
   private HardwareButton mStopButton;
   private HardwareButton mPlayButton;
   private HardwareButton mLoopButton;
   private HardwareButton mRecordButton;
   private HardwareButton[] mTrackSelectButtons;
}
