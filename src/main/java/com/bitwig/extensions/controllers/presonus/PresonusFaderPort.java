package com.bitwig.extensions.controllers.presonus;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class PresonusFaderPort extends ControllerExtension
{
   public PresonusFaderPort(
      final PresonusFaderPortDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);

      mChannelCount = definition.channelCount();
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      mApplication = host.createApplication();

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback(this::onMidi);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);

      mTrackBank = host.createTrackBank(mChannelCount, 1, 0, false);

      mCursorDevice =
         mCursorTrack.createCursorDevice("main", "Main", 0, CursorDeviceFollowMode.FOLLOW_SELECTION);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(4);

      mTransport = host.createTransport();

      for(int c=0; c<mChannelCount; c++)
      {
         Fader fader = new Fader(c);
         Parameter volume = mTrackBank.getItemAt(c).volume();
         volume.markInterested();

         fader.setTarget(volume);
         mFaders.add(fader);
      }

      mFlushables.addAll(mFaders);
      mMidiReceivers.addAll(mFaders);
   }


   @Override
   public void exit()
   {
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      for (MidiReceiver midiReceiver : mMidiReceivers)
      {
         midiReceiver.onMidi(status, data1, data2);
      }
   }

   @Override
   public void flush()
   {
      for (Flushable flushable : mFlushables)
      {
         flushable.flush(mMidiOut);
      }
   }

   /* API Objects */
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private Transport mTransport;
   private MidiOut mMidiOut;
   private Application mApplication;
   private DrumPadBank mDrumPadBank;
   private List<AtomButton> mButtons = new ArrayList<>();
   private boolean mShift;
   private NoteInput mNoteInput;
   private AtomButton mMetronomeButton;
   private List<Flushable> mFlushables = new ArrayList<>();
   private List<MidiReceiver> mMidiReceivers = new ArrayList<>();
   private final int mChannelCount;
   private TrackBank mTrackBank;
   private List<Fader> mFaders = new ArrayList<>();
}
