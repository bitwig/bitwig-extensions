package com.bitwig.extension.controllers.studiologic;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.TriggerAction;
import com.bitwig.extensions.framework2.Layer;
import com.bitwig.extensions.framework2.Layers;

public class SLMixfaceExtension extends ControllerExtension
{
   protected SLMixfaceExtension(final SLMixfaceExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();
      mTransport.isPlaying().markInterested();
      mTrackBank = host.createTrackBank(8, 0, 0);
      mMasterTrack = host.createMasterTrack(0);
      mCursorTrack = host.createCursorTrack(0, 8);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mMainRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage(8);
      mSlidersRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage("sliders", 8, "envelope");
      mTrackBank.followCursorTrack(mCursorTrack);

      final MidiIn midiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
      midiIn.setSysexCallback((final String data) -> onSysex0(data));

      defineHardwareControls(host, midiIn);

      defineLayers();

      mTrackLayer.setIsActive(true);
   }

   private void defineHardwareControls(final ControllerHost host, final MidiIn midiIn)
   {
      final MidiExpressions midiExpressions = host.midiExpressions();
      final HardwareSurface surface = host.createHardwareSurface();
      mHardwareSurface = surface;

      surface.setPhysicalSize(262, 130);

      for (int i = 0; i < 8; i++)
      {
         final AbsoluteHardwareKnob panKnob = surface.createAbsoluteHardwareKnob();
         panKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 2 + i));
         final String panLabel = "Pan" + (i + 1);
         panKnob.setLabel(panLabel);
         // panKnob.value().addValueObserver(value -> host.println(panLabel + ": " + value));
         mPanKnobs[i] = panKnob;

