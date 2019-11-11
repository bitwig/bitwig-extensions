package com.bitwig.extensions.controllers.presonus.faderport;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.LayerGroup;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.util.ValueUtils;

public class PresonusFaderPort extends ControllerExtension
{
   private static final Color WHITE = Color.fromRGB(1.f, 1.f, 1.f);

   private static final Color DIM_WHITE = Color.fromRGB(0.1f, 0.1f, 0.1f);

   private static final Color HALF_WHITE = Color.fromRGB(0.3f, 0.3f, 0.3f);

   private static final Color BLACK = Color.fromRGB(0.f, 0.f, 0.f);

   private static final Color ARM_LOW = Color.fromRGB(0.1f, 0.0f, 0.0f);

   private static final Color ARM_HIGH = Color.fromRGB(1.0f, 0.0f, 0.0f);

   static int[] SELECT_IDS = { 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x7, 0x21, 0x22, 0x23, 0x24,
         0x25, 0x26, 0x27 };

   static int[] SOLOD_IDS = { 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x50, 0x51, 0x52, 0x58, 0x54,
         0x55, 0x59, 0x57 };

   private RelativeHardwareKnob mDisplayEncoder;

   private HardwareButton mMasterButton;

   private HardwareButton mShiftLeftButton;

   private HardwareButton mShiftRightButton;

   private HardwareButton mPlayButton;

   private HardwareButton mStopButton;

   private HardwareButton mRecordButton;

   private HardwareButton mMetronomeButton;

   private HardwareButton mLoopButton;

   private HardwareButton mRewindButton;

   private HardwareButton mClearSoloButton;

   private HardwareButton mClearMuteButton;

   private HardwareButton mTrackModeButton;

   private HardwareButton mPluginModeButton;

   private HardwareButton mSendsModeButton;

   private HardwareButton mPanModeButton;

   private HardwareButton mScrollLeftButton;

   private HardwareButton mScrollRightButton;

   private RelativeHardwareKnob mTransportEncoder;

   private HardwareButton mChannelButton;

   private HardwareButton mZoomButton;

   private HardwareButton mScrollButton;

   private HardwareButton mBankButton;

   private HardwareButton mSectionButton;

   private HardwareButton mMarkerButton;

   private HardwareButton mAutomationOffButton;

   private HardwareButton mAutomationWriteButton;

   private HardwareButton mAutomationTouchButton;

   public PresonusFaderPort(final PresonusFaderPortDefinition definition, final ControllerHost host)
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

