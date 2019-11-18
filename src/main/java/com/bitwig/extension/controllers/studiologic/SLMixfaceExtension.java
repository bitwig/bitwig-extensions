package com.bitwig.extension.controllers.studiologic;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ClipLauncherSlotOrScene;
import com.bitwig.extension.controller.api.ClipLauncherSlotOrSceneBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.animation.BlinkAnimation;

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
      mCursorTrack = host.createCursorTrack(0, 16);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mMainRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage(8);
      mSlidersRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage("sliders", 8, "envelope");
      mTrackBank.followCursorTrack(mCursorTrack);
      mSceneBank = host.createSceneBank(16);

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

      surface.setPhysicalSize(262, 180);

      for (int i = 0; i < 8; i++)
      {
         final double x = 13 + i * 20;

         final AbsoluteHardwareKnob panKnob = surface.createAbsoluteHardwareKnob("pan" + (i + 1));
         panKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 2 + i));
         final String panLabel = "Pan" + (i + 1);
         panKnob.setLabel(panLabel);
         mPanKnobs[i] = panKnob;

         final HardwareSlider slider = surface.createHardwareSlider("volume" + (i + 1));
         slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 16 + i));
         final String volLabel = "Volume " + (i + 1);
         slider.setLabel(volLabel);
         mVolumeSliders[i] = slider;

         // Create the arm button

         final HardwareButton armButton = createButton("arm" + (i + 1));
         final int armCC = 48 + i;
         armButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, armCC, 127));
         armButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, armCC, 0));
         final String armLabel = "Arm " + (i + 1);
         armButton.setLabel(armLabel);
         final OnOffHardwareLight armBackgroundLight = surface.createOnOffHardwareLight("arm_light" + (i + 1));

         final int j = i;

         armBackgroundLight.isOn().onUpdateHardware(value -> {
            // System.out.println("Updating arm light state " + j + " at time " + System.currentTimeMillis());
            sendCC(15, armCC, value ? 127 : 0);
         });

         armButton.setBackgroundLight(armBackgroundLight);

         mArmButtons[i] = armButton;

         // Create the mute button

         final HardwareButton muteButton = createButton("mute" + (i + 1));
         final int muteCC = 64 + i;
         muteButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, muteCC, 127));
         muteButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, muteCC, 0));
         final String muteLabel = "Mute " + (i + 1);
         muteButton.setLabel(muteLabel);
         final OnOffHardwareLight muteBackgroundLight = surface.createOnOffHardwareLight("mute_light" + (i + 1));

         muteBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, muteCC, value ? 127 : 0);
         });

         muteButton.setBackgroundLight(muteBackgroundLight);

         mMuteButtons[i] = muteButton;

         // Create the solo button

         final HardwareButton soloButton = createButton("solo" + (i + 1));
         final int soloCC = 80 + i;
         soloButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, soloCC, 127));
         soloButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, soloCC, 0));
         final String soloLabel = "Solo " + (i + 1);
         soloButton.setLabel(soloLabel);
         final OnOffHardwareLight soloBackgroundLight = surface.createOnOffHardwareLight("solo_light" + (i + 1));

         soloBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, soloCC, value ? 127 : 0);
         });

         soloButton.setBackgroundLight(soloBackgroundLight);

         mSoloButtons[i] = soloButton;

         // Create the select button

         final HardwareButton selectButton = createButton("select" + (i + 1));
         final int selectCC = 96 + i;
         selectButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, selectCC, 127));
         selectButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, selectCC, 0));
         final String selectLabel = "Select " + (i + 1);
         selectButton.setLabel(selectLabel);
         final OnOffHardwareLight selectBackgroundLight = surface.createOnOffHardwareLight("select_light" + (i + 1));

         selectBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, selectCC, value ? 127 : 0);
         });

         selectButton.setBackgroundLight(selectBackgroundLight);

         mSelectButtons[i] = selectButton;
      }

      mMasterVolumeSlider = surface.createHardwareSlider("master_volume");
      mMasterVolumeSlider.setLabel("Master Volume");
      mMasterVolumeSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 24));

      mPlayButton = createButton("play");
      mPlayButton.setLabel("Play");

      final String playPressedExpression = midiExpressions.createIsCCValueExpression(15, 32, 127) + " || "
         + midiExpressions.createIsCCValueExpression(15, 33, 127);
      mPlayButton.pressedAction().setActionMatcher(midiIn.createActionMatcher(playPressedExpression));
      mPlayButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 45, 0));
      final OnOffHardwareLight playLight = surface.createOnOffHardwareLight("play_light");
      playLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 33, value ? 127 : 0);
      });
      mPlayButton.setBackgroundLight(playLight);

      mRecordButton = createButton("record");
      mRecordButton.setLabel("Record");
      mRecordButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 127));
      mRecordButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 0));
      final OnOffHardwareLight recordLight = surface.createOnOffHardwareLight("record_light");
      recordLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 34, value ? 127 : 0);
      });
      mRecordButton.setBackgroundLight(recordLight);

      mModeButton = createButton("mode");
      mModeButton.setLabel("Mode");
      mModeButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 39, 127));
      final OnOffHardwareLight modeLight = surface.createOnOffHardwareLight("mode_light");
      modeLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 39, value ? 127 : 0);
      });
      mModeButton.setBackgroundLight(modeLight);

      mScrollBankForwardsButton = createButton("scroll_forwards");
      mScrollBankForwardsButton.setLabel("Scroll Forwards");
      mScrollBankForwardsButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 38, 127));
      mScrollBankForwardsButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 38, 0));

      mScrollBankBackwardsButton = createButton("scroll_backwards");
      mScrollBankBackwardsButton.setLabel("Scroll Backwards");
      mScrollBankBackwardsButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 37, 127));
      mScrollBankBackwardsButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 37, 0));

      mFastForwardButton = createButton("fast_forward");
      mFastForwardButton.setLabel("Fast Forward");
      mFastForwardButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 36, 127));
      mFastForwardButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 36, 0));

      mRewindButton = createButton("rewind");
      mRewindButton.setLabel("Rewind");
      mRewindButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 35, 127));
      mRewindButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 35, 0));

      mNavigationDial = surface.createRelativeHardwareKnob("dial");

      final double stepSize = 1 / 16.0;

      final RelativeHardwareValueMatcher stepDownMatcher = midiIn
         .createRelativeValueMatcher(midiExpressions.createIsCCValueExpression(15, 44, 127), -stepSize);
      final RelativeHardwareValueMatcher stepUpMatcher = midiIn
         .createRelativeValueMatcher(midiExpressions.createIsCCValueExpression(15, 43, 127), stepSize);
      final RelativeHardwareValueMatcher valueMatcher = host
         .createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);

      mNavigationDial.setAdjustValueMatcher(valueMatcher);
      mNavigationDial.setStepSize(stepSize);

      initHardwareLayout();
   }

   private void initHardwareLayout()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("pan1").setBounds(9.0, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume1").setBounds(9.0, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm1").setBounds(9.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute1").setBounds(9.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo1").setBounds(9.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select1").setBounds(9.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan2").setBounds(29.0, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume2").setBounds(29.0, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm2").setBounds(29.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute2").setBounds(29.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo2").setBounds(29.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select2").setBounds(29.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan3").setBounds(49.25, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume3").setBounds(49.25, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm3").setBounds(49.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute3").setBounds(49.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo3").setBounds(49.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select3").setBounds(49.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan4").setBounds(69.25, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume4").setBounds(69.25, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm4").setBounds(69.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute4").setBounds(69.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo4").setBounds(69.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select4").setBounds(69.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan5").setBounds(89.5, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume5").setBounds(89.25, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm5").setBounds(89.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute5").setBounds(89.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo5").setBounds(89.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select5").setBounds(89.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan6").setBounds(109.5, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume6").setBounds(109.5, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm6").setBounds(109.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute6").setBounds(109.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo6").setBounds(109.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select6").setBounds(109.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan7").setBounds(129.75, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume7").setBounds(129.5, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm7").setBounds(129.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute7").setBounds(129.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo7").setBounds(129.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select7").setBounds(129.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("pan8").setBounds(149.75, 20.75, 13.0, 13.0);
      surface.hardwareElementWithId("volume8").setBounds(149.75, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("arm8").setBounds(149.0, 108.25, 13.0, 13.0);
      surface.hardwareElementWithId("mute8").setBounds(149.0, 124.25, 13.0, 13.0);
      surface.hardwareElementWithId("solo8").setBounds(149.0, 140.25, 13.0, 13.0);
      surface.hardwareElementWithId("select8").setBounds(149.0, 156.25, 13.0, 13.0);
      surface.hardwareElementWithId("master_volume").setBounds(169.75, 46.25, 13.0, 50.0);
      surface.hardwareElementWithId("play").setBounds(226.0, 91.0, 12.0, 8.0);
      surface.hardwareElementWithId("record").setBounds(241.0, 91.0, 12.0, 8.0);
      surface.hardwareElementWithId("mode").setBounds(241.0, 77.0, 12.0, 8.0);
      surface.hardwareElementWithId("scroll_forwards").setBounds(241.0, 57.25, 12.0, 8.0);
      surface.hardwareElementWithId("scroll_backwards").setBounds(225.0, 57.0, 12.0, 8.0);
      surface.hardwareElementWithId("fast_forward").setBounds(210.5, 91.0, 12.0, 8.0);
      surface.hardwareElementWithId("rewind").setBounds(195.0, 91.0, 12.0, 8.0);
      surface.hardwareElementWithId("dial").setBounds(239.0, 28.5, 18.25, 18.0);


   }

   private HardwareButton createButton(final String id)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton(id);
      button.setLabelColor(Color.blackColor());

      return button;
   }

   private void defineLayers()
   {
      mCommonLayer = createCommonLayer();
      mTrackLayer = createTrackLayer();
      mDeviceLayer = createDeviceLayer();
      mDebugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
      // mDebugLayer.activate();
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

         layer.bindToggle(armButton, track.arm());

         final HardwareButton muteButton = mMuteButtons[i];

         layer.bindToggle(muteButton, track.mute());

         final HardwareButton soloButton = mSoloButtons[i];

         layer.bindToggle(soloButton, track.solo());

         final HardwareButton selectButton = mSelectButtons[i];

         layer.bindPressed(selectButton, () -> mCursorTrack.selectChannel(track));

         final BooleanValue isSelected = track.createEqualsValue(mCursorTrack);

         layer.bind(isSelected, selectButton);
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

      }

      final ClipLauncherSlotBank slots = mCursorTrack.clipLauncherSlotBank();
      ClipLauncherSlotOrSceneBank<? extends ClipLauncherSlotOrScene> slotsOrScenes = slots;

      int sceneIndex = 0;

      for (int mode = 0; mode < 4; mode++)
      {
         if (mode == 2)
         {
            sceneIndex = 0;
            slotsOrScenes = mSceneBank;
         }

         final HardwareButton[] buttons = getLowerButtons(mode);

         for (int i = 0; i < 8; i++)
         {
            final HardwareButton button = buttons[i];
            final ClipLauncherSlotOrScene slotOrScene = slotsOrScenes.getItemAt(sceneIndex);

            final HardwareActionBindable launchAction = slotOrScene.launchAction();

            layer.bindPressed(button, launchAction);

            final ClipLauncherSlot slot = slots.getItemAt(sceneIndex);

            slot.isPlaying().markInterested();

            layer.bind(new BlinkAnimation(this, () -> slot.isPlaying().get() && mTransport.isPlaying().get(),
               slot.hasContent(), 0.1), button);

            sceneIndex++;
         }
      }

      layer.bind(mNavigationDial, mCursorDevice);

      return layer;
   }

   private HardwareButton[] getLowerButtons(final int mode)
   {
      switch (mode)
      {
      case 0:
         return mArmButtons;
      case 1:
         return mMuteButtons;
      case 2:
         return mSoloButtons;
      case 3:
         return mSelectButtons;
      }

      throw new IllegalStateException();
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

   private SceneBank mSceneBank;

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

   private Layer mCommonLayer, mTrackLayer, mDeviceLayer, mDebugLayer;
}