         final HardwareSlider slider = surface.createHardwareSlider();
         slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 16 + i));
         final String volLabel = "Volume " + (i + 1);
         slider.setLabel(panLabel);
         // slider.value().addValueObserver(value -> host.println(volLabel + ": " + value));
         mVolumeSliders[i] = slider;

         // Create the arm button

         final HardwareButton armButton = surface.createHardwareButton();
         final int armCC = 48 + i;
         armButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, armCC, 127));
         armButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, armCC, 0));
         final String armLabel = "Arm " + (i + 1);
         armButton.setLabel(armLabel);
         // armButton.pressedAction().onAction(() -> host.println(armLabel + " presesd"));
         // armButton.releasedAction().onAction(() -> host.println(armLabel + " released"));
         final OnOffHardwareLight armBackgroundLight = surface.createOnOffHardwareLight();

         armBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, armCC, value ? 127 : 0);
         });

         armButton.setBackgroundLight(armBackgroundLight);

         mArmButtons[i] = armButton;

         // Create the mute button

         final HardwareButton muteButton = surface.createHardwareButton();
         final int muteCC = 64 + i;
         muteButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, muteCC, 127));
         muteButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, muteCC, 0));
         final String muteLabel = "Mute " + (i + 1);
         muteButton.setLabel(muteLabel);
         // armButton.pressedAction().onAction(() -> host.println(armLabel + " presesd"));
         // armButton.releasedAction().onAction(() -> host.println(armLabel + " released"));
         final OnOffHardwareLight muteBackgroundLight = surface.createOnOffHardwareLight();

         muteBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, muteCC, value ? 127 : 0);
         });

         muteButton.setBackgroundLight(muteBackgroundLight);

         mMuteButtons[i] = muteButton;

         // Create the solo button

         final HardwareButton soloButton = surface.createHardwareButton();
         final int soloCC = 80 + i;
         soloButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, soloCC, 127));
         soloButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, soloCC, 0));
         final String soloLabel = "Solo " + (i + 1);
         soloButton.setLabel(soloLabel);
         // armButton.pressedAction().onAction(() -> host.println(armLabel + " presesd"));
         // armButton.releasedAction().onAction(() -> host.println(armLabel + " released"));
         final OnOffHardwareLight soloBackgroundLight = surface.createOnOffHardwareLight();

         soloBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, soloCC, value ? 127 : 0);
         });

         soloButton.setBackgroundLight(soloBackgroundLight);

         mSoloButtons[i] = soloButton;

         // Create the select button

         final HardwareButton selectButton = surface.createHardwareButton();
         final int selectCC = 96 + i;
         selectButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, selectCC, 127));
         selectButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, selectCC, 0));
         final String selectLabel = "Select " + (i + 1);
         selectButton.setLabel(selectLabel);
         // armButton.pressedAction().onAction(() -> host.println(armLabel + " presesd"));
         // armButton.releasedAction().onAction(() -> host.println(armLabel + " released"));
         final OnOffHardwareLight selectBackgroundLight = surface.createOnOffHardwareLight();

         selectBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, selectCC, value ? 127 : 0);
         });

         selectButton.setBackgroundLight(selectBackgroundLight);

         mSelectButtons[i] = selectButton;
      }

      mMasterVolumeSlider = surface.createHardwareSlider();
      mMasterVolumeSlider.setLabel("Master Volume");
      mMasterVolumeSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 24));
      // mMasterVolumeSlider.value().addValueObserver(value -> host.println("Master Volume " + value));

      mPlayButton = surface.createHardwareButton();
      mPlayButton.setLabel("Play");

      final String playPressedExpression = midiExpressions.createIsCCValueExpression(15, 32, 127) + " || "
         + midiExpressions.createIsCCValueExpression(15, 33, 127);
      mPlayButton.pressedAction().setActionMatcher(midiIn.createActionMatcher(playPressedExpression));
      mPlayButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 45, 0));
      // mPlayButton.pressedAction().onAction(() -> host.println("Play pressed"));
      // mPlayButton.releasedAction().onAction(() -> host.println("Play released"));
      final OnOffHardwareLight playLight = surface.createOnOffHardwareLight();
      playLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 33, value ? 127 : 0);
      });
      mPlayButton.setBackgroundLight(playLight);

      mRecordButton = surface.createHardwareButton();
      mRecordButton.setLabel("Record");
      mRecordButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 127));
      mRecordButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 0));
      // mRecordButton.pressedAction().onAction(() -> host.println("Rec pressed"));
      // mRecordButton.releasedAction().onAction(() -> host.println("Rec released"));
      final OnOffHardwareLight recordLight = surface.createOnOffHardwareLight();
      recordLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 34, value ? 127 : 0);
      });
      mRecordButton.setBackgroundLight(recordLight);

      mModeButton = surface.createHardwareButton();
      mModeButton.setLabel("Mode");
      mModeButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 39, 127));
      // mModeButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 0));
      // mModeButton.pressedAction().onAction(() -> host.println("Rec pressed"));
      // mModeButton.releasedAction().onAction(() -> host.println("Rec released"));
      final OnOffHardwareLight modeLight = surface.createOnOffHardwareLight();
      modeLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 39, value ? 127 : 0);
      });
      mModeButton.setBackgroundLight(modeLight);

      mScrollBankForwardsButton = surface.createHardwareButton();
      mScrollBankForwardsButton.setLabel("Scroll Forwards");
      mScrollBankForwardsButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 38, 127));
      mScrollBankForwardsButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 38, 0));

      mScrollBankBackwardsButton = surface.createHardwareButton();
      mScrollBankBackwardsButton.setLabel("Scroll Backwards");
      mScrollBankBackwardsButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 37, 127));
      mScrollBankBackwardsButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 37, 0));

      mFastForwardButton = surface.createHardwareButton();
      mFastForwardButton.setLabel("Fast Forward");
      mFastForwardButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 36, 127));
      mFastForwardButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 36, 0));

      mRewindButton = surface.createHardwareButton();
      mRewindButton.setLabel("Rewind");
      mRewindButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 35, 127));
      mRewindButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 35, 0));

      // mNextButton = surface.createHardwareButton();
      // mNextButton.setLabel("Next");
      // mNextButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 43, 127));
      //
      // mPreviousButton = surface.createHardwareButton();
      // mPreviousButton.setLabel("Prev");
      // mPreviousButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 44, 127));

      mNavigationDial = surface.createRelativeHardwareKnob();

      final double stepSize = 1 / 16.0;

      final RelativeHardwareValueMatcher stepDownMatcher = midiIn
         .createRelativeValueMatcher(midiExpressions.createIsCCValueExpression(15, 44, 127), -stepSize);
      final RelativeHardwareValueMatcher stepUpMatcher = midiIn
         .createRelativeValueMatcher(midiExpressions.createIsCCValueExpression(15, 43, 127), stepSize);
      final RelativeHardwareValueMatcher valueMatcher = host
         .createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);

      mNavigationDial.setAdjustValueMatcher(valueMatcher);
      mNavigationDial.setStepSize(stepSize);
   }

   private void defineLayers()
   {
      mCommonLayer = createCommonLayer();
      mTrackLayer = createTrackLayer();
      mDeviceLayer = createDeviceLayer();
   }

   private void toggleDeviceLayer()
   {
      final boolean isDeviceLayerActive = mDeviceLayer.isActive();

      mTrackLayer.setIsActive(isDeviceLayerActive);
      mDeviceLayer.setIsActive(!isDeviceLayerActive);
   }

   private Layer createCommonLayer()
   {
      final Layer layer = mLayers.addLayer("Common");

      layer.bindPressed(mModeButton, this::toggleDeviceLayer);
      layer.bind((BooleanSupplier)(() -> mDeviceLayer.isActive()), mModeButton);

      layer.bindPressed(mPlayButton, mTransport.playAction());
      layer.bind(mTransport.isPlaying(), mPlayButton);

      layer.bindPressed(mRecordButton, mTransport.recordAction());
      layer.bind(mTransport.isArrangerRecordEnabled(), mRecordButton);

      layer.bindPressed(mFastForwardButton, mTransport.fastForwardAction());
      layer.bindPressed(mRewindButton, mTransport.rewindAction());

      layer.bind(mMasterVolumeSlider, mMasterTrack.volume());

      layer.setIsActive(true);

      return layer;
   }

   private Layer createTrackLayer()
   {
      final Layer layer = mLayers.addLayer("Track");

      layer.bindPressed(mScrollBankForwardsButton, mTrackBank.scrollPageForwardsAction());
      layer.bindPressed(mScrollBankBackwardsButton, mTrackBank.scrollPageBackwardsAction());

      for (int i = 0; i < 8; i++)
      {
         final Track track = mTrackBank.getItemAt(i);

         layer.bind(mVolumeSliders[i], track.volume());
         layer.bind(mPanKnobs[i], track.pan());

         final HardwareButton armButton = mArmButtons[i];

         layer.bindPressed(armButton, track.arm());
         layer.bind(track.arm(), armButton);

         final HardwareButton muteButton = mMuteButtons[i];

         layer.bindPressed(muteButton, track.mute());
         layer.bind(track.mute(), muteButton);

         final HardwareButton soloButton = mSoloButtons[i];

         layer.bindPressed(soloButton, track.solo());
         layer.bind(track.solo(), soloButton);

         final HardwareButton selectButton = mSelectButtons[i];

         layer.bindPressed(selectButton, () -> mCursorTrack.selectChannel(track));
      }

      layer.bind(mNavigationDial, mCursorTrack);

      layer.setIsActive(true);

      return layer;
   }

   private Layer createDeviceLayer()
   {
      final Layer layer = mLayers.addLayer("Device");

      layer.bindPressed(mScrollBankForwardsButton, mMainRemoteControlsPage.selectNextAction());
      layer.bindPressed(mScrollBankBackwardsButton, mMainRemoteControlsPage.selectPreviousAction());

      for (int i = 0; i < 8; i++)
      {
         layer.bind(mPanKnobs[i], mMainRemoteControlsPage.getParameter(i));
         layer.bind(mVolumeSliders[i], mSlidersRemoteControlsPage.getParameter(i));

         final HardwareButton armButton = mArmButtons[i];

         final ClipLauncherSlotBank slots = mCursorTrack.clipLauncherSlotBank();
         final ClipLauncherSlot slot = slots.getItemAt(i);

         final TriggerAction launchAction = slot.launchAction();

         layer.bindPressed(armButton, launchAction);
         layer.bind(slot.hasContent(), armButton);

         final HardwareButton muteButton = mMuteButtons[i];

         layer.bindPressed(muteButton, launchAction);
         layer.bind(slot.isPlaying(), muteButton);

         final HardwareButton soloButton = mSoloButtons[i];

         layer.bindPressed(soloButton, launchAction);
         layer.bind(slot.hasContent(), soloButton);
      }

      layer.bind(mNavigationDial, mCursorDevice);

      return layer;
   }

   private void sendMidi(final int status, final int data1, final int data2)
   {
      mMidiOut.sendMidi(status, data1, data2);
   }

   private void sendCC(final int channel, final int cc, final int value)
   {
      sendMidi(0xb0 | channel, cc, value);
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(final ShortMidiMessage msg)
   {
      // TODO: Implement your MIDI input handling code here.

      getHost().println(msg.toString());
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data)
   {
      // MMC Transport Controls:
      if (data.equals("f07f7f0605f7"))
         mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
         mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
         mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
         mTransport.play();
      else if (data.equals("f07f7f0606f7"))
         mTransport.record();
   }

   private Transport mTransport;

   private TrackBank mTrackBank;

   private Track mMasterTrack;

   private MidiOut mMidiOut;

   private CursorTrack mCursorTrack;

   private CursorDevice mCursorDevice;

   private CursorRemoteControlsPage mMainRemoteControlsPage, mSlidersRemoteControlsPage;

   private HardwareSurface mHardwareSurface;

   private final AbsoluteHardwareKnob[] mPanKnobs = new AbsoluteHardwareKnob[8];

   private final HardwareSlider[] mVolumeSliders = new HardwareSlider[8];

   private HardwareSlider mMasterVolumeSlider;

   private final HardwareButton[] mArmButtons = new HardwareButton[8];

   private final HardwareButton[] mMuteButtons = new HardwareButton[8];

   private final HardwareButton[] mSoloButtons = new HardwareButton[8];

   private final HardwareButton[] mSelectButtons = new HardwareButton[8];

   private HardwareButton mPlayButton, mRecordButton, mFastForwardButton, mRewindButton, mModeButton,
      mScrollBankForwardsButton, mScrollBankBackwardsButton;

   private RelativeHardwareKnob mNavigationDial;

   private final Layers mLayers = new Layers(this);

   private Layer mCommonLayer, mTrackLayer, mDeviceLayer;
}