      mMidiIn = host.getMidiInPort(0);
      mMidiIn.setMidiCallback((ShortMidiMessageReceivedCallback)this::onMidi);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);

      mTrackBank = host.createTrackBank(mChannelCount, 1, 0, false);
      mMasterTrack = host.createMasterTrack(0);

      mCursorDevice = mCursorTrack.createCursorDevice("main", "Main", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      if (mChannelCount > 8)
         mRemoteControls2 = mCursorDevice.createCursorRemoteControlsPage("right", 8, "");

      mTransport = host.createTransport();

      mClearMute = mApplication.getAction("clear_mute");
      mClearSolo = mApplication.getAction("clear_solo");

      // Link all send positions to the first
      mTrackBank.getItemAt(0).sendBank().scrollPosition().addValueObserver(p -> {
         for (int i = 1; i < mChannelCount; i++)
            mTrackBank.getItemAt(i).sendBank().scrollPosition().set(p);
      });

      // Automation Write Modes
      mTransport.isArrangerAutomationWriteEnabled().markInterested();
      mTransport.automationWriteMode().markInterested();

      initHardwareSurface();
      initLayers();

      runningStatusTimer();

      initDeviceMode();
   }

   private void initHardwareSurface()
   {
      mHardwareSurface = getHost().createHardwareSurface();

      mArmButton = createToggleButton("arm", 0);
      mMasterButton = createToggleButton("master", 0x3A);
      mShiftLeftButton = createToggleButton("shift_left", 0x06);
      mShiftRightButton = createToggleButton("shift_right", 0x46);
      mPlayButton = createToggleButton("play", 0x5e);
      mStopButton = createToggleButton("stop", 0x5d);
      mRecordButton = createToggleButton("record", 0x5f);
      mMetronomeButton = createToggleButton("metronome", 0x3B);
      mLoopButton = createToggleButton("loop", 0x56);
      mRewindButton = createToggleButton("rewind", 0x5B);
      mClearSoloButton = createToggleButton("clear_solo", 0x01);
      mClearMuteButton = createToggleButton("clear_mute", 0x02);

      mTrackModeButton = createToggleButton("track_mode", 0x28);
      mPluginModeButton = createToggleButton("plugin_mode", 0x2B);
      mSendsModeButton = createToggleButton("sends_mode", 0x29);
      mPanModeButton = createToggleButton("pan_mode", 0x2A);

      mScrollLeftButton = createToggleButton("scroll_left", 0x2E);
      mScrollRightButton = createToggleButton("scroll_right", 0x2F);

      mChannelButton = createToggleButton("channel", 0x36);
      mZoomButton = createToggleButton("zoom", 0x37);
      mScrollButton = createToggleButton("scroll", 0x38);
      mBankButton = createToggleButton("bank", 0x39);
      mSectionButton = createToggleButton("section", 0x3C);
      mMarkerButton = createToggleButton("marker", 0x3D);

      mDisplayEncoder = createClickEncoder("display", 0x20, 0x10);
      mTransportEncoder = createClickEncoder("transport", 0x53, 0x3C);

      mAutomationOffButton = createRGBButton("automation_on_off", 0x4F);
      mAutomationWriteButton = createRGBButton("automation_write", 0x4B);
      mAutomationTouchButton = createRGBButton("automation_touch", 0x4D);

      for (int index = 0; index < mChannelCount; index++)
      {
         final Channel channel = new Channel(index);

         mChannels[index] = channel;
      }
   }

   private HardwareButton createToggleButton(final String id, final int note)
   {
      final HardwareButton button = createButton(id, note);

      final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight();
      button.setBackgroundLight(light);

      light.isOn().onUpdateHardware(isOn -> {
         mMidiOut.sendMidi(0x90, note, isOn ? 127 : 0);
      });

      return button;
   }

   private HardwareButton createRGBButton(final String id, final int note)
   {
      final HardwareButton button = createButton(id, note);

      final MultiStateHardwareLight light = mHardwareSurface
         .createMultiStateHardwareLight(PresonusFaderPort::stateToColor);
      button.setBackgroundLight(light);

      light.setColorToStateFunction(PresonusFaderPort::colorToState);

      final IntConsumer sendState = new IntConsumer()
      {
         @Override
         public void accept(final int state)
         {
            for (int i = 0; i < 4; i++)
            {
               final int shift = 24 - 8 * i;
               final int byteValue = (state & (0x7F << shift)) >> shift;

               assert byteValue >= 0 && byteValue <= 127;

               if (mLastSent[i] != byteValue)
               {
                  mMidiOut.sendMidi(0x90 + i, note, byteValue);
                  mLastSent[i] = byteValue;
               }
            }
         }

         private final int[] mLastSent = new int[] { -1, -1, -1, -1 };
      };

      light.state().onUpdateHardware(sendState);

      return button;
   }

   private HardwareButton createButton(final String id, final int note)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton(id);

      button.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, note));
      button.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, note));

      return button;
   }

   private HardwareSlider createMotorFader(final int channel)
   {
      final HardwareSlider fader = mHardwareSurface.createHardwareSlider("fader" + (channel + 1));

      fader.setAdjustValueMatcher(mMidiIn.createAbsolutePitchBendValueMatcher(channel));

      fader.beginTouchAction().setActionMatcher(
         mMidiIn.createActionMatcher("status == 0x90 && data1 == " + (0x68 + channel) + " && data2 > 0"));
      fader.endTouchAction().setActionMatcher(
         mMidiIn.createActionMatcher("status == 0x90 && data1 == " + (0x68 + channel) + " && data2 == 0"));

      final DoubleConsumer moveFader = new DoubleConsumer()
      {
         @Override
         public void accept(final double value)
         {
            final int faderValue = Math.max(0, Math.min(16383, (int)(value * 16384.0)));

            if (mLastSentValue != value)
            {
               mMidiOut.sendMidi(0xE0 | channel, faderValue & 0x7f, faderValue >> 7);
               mLastSentValue = faderValue;
            }
         }

         private int mLastSentValue = -1;
      };

      // TODO: Register move fader callback somehow

      return fader;
   }

   private RelativeHardwareKnob createClickEncoder(final String id, final int key, final int CC)
   {
      final RelativeHardwareKnob encoder = mHardwareSurface.createRelativeHardwareKnob(id);

      encoder.setAdjustValueMatcher(mMidiIn.createRelative2sComplementCCValueMatcher(0, CC));

      final HardwareButton clickButton = mHardwareSurface.createHardwareButton(id + "_click");

      clickButton.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, key));
      clickButton.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, key));

      encoder.setHardwareButton(clickButton);

      return encoder;
   }

   private static int colorToState(final Color c)
   {
      if (c == null) // Off
         return 0;

      return 0x7F000000 | colorPartFromDouble(c.getRed()) << 16 | colorPartFromDouble(c.getGreen()) << 8
         | colorPartFromDouble(c.getBlue());
   }

   private static Color stateToColor(final int state)
   {
      final int red = (state & 0x7F0000) >> 16;
      final int green = (state & 0x7F00) >> 8;
      final int blue = (state & 0x7F);

      return Color.fromRGB(red / 127.0, green / 127.0, blue / 127.0);
   }

   private static int colorPartFromDouble(final double x)
   {
      return Math.max(0, Math.min((int)(127.0 * x), 127));
   }

   private void initLayers()
   {
      mDefaultLayer = createLayer("Default");
      mSendsLayer = createLayer("Sends");
      mPanLayer = createLayer("Pan");
      mTrackLayer = createLayer("Track");
      mChannelLayer = createLayer("Channel");
      mMarkerLayer = createLayer("Marker");
      mDeviceLayer = createLayer("Device");
      mZoomLayer = createLayer("Zoom");
      mScrollLayer = createLayer("Scroll");
      mBankLayer = createLayer("Bank");
      mMasterLayer = createLayer("Master");
      mSectionLayer = createLayer("Section");

      new LayerGroup(mTrackLayer, mDeviceLayer, mSendsLayer, mPanLayer);

      new LayerGroup(mChannelLayer, mZoomLayer, mScrollLayer, mBankLayer, mSectionLayer, mMarkerLayer);

      initDefaultLayer();
      initSendsLayer();
      initChannelLayer();
      initMarkerLayer();
      initDeviceLayer();
      initZoomLayer();
      initScrollLayer();
      initBankLayer();
      initMasterLayer();
      initSectionLayer();

      mDefaultLayer.activate();
      mTrackLayer.activate();

      // DebugUtilities.createDebugLayer(mLayers, mHardwareSurface).activate();
   }

   private Layer createLayer(final String name)
   {
      return new Layer(mLayers, name);
   }

   private void initDefaultLayer()
   {
      mDefaultLayer.bindPressed(mArmButton, () -> mArm = !mArm);
      mDefaultLayer.bind(() -> mArm, mArmButton);

      mDefaultLayer.bindToggle(mMasterButton, mMasterLayer);

      mDefaultLayer.bind(mDisplayEncoder, mCursorTrack.pan());

      mDefaultLayer.bindPressed(mShiftLeftButton, () -> mShift = true);
      mDefaultLayer.bindReleased(mShiftLeftButton, () -> mShift = false);
      mDefaultLayer.bind(() -> mShift, mShiftLeftButton);

      mDefaultLayer.bindPressed(mShiftRightButton, () -> mShift = true);
      mDefaultLayer.bindReleased(mShiftRightButton, () -> mShift = false);
      mDefaultLayer.bind(() -> mShift, mShiftRightButton);

      mDefaultLayer.bindToggle(mPlayButton, mTransport.isPlaying());
      mDefaultLayer.bindToggle(mStopButton, mTransport.stopAction(), () -> !mTransport.isPlaying().get());
      mDefaultLayer.bindToggle(mRecordButton, mTransport.isArrangerRecordEnabled());
      mDefaultLayer.bindToggle(mMetronomeButton, mTransport.isMetronomeEnabled());
      mDefaultLayer.bindToggle(mLoopButton, mTransport.isArrangerLoopEnabled());
      mDefaultLayer.bindPressed(mRewindButton, mTransport.rewindAction());
      mDefaultLayer.bindToggle(mClearSoloButton, mClearSolo, mClearSolo.isEnabled());
      mDefaultLayer.bindToggle(mClearMuteButton, mClearMute, mClearMute.isEnabled());

      mDefaultLayer.bindToggle(mTrackModeButton, mTrackLayer);
      mDefaultLayer.bindToggle(mPluginModeButton, mDeviceLayer);
      mDefaultLayer.bindToggle(mSendsModeButton, mSendsLayer);
      mDefaultLayer.bindToggle(mPanModeButton, mPanLayer);

      mDefaultLayer.bindToggle(mScrollLeftButton, mTrackBank.scrollBackwardsAction(),
         mTrackBank.canScrollBackwards());
      mDefaultLayer.bindToggle(mScrollRightButton, mTrackBank.scrollForwardsAction(),
         mTrackBank.canScrollForwards());

      mDefaultLayer.bindToggle(mChannelButton, mChannelLayer);
      mDefaultLayer.bindToggle(mZoomButton, mZoomLayer);
      mDefaultLayer.bindToggle(mScrollButton, mScrollLayer);
      mDefaultLayer.bindToggle(mBankButton, mBankLayer);
      mDefaultLayer.bindToggle(mScrollButton, mSectionLayer);
      mDefaultLayer.bindToggle(mMarkerButton, mMarkerLayer);

      mDefaultLayer.bindPressed(mAutomationOffButton, mTransport.isArrangerAutomationWriteEnabled());
      mDefaultLayer.bind(() -> {
         final boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
         return isEnabled ? ARM_HIGH : DIM_WHITE;
      }, mAutomationOffButton);

      mDefaultLayer.bindPressed(mAutomationWriteButton, () -> mTransport.automationWriteMode().set("write"));
      mDefaultLayer.bind(() -> {
         if (mTransport.automationWriteMode().get().equals("write"))
         {
            final boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
            return isEnabled ? ARM_HIGH : DIM_WHITE;
         }

         return null;
      }, mAutomationWriteButton);

      mDefaultLayer.bindPressed(mAutomationTouchButton, () -> mTransport.automationWriteMode().set("touch"));
      mDefaultLayer.bind(() -> {
         if (mTransport.automationWriteMode().get().equals("touch"))
         {
            final boolean isEnabled = mTransport.isArrangerAutomationWriteEnabled().get();
            return isEnabled ? ARM_HIGH : DIM_WHITE;
         }

         return null;
      }, mAutomationTouchButton);

      for (int channelIndex = 0; channelIndex < mChannelCount; channelIndex++)
      {
         final Track track = mTrackBank.getItemAt(channelIndex);
         bindTrack(mDefaultLayer, channelIndex, track);
      }
   }

   private void initChannelLayer()
   {
      mChannelLayer.bind(mTransportEncoder, mCursorTrack);
   }

   private void initSendsLayer()
   {
      mSendsLayer.bind(mDisplayEncoder, mTrackBank.getItemAt(0).sendBank());
   }

   private void initBankLayer()
   {
      mBankLayer.bind(mTransportEncoder, mTrackBank);
      mBankLayer.bindPressed(mTransportEncoder,
         () -> mTrackBank.scrollIntoView(mCursorTrack.position().get()));
   }

   private void initScrollLayer()
   {
      mScrollLayer.bindPressed(mTransportEncoder, mApplication.zoomToFitAction());
   }

   private void initMarkerLayer()
   {
      mMarkerLayer.bindToggle(mScrollLeftButton, mCueMarkerBank.scrollPageBackwardsAction(),
         mCueMarkerBank.canScrollBackwards());
      mMarkerLayer.bindToggle(mScrollRightButton, mCueMarkerBank.scrollPageForwardsAction(),
         mCueMarkerBank.canScrollForwards());

      mMarkerLayer.bindPressed(mTransportEncoder, () -> mCueMarkerBank.getItemAt(0).launch(true));
      mMarkerLayer.bind(mTransportEncoder, mCueMarkerBank);
   }

   private void initDeviceLayer()
   {
      for (int c = 0; c < mChannelCount; c++)
      {
         final int channelIndex = c;
         final int indexInGroup = c & 0x7;

         final RemoteControl parameter = c >= 8 ? mRemoteControls2.getParameter(indexInGroup)
            : mRemoteControls.getParameter(indexInGroup);

         parameter.name().markInterested();
         parameter.displayedValue().markInterested();

         final Channel channel = mChannels[c];

         mDeviceLayer.bind(channel.motorFader, parameter);

         mDeviceLayer.bindPressed(channel.mute, parameter::reset);

         mDeviceLayer.bindPressed(channel.select, () -> getPageIndex().set(channelIndex));
         mDeviceLayer.bind(() -> {
            if (channelIndex < mRemoteControls.pageNames().get().length)
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

            return null;
         }, channel.select);
      }
   }

   private void initZoomLayer()
   {
      mZoomLayer.bind(mTransportEncoder, mApplication.zoomInAction(), mApplication.zoomOutAction());
      mZoomLayer.bindPressed(mTransportEncoder, mApplication.zoomToSelectionAction());
   }

   private void initMasterLayer()
   {
      bindTrack(mMasterLayer, mChannelCount - 1, mMasterTrack);
   }

   private void initSectionLayer()
   {

   }

   private void onMidi(final ShortMidiMessage data)
   {
      getHost().println(data.toString());
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();

      for (int index = 0; index < mChannelCount; index++)
      {
         mChannels[index].display.updateHardware();
      }
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

      for (int c = 0; c < mChannelCount; c++)
      {
         final int channelIndex = c;
         final int indexInGroup = c & 0x7;

         final RemoteControl parameter = c >= 8 ? mRemoteControls2.getParameter(indexInGroup)
            : mRemoteControls.getParameter(indexInGroup);

         parameter.name().markInterested();
         parameter.displayedValue().markInterested();

         final Channel channel = mChannels[c];

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
                     final int rightIndex = mRemoteControls2.selectedPageIndex().get();
                     return rightIndex < pageNames.length ? pageNames[rightIndex] : "";
                  }

                  if (channelIndex < pageNames.length)
                  {
                     return pageNames[channelIndex];
                  }
               }

               if (line == 2)
                  return parameter.name().getLimited(7);
               if (line == 3)
                  return parameter.displayedValue().getLimited(7);

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

   private Channel[] mChannels;

   class Channel
   {

      public Channel(final int index)
      {
         super();

         final int channelNumber = index + 1;

         solo = createToggleButton("solo" + channelNumber, SOLOD_IDS[index]);
         mute = createToggleButton("mute" + channelNumber, (index >= 8 ? 0x70 : 0x10) + index);
         select = createRGBButton("select" + channelNumber, SELECT_IDS[index]);
         motorFader = createMotorFader(index);
         display = new Display(index, mSysexHeader, PresonusFaderPort.this);

         motorFader.isBeingTouched().markInterested();
      }

      final HardwareButton solo;

      final HardwareButton mute;

      final HardwareButton select;

      final HardwareSlider motorFader;

      final Display display;
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

      final Channel channel = mChannels[index];
      final HardwareButton solo = channel.solo;
      final HardwareButton mute = channel.mute;
      final HardwareButton select = channel.select;
      final HardwareSlider motorFader = channel.motorFader;
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

      layer.bindPressed(select, () -> {
         if (mArm)
            track.arm().toggle();
         else
            track.selectInEditor();
      });

      layer.bind((Supplier<Color>)() -> {
         if (track.exists().get())
         {
            if (mArm)
            {
               return track.arm().get() ? ARM_HIGH : ARM_LOW;
            }
            else
            {
               if (isSelected.get())
                  return WHITE;

               return track.color().get();
            }
         }

         return BLACK;
      }, select);

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
      getMidiOutPort(0).sendMidi(0xA0, 0, 0);

      getHost().scheduleTask(this::runningStatusTimer, 1000);
   }

   @Override
   public void exit()
   {
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

   private Layer mDefaultLayer;

   private Layer mTrackLayer;

   private Layer mDeviceLayer;

   private Layer mSendsLayer;

   private Layer mPanLayer;

   private Layer mChannelLayer;

   private Layer mZoomLayer;

   private Layer mScrollLayer;

   private Layer mBankLayer;

   private Layer mMasterLayer;

   private Layer mSectionLayer;

   private Layer mMarkerLayer;

   private static class PanDisplayTarget extends ChannelDisplayTarget
   {
      public PanDisplayTarget(
         final Track track,
         final BooleanValue isSelected,
         final HardwareSlider motorFader)
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
         final Track track,
         final BooleanValue isSelected,
         final HardwareSlider motorFader)
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

   // Hardware

   HardwareSurface mHardwareSurface;

   private HardwareButton mArmButton;

   private MidiIn mMidiIn;

   private final Layers mLayers = new Layers(this);
}
