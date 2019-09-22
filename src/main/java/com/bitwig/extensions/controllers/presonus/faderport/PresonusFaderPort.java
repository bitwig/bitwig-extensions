package com.bitwig.extensions.controllers.presonus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.presonus.atom.AtomButton;

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
         final Channel channel = new Channel(c);
         final Track track = mTrackBank.getItemAt(c);
         final Parameter volume = track.volume();
         volume.markInterested();
         track.mute().markInterested();
         track.solo().markInterested();

         channel.setTarget(track);

         channel.getSelect().setBooleanValue(mCursorTrack.createEqualsValue(track));

         mChannels.add(channel);
      }

      addSimpleToggleButton(0x5e, mTransport.isPlaying());
      addSimpleToggleButton(0x38, mTransport.isMetronomeEnabled());
      addSimpleToggleButton(0x2E, mTrackBank.canScrollBackwards(), mTrackBank::scrollBackwards);
      addSimpleToggleButton(0x2F, mTrackBank.canScrollForwards(), mTrackBank::scrollForwards);

      mFlushables.addAll(mChannels);
      mMidiReceivers.addAll(mChannels);
   }

   private void addSimpleToggleButton(final int ID, final SettableBooleanValue value)
   {
      final ToggleButton button = new ToggleButton(ID);
      button.setBooleanValue(value);
      button.setRunnable(value::toggle);
      value.markInterested();

      mFlushables.add(button);
      mMidiReceivers.add(button);
   }

   private void addSimpleToggleButton(final int ID, final BooleanValue value, final Runnable runnable)
   {
      final ToggleButton button = new ToggleButton(ID);
      button.setBooleanValue(value);
      button.setRunnable(runnable);
      value.markInterested();

      mFlushables.add(button);
      mMidiReceivers.add(button);
   }

   private void addSimpleToggleButton(final int ID, final BooleanSupplier value, final Runnable runnable)
   {
      final ToggleButton button = new ToggleButton(ID);
      button.setBooleanSupplier(value);
      button.setRunnable(runnable);

      mFlushables.add(button);
      mMidiReceivers.add(button);
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
   private List<AtomButton> mButtons = new ArrayList<>();
   private boolean mShift;
   private NoteInput mNoteInput;
   private AtomButton mMetronomeButton;
   private List<Flushable> mFlushables = new ArrayList<>();
   private List<MidiReceiver> mMidiReceivers = new ArrayList<>();
   private final int mChannelCount;
   private TrackBank mTrackBank;
   private List<Channel> mChannels = new ArrayList<>();
}
