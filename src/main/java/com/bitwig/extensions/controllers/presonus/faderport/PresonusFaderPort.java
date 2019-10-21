package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.targets.ButtonTarget;
import com.bitwig.extensions.framework.targets.ClickEncoderTarget;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.targets.RGBButtonTarget;
import com.bitwig.extensions.controllers.presonus.util.ValueUtils;

public class PresonusFaderPort extends LayeredControllerExtension
{
   static float[] WHITE = new float[] {1.f, 1.f, 1.f};
   static float[] DIM_WHITE = new float[] {0.1f, 0.1f, 0.1f};
   static float[] HALF_WHITE = new float[] {0.3f, 0.3f, 0.3f};
   static float[] BLACK = new float[] {0.f, 0.f, 0.f};
   static float[] ARM_LOW = new float[] {0.1f, 0.0f, 0.0f};
   static float[] ARM_HIGH = new float[] {1.0f, 0.0f, 0.0f};

   static int[] SELECT_IDS = {0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x7, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27};
   static int[] SOLOD_IDS = {0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x50, 0x51, 0x52, 0x58, 0x54, 0x55, 0x59, 0x57};

   public PresonusFaderPort(
      final PresonusFaderPortDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);

      mChannelCount = definition.channelCount();
      mChannels = new Channel[mChannelCount];
      mSysexHeader = "F0 00 01 06 " + definition.sysexDeviceID();
   }

   @Override
   public void init()
   {

      final ControllerHost host = getHost();
      mApplication = host.createApplication();
      mArranger = host.createArranger();
      mCueMarkerBank = mArranger.createCueMarkerBank(1);

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback(this::onMidi);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);

      mTrackBank = host.createTrackBank(mChannelCount, 1, 0, false);
      mMasterTrack = host.createMasterTrack(0);

      mCursorDevice =
         mCursorTrack.createCursorDevice("main", "Main", 0, CursorDeviceFollowMode.FOLLOW_SELECTION);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      if (mChannelCount > 8)
         mRemoteControls2 = mCursorDevice.createCursorRemoteControlsPage("right", 8, "");

      mTransport = host.createTransport();

      mClearMute = mApplication.getAction("clear_mute");
      mClearSolo = mApplication.getAction("clear_solo");

      ClickEncoder displayEncoder = addElement(new ClickEncoder(0x20, 0x10));

      mDefaultLayer.bind(displayEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
         }

         @Override
         public void inc(final int steps)
         {
            mCursorTrack.pan().inc(steps, mShift ? 1010 : 101);
         }
      });

      initTransportEncoder();

      // Link all send positions to the first
      mTrackBank.getItemAt(0).sendBank().scrollPosition().addValueObserver(p ->
      {
         for(int i=1; i<mChannelCount; i++)
            mTrackBank.getItemAt(i).sendBank().scrollPosition().set(p);
      });

      mSendsLayer.bind(displayEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
         }

         @Override
         public void inc(final int steps)
         {
            SendBank sendBank = mTrackBank.getItemAt(0).sendBank();

            if (steps > 0)
               sendBank.scrollBy(1);
            else
               sendBank.scrollBy(-1);
         }
      });

      Button arm = addElement(new Button(0x00));
      mDefaultLayer.bind(arm, new ButtonTarget()
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

      for(int index=0; index<mChannelCount; index++)
      {
         Channel channel = new Channel();

         channel.solo = addElement(new Button(SOLOD_IDS[index]));
         channel.mute = addElement(new Button((index >= 8 ? 0x70 : 0x10) + index));
         channel.select = addElement(new RGBButton(SELECT_IDS[index]));
         channel.motorFader = addElement(new MotorFader(index));
         channel.display = addElement(new Display(index, mSysexHeader));

         mChannels[index] = channel;
      }

      for(int channelIndex=0; channelIndex<mChannelCount; channelIndex++)
      {
         final Track track = mTrackBank.getItemAt(channelIndex);
         bindTrack(mDefaultLayer, channelIndex, track);
      }

      bindTrack(mMasterLayer, mChannelCount-1, mMasterTrack);

      Button master = addElement(new Button(0x3A));
      mDefaultLayer.bind(master, new ButtonTarget()
         {
            @Override
            public boolean get()
            {
               return isLayerActive(mMasterLayer);
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed) toggleLayer(mMasterLayer);
            }
         });

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
      mDefaultLayer.bind(shiftLeft, shiftTarget);
      mDefaultLayer.bind(shiftRight, shiftTarget);

      Button playButton = addElement(new Button(0x5e));
      mDefaultLayer.bindPressedRunnable(playButton, mTransport.isPlaying(), mTransport::togglePlay);

      Button stopButton = addElement(new Button(0x5d));
      mDefaultLayer.bind(stopButton, new ButtonTarget()
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
      mDefaultLayer.bindPressedRunnable(record, mTransport.isArrangerRecordEnabled(), mTransport::record);

      Button metronome = addElement(new Button(0x3B));
      mDefaultLayer.bindToggle(metronome, mTransport.isMetronomeEnabled());

      Button loop = addElement(new Button(0x56));
      mDefaultLayer.bindToggle(loop, mTransport.isArrangerLoopEnabled());

      Button rewind = addElement(new Button(0x5B));
      mDefaultLayer.bindPressedRunnable(rewind, null, mTransport::rewind);
      Button fastForward = addElement(new Button(0x5C));
      mDefaultLayer.bindPressedRunnable(fastForward, null, mTransport::fastForward);

      Button clearSolo = addElement(new Button(0x01));
      mDefaultLayer.bindPressedRunnable(clearSolo, mClearSolo.isEnabled(), mClearSolo::invoke);

      Button clearMute = addElement(new Button(0x02));
      mDefaultLayer.bindPressedRunnable(clearMute, mClearMute.isEnabled(), mClearMute::invoke);

      // Automation Write Modes
      RGBButton automationOff = addElement(new RGBButton(0x4F));
      mTransport.isArrangerAutomationWriteEnabled().markInterested();
      mTransport.automationWriteMode().markInterested();
      mDefaultLayer.bind(automationOff, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
            return isEnabled ? ARM_HIGH : DIM_WHITE;
         }

         @Override
         public boolean get()
         {
            return true;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) mTransport.isArrangerAutomationWriteEnabled().toggle();
         }
      });

      RGBButton automationLatch = addElement(new RGBButton(0x4E));
      mDefaultLayer.bind(automationLatch, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
            return isEnabled ? ARM_HIGH : DIM_WHITE;
         }

         @Override
         public boolean get()
         {
            return mTransport.automationWriteMode().get().equals("latch");
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.automationWriteMode().set("latch");
         }
      });

      RGBButton automationWrite = addElement(new RGBButton(0x4B));
      mDefaultLayer.bind(automationWrite, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
            return isEnabled ? ARM_HIGH : DIM_WHITE;
         }

         @Override
         public boolean get()
         {
            return mTransport.automationWriteMode().get().equals("write");
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.automationWriteMode().set("write");
         }
      });

      RGBButton automationTouch = addElement(new RGBButton(0x4D));
      mDefaultLayer.bind(automationTouch, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
            return isEnabled ? ARM_HIGH : DIM_WHITE;
         }

         @Override
         public boolean get()
         {
            return mTransport.automationWriteMode().get().equals("touch");
         }

         @Override
         public void set(final boolean pressed)
         {
            mTransport.automationWriteMode().set("touch");
         }
      });

      activateLayer(mDefaultLayer);
      activateLayer(mTrackLayer);

      Button trackMode = addElement(new Button(0x28));
      Button pluginMode = addElement(new Button(0x2B));
      Button sendsMode = addElement(new Button(0x29));
      Button panMode = addElement(new Button(0x2A));

      Layer[] faderGroup = {mTrackLayer, mDeviceLayer, mSendsLayer, mPanLayer};

      mDefaultLayer.bindLayerInGroup(this, trackMode, mTrackLayer, faderGroup);
      mDefaultLayer.bindLayerInGroup(this, pluginMode, mDeviceLayer, faderGroup);
      mDefaultLayer.bindLayerInGroup(this, sendsMode, mSendsLayer, faderGroup);
      mDefaultLayer.bindLayerInGroup(this, panMode, mPanLayer, faderGroup);

      runningStatusTimer();

      initDeviceMode();
   }

   private SettableIntegerValue getPageIndex()
   {
      if (mShift && mRemoteControls2 != null)
      {
         return mRemoteControls2.selectedPageIndex();
      }
      return mRemoteControls.selectedPageIndex();
   }

   private void initDeviceMode()
   {
      mRemoteControls.pageNames().markInterested();
      mRemoteControls.selectedPageIndex().markInterested();

      if (mRemoteControls2 != null)
      {
         mRemoteControls2.selectedPageIndex().markInterested();
      }

      for(int c = 0; c < mChannelCount; c++)
      {
         final int channelIndex = c;
         final int indexInGroup = c & 0x7;

         final RemoteControl parameter = c >= 8
            ? mRemoteControls2.getParameter(indexInGroup)
            : mRemoteControls.getParameter(indexInGroup);

         parameter.name().markInterested();
         parameter.displayedValue().markInterested();

         Channel channel = mChannels[c];

         mDeviceLayer.bind(channel.motorFader, parameter);

         mDeviceLayer.bindPressedRunnable(channel.mute, null, parameter::reset);

         mDeviceLayer.bind(channel.select, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               if (getPageIndex().get() == channelIndex)
               {
                  return WHITE;
               }

               if (mRemoteControls2 != null)
               {
                  if (mRemoteControls.selectedPageIndex().get() == channelIndex
                     || mRemoteControls2.selectedPageIndex().get() == channelIndex)
                  {
                     return HALF_WHITE;
                  }
               }

               return DIM_WHITE;
            }

            @Override
            public boolean get()
            {
               return channelIndex < mRemoteControls.pageNames().get().length;
            }

            @Override
            public void set(final boolean pressed)
            {
               getPageIndex().set(channelIndex);
            }
         });

         mDeviceLayer.bind(channel.display, new DisplayTarget()
         {
            @Override
            public int getBarValue()
            {
               if (channelIndex < 8)
               {
                  return ValueUtils.doubleToUnsigned7(parameter.value().get());
               }
               return 0;
            }

            @Override
            public String getText(final int line)
            {
               if (line == 0)
               {
                  final String[] pageNames = mRemoteControls.pageNames().get();

                  if (channelIndex == 15)
                  {
                     int rightIndex = mRemoteControls2.selectedPageIndex().get();
                     return rightIndex < pageNames.length ? pageNames[rightIndex] : "";
                  }

                  if (channelIndex < pageNames.length)
                  {
                     return pageNames[channelIndex];
                  }
               }

               if (line == 2) return parameter.name().getLimited(7);
               if (line == 3) return parameter.displayedValue().getLimited(7);

               return null;
            }

            @Override
            public DisplayMode getMode()
            {
               return DisplayMode.SmallText;
            }

            @Override
            public boolean isTextInverted(final int line)
            {
               if (line == 0 && getPageIndex().get() == channelIndex)
               {
                  return true;
               }

               if (line == 0 && channelIndex == 15)
               {
                  return true;
               }

               return false;
            }
         });
      }
   }

   private void initTransportEncoder()
   {
      Button scrollLeft = addElement(new Button(0x2E));
      Button scrollRight = addElement(new Button(0x2F));

      ClickEncoder transportEncoder = addElement(new ClickEncoder(0x53, 0x3C));
      mChannelLayer.bind(transportEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
            /*if (b)
            {
               if (mShift) mApplication.navigateToParentTrackGroup();
               else mApplication.navigateIntoTrackGroup(mCursorTrack);
            }*/
         }

         @Override
         public void inc(final int steps)
         {
            if (steps > 0)
               mCursorTrack.selectNext();
            else
               mCursorTrack.selectPrevious();
         }
      });

      mDefaultLayer.bindPressedRunnable(scrollLeft, mTrackBank.canScrollBackwards(), mTrackBank::scrollBackwards);
      mDefaultLayer.bindPressedRunnable(scrollRight, mTrackBank.canScrollForwards(), mTrackBank::scrollForwards);

      activateLayer(mChannelLayer);

      mZoomLayer.bind(transportEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
            if (b) mApplication.zoomToSelection();
         }

         @Override
         public void inc(final int steps)
         {
            if (steps > 0)
               mApplication.zoomIn();
            else
               mApplication.zoomOut();
         }
      });

      mScrollLayer.bind(transportEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
            if (b) mApplication.zoomToFit();
         }

         @Override
         public void inc(final int steps)
         {
         }
      });

      mBankLayer.bind(transportEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
            if (b) mTrackBank.scrollIntoView(mCursorTrack.position().get());
         }

         @Override
         public void inc(final int steps)
         {
            if (steps > 0) mTrackBank.scrollForwards();
            else mTrackBank.scrollBackwards();
         }
      });

      mBankLayer.bindPressedRunnable(scrollLeft, mTrackBank.canScrollBackwards(), mTrackBank::scrollPageBackwards);
      mBankLayer.bindPressedRunnable(scrollRight, mTrackBank.canScrollForwards(), mTrackBank::scrollPageForwards);

      mMarkerLayer.bind(transportEncoder, new ClickEncoderTarget()
      {
         @Override
         public void click(final boolean b)
         {
            if (b) mCueMarkerBank.getItemAt(0).launch(true);
         }

         @Override
         public void inc(final int steps)
         {
            if (steps > 0) mCueMarkerBank.scrollForwards();
            else mCueMarkerBank.scrollBackwards();
            mCueMarkerBank.scrollIntoView(0);
         }
      });

      mMarkerLayer.bindPressedRunnable(scrollLeft, mCueMarkerBank.canScrollBackwards(), mCueMarkerBank::scrollPageBackwards);
      mMarkerLayer.bindPressedRunnable(scrollRight, mCueMarkerBank.canScrollForwards(), mCueMarkerBank::scrollPageForwards);

      Layer[] layers = {mChannelLayer, mZoomLayer, mScrollLayer, mBankLayer, mSectionLayer, mMarkerLayer};

      Button channel = addElement(new Button(0x36));
      Button zoom = addElement(new Button(0x37));
      Button scroll = addElement(new Button(0x38));
      Button bank = addElement(new Button(0x39));
      Button section = addElement(new Button(0x3C));
      Button marker = addElement(new Button(0x3D));

      mDefaultLayer.bindLayerInGroup(this, channel, mChannelLayer, layers);
      mDefaultLayer.bindLayerInGroup(this, zoom, mZoomLayer, layers);
      mDefaultLayer.bindLayerInGroup(this, scroll, mScrollLayer, layers);
      mDefaultLayer.bindLayerInGroup(this, bank, mBankLayer, layers);
      mDefaultLayer.bindLayerInGroup(this, section, mSectionLayer, layers);
      mDefaultLayer.bindLayerInGroup(this, marker, mMarkerLayer, layers);
   }

   Channel[] mChannels;

   class Channel
   {
      Button solo;
      Button mute;
      RGBButton select;
      MotorFader motorFader;
      Display display;
   }

   private void bindTrack(final Layer layer, final int index, final Track track)
   {
      track.position().markInterested();
      track.name().markInterested();
      track.color().markInterested();
      final Parameter volume = track.volume();
      volume.markInterested();
      volume.displayedValue().markInterested();
      track.mute().markInterested();
      track.solo().markInterested();
      track.arm().markInterested();
      track.exists().markInterested();
      track.pan().markInterested();
      volume.markInterested();
      track.pan().name().markInterested();
      track.pan().displayedValue().markInterested();

      if (track != mMasterTrack)
      {
         track.sendBank().getItemAt(0).name().markInterested();
         track.sendBank().getItemAt(0).displayedValue().markInterested();
      }

      Channel channel = mChannels[index];
      final Button solo = channel.solo;
      final Button mute = channel.mute;
      final RGBButton select = channel.select;
      final MotorFader motorFader = channel.motorFader;
      final Display display = channel.display;

      layer.bindToggle(solo, track.solo());
      layer.bindToggle(mute, track.mute());

      final BooleanValue isSelected = mCursorTrack.createEqualsValue(track);

      if (track == mMasterTrack)
      {
         layer.bind(motorFader, volume);
      }
      else
      {
         mTrackLayer.bind(motorFader, volume);
         mPanLayer.bind(motorFader, track.pan());
         mSendsLayer.bind(motorFader, track.sendBank().getItemAt(0));
      }

      layer.bind(select, new RGBButtonTarget()
      {
         @Override
         public boolean get()
         {
            return track.exists().get();
         }

         @Override
         public float[] getRGB()
         {
            if (get())
            {
               if (mArm)
               {
                  return track.arm().get() ? ARM_HIGH : ARM_LOW;
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

      if (track == mMasterTrack)
      {
         layer.bind(display, new ChannelDisplayTarget(track, isSelected, motorFader));
      }
      else
      {
         mTrackLayer.bind(display, new ChannelDisplayTarget(track, isSelected, motorFader));
         mSendsLayer.bind(display, new SendDisplayTarget(track, isSelected, motorFader));
         mPanLayer.bind(display, new PanDisplayTarget(track, isSelected, motorFader));
      }
   }

   private void runningStatusTimer()
   {
      getMidiOut().sendMidi(0xA0, 0, 0);

      getHost().scheduleTask(this::runningStatusTimer, 1000);
   }

   @Override
   public void exit()
   {
   }

   class FPLayer extends Layer
   {
      @Override
      public void setActivate(final boolean active)
      {
         super.setActivate(active);

         if (active) updateIndications();
      }
   };

   private void updateIndications()
   {
      for(int c=0; c<mChannelCount; c++)
      {
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
   private final String mSysexHeader;

   private Layer mDefaultLayer = new FPLayer();
   private Layer mTrackLayer = new FPLayer();
   private Layer mDeviceLayer = new FPLayer();
   private Layer mSendsLayer = new FPLayer();
   private Layer mPanLayer = new FPLayer();

   private Layer mChannelLayer = new Layer();
   private Layer mZoomLayer = new Layer();
   private Layer mScrollLayer = new Layer();
   private Layer mBankLayer = new Layer();
   private Layer mMasterLayer = new Layer();
   private Layer mSectionLayer = new Layer();
   private Layer mMarkerLayer = new Layer();

   private static class PanDisplayTarget extends ChannelDisplayTarget
   {
      public PanDisplayTarget(
         final Track track, final BooleanValue isSelected, final MotorFader motorFader)
      {
         super(track, isSelected, motorFader);
      }

      @Override
      protected Parameter getMainControl()
      {
         return mTrack.pan();
      }
   };

   private static class SendDisplayTarget extends ChannelDisplayTarget
   {
      public SendDisplayTarget(
         final Track track, final BooleanValue isSelected, final MotorFader motorFader)
      {
         super(track, isSelected, motorFader);
      }

      @Override
      protected Parameter getMainControl()
      {
         return mTrack.sendBank().getItemAt(0);
      }

      @Override
      protected Parameter getLabelControl()
      {
         return mTrack.sendBank().getItemAt(0);
      }
   };

   private MasterTrack mMasterTrack;
   private Arranger mArranger;
   private CueMarkerBank mCueMarkerBank;
   private Action mClearMute;
   private Action mClearSolo;
   private CursorRemoteControlsPage mRemoteControls2;
}
