package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.presonus.framework.ButtonTarget;
import com.bitwig.extensions.controllers.presonus.framework.ControllerExtensionWithModes;
import com.bitwig.extensions.controllers.presonus.framework.Mode;
import com.bitwig.extensions.controllers.presonus.framework.RGBButtonTarget;

public class PresonusFaderPort extends ControllerExtensionWithModes
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

      Mode defaultMode = getDefaultMode();

      Button arm = addElement(new Button(0x00));
      defaultMode.bind(arm, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mArm;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) mArm = !mArm;
         }
      });

      for(int channelIndex=0; channelIndex<mChannelCount; channelIndex++)
      {
         int c = channelIndex;
         final Track track = mTrackBank.getItemAt(c);
         track.color().markInterested();
         final Parameter volume = track.volume();
         volume.markInterested();
         track.mute().markInterested();
         track.solo().markInterested();
         track.arm().markInterested();
         track.exists().markInterested();

         final Button solo = addElement(new Button((c >= 8 ? 0x48 : 0x08) + c));
         defaultMode.bindToggle(solo, track.solo());
         final Button mute = addElement(new Button((c >= 8 ? 0x70 : 0x10) + c));
         defaultMode.bindToggle(mute, track.mute());

         int[] selectId = {0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1d, 0x1e, 0x1f, 0x7, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27};
         final RGBButton select = addElement(new RGBButton(selectId[c]));
         final BooleanValue isSelected = mCursorTrack.createEqualsValue(track);

         defaultMode.bind(select, new RGBButtonTarget()
         {
            @Override
            public boolean get()
            {
               return track.exists().get();
            }

            @Override
            public float[] getRGB()
            {
               float[] WHITE = new float[] {1.f, 1.f, 1.f};
               float[] BLACK = new float[] {0.f, 0.f, 0.f};

               if (get())
               {
                  if (mArm)
                  {
                     float[] LOW = new float[] {0.2f, 0.0f, 0.0f};
                     float[] HIGH = new float[] {1.0f, 0.0f, 0.0f};
                     return track.arm().get() ? HIGH : LOW;
                  }
                  else
                  {
                     if (isSelected.get())
                        return WHITE;

                     SettableColorValue c = track.color();
                     float[] trackColor = new float[] {c.red(), c.green(), c.blue()};
                     return trackColor;
                  }
               }
               return BLACK;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
               {
                  if (mArm)
                     track.arm().toggle();
                  else
                     track.selectInEditor();
               }
            }
         });

         Fader fader = addElement(new Fader(c));
         defaultMode.bind(fader, volume);
      }

      Button shiftLeft = addElement(new Button(0x06));
      Button shiftRight = addElement(new Button(0x46));
      ButtonTarget shiftTarget = new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mShift;
         }

         @Override
         public void set(final boolean pressed)
         {
            mShift = pressed;
         }
      };
      defaultMode.bind(shiftLeft, shiftTarget);
      defaultMode.bind(shiftRight, shiftTarget);

      Button playButton = addElement(new Button(0x5e));
      defaultMode.bindPressedRunnable(playButton, mTransport.isPlaying(), mTransport::togglePlay);

      Button stopButton = addElement(new Button(0x5d));
      defaultMode.bind(stopButton, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return !mTransport.isPlaying().get();
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) mTransport.stop();
         }
      });

      Button record = addElement(new Button(0x5f));
      defaultMode.bindPressedRunnable(record, mTransport.isArrangerRecordEnabled(), mTransport::record);

      Button metronome = addElement(new Button(0x3B));
      defaultMode.bindToggle(metronome, mTransport.isMetronomeEnabled());

      Button loop = addElement(new Button(0x56));
      defaultMode.bindToggle(loop, mTransport.isArrangerLoopEnabled());

      Button rewind = addElement(new Button(0x5B));
      Button fastForward = addElement(new Button(0x5C));

      Button scrollLeft = addElement(new Button(0x2E));
      defaultMode.bindPressedRunnable(scrollLeft, mTrackBank.canScrollBackwards(), mTrackBank::scrollBackwards);
      Button scrollRight = addElement(new Button(0x2F));
      defaultMode.bindPressedRunnable(scrollRight, mTrackBank.canScrollForwards(), mTrackBank::scrollForwards);

      // Automation Write Modes
      Button automationOff = addElement(new Button(0x4F));
      mTransport.isArrangerAutomationWriteEnabled().markInterested();
      mTransport.automationWriteMode().markInterested();
      defaultMode.bind(automationOff, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return !mTransport.isArrangerAutomationWriteEnabled().get();
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.isArrangerAutomationWriteEnabled().set(false);
         }
      });

      Button automationLatch = addElement(new Button(0x4E));
      defaultMode.bind(automationLatch, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mTransport.isArrangerAutomationWriteEnabled().get() && mTransport.automationWriteMode().get().equals("latch");
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.isArrangerAutomationWriteEnabled().set(true);
            mTransport.automationWriteMode().set("latch");
         }
      });

      Button automationWrite = addElement(new Button(0x4B));
      defaultMode.bind(automationWrite, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mTransport.isArrangerAutomationWriteEnabled().get() && mTransport.automationWriteMode().get().equals("write");
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.isArrangerAutomationWriteEnabled().set(true);
            mTransport.automationWriteMode().set("write");
         }
      });

      Button automationTouch = addElement(new Button(0x4D));
      defaultMode.bind(automationTouch, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mTransport.isArrangerAutomationWriteEnabled().get() && mTransport.automationWriteMode().get().equals("touch");
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.isArrangerAutomationWriteEnabled().set(true);
            mTransport.automationWriteMode().set("touch");
         }
      });
   }

   @Override
   public void exit()
   {
   }

   @Override
   protected Mode createDefaultMode()
   {
      return new Mode()
      {
         @Override
         public void selected()
         {
            super.selected();

            updateIndications();
         }
      };
   }

   private void updateIndications()
   {
      for(int c=0; c<mChannelCount; c++)
      {
         final Track track = mTrackBank.getItemAt(c);
         final Parameter volume = track.volume();
         volume.setIndication(getMode() == getDefaultMode());
      }
   }

   @Override
   protected MidiOut getMidiOut()
   {
      return mMidiOut;
   }

   /* API Objects */
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private Transport mTransport;
   private MidiOut mMidiOut;
   private Application mApplication;
   private boolean mShift;
   private final int mChannelCount;
   private TrackBank mTrackBank;
   private boolean mArm;
}
