package com.bitwig.extensions.controllers.befaco;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.UserControlBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayerGroup;
import com.bitwig.extensions.framework.Layers;

class VCMC extends ControllerExtension
{
   private static final String DEVICE_MODE_NAME = "Device";

   private static final String MIXER_MODE_NAME = "Mixer";

   public VCMC(final VCMCDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.createNoteInput("All Channels");

      // Define the hardware

      mHardwareSurface = host.createHardwareSurface();
      mHardwareSurface.setPhysicalSize(100, 130);

      for (int i = 0; i < 8; i++)
      {
         mSliders[i] = createSlider(i);
         mCVInputs[i] = createCVInput(i);
      }

      mAuxA = createAuxInput(true);
      mAuxB = createAuxInput(false);

      initHardwareLayout();

      // Create objects to control

      final DocumentState settings = host.getDocumentState();

      mModeSetting = settings.getEnumSetting("Mode", "Mode",
         new String[] { DEVICE_MODE_NAME, MIXER_MODE_NAME }, DEVICE_MODE_NAME);

      mModeSetting.addValueObserver(mode -> updateActiveLayer());

      final CursorTrack cursorTrack = host.createCursorTrack(3, 0);
      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      mRemoteControls = cursorDevice.createCursorRemoteControlsPage(8);

      mTrackBank = host.createTrackBank(8, 0, 0);

      mUserControlBank = host.createUserControls(8 + 2);

      // Create layers and bindings

      createBaseLayer();
      createDeviceLayer();
      createMixerLayer();

      new LayerGroup(mDeviceLayer, mMixerLayer);

      updateActiveLayer();
   }

   private void updateActiveLayer()
   {
      final String modeName = mModeSetting.get();

      if (DEVICE_MODE_NAME.equals(modeName))
         mDeviceLayer.activate();
      else if (MIXER_MODE_NAME.equals(modeName))
         mMixerLayer.activate();
   }

   private void createDeviceLayer()
   {
      final Layer layer = new Layer(mLayers, "Device");

      for (int i = 0; i < 8; i++)
      {
         layer.bind(mSliders[i], mRemoteControls.getParameter(i));
      }

      mDeviceLayer = layer;
   }

   private void createBaseLayer()
   {
      final Layer layer = new Layer(mLayers, "Base");

      for (int i = 0; i < 8; i++)
      {
         layer.bind(mCVInputs[i], mUserControlBank.getControl(i));
      }

      layer.bind(mAuxA, mUserControlBank.getControl(8));
      layer.bind(mAuxB, mUserControlBank.getControl(9));

      layer.activate();
   }

   private void createMixerLayer()
   {
      final Layer layer = new Layer(mLayers, "Device");

      for (int i = 0; i < 8; i++)
      {
         layer.bind(mSliders[i], mTrackBank.getItemAt(i).volume());
      }

      mMixerLayer = layer;
   }

   private AbsoluteHardwareKnob createCVInput(final int index)
   {
      final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("CV" + (index + 1));

      final MidiIn midiIn = getHost().getMidiInPort(0);

      knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 14 + index));

      return knob;
   }

   private AbsoluteHardwareKnob createAuxInput(final boolean isA)
   {
      final AbsoluteHardwareKnob knob = mHardwareSurface
         .createAbsoluteHardwareKnob("Aux" + (isA ? "A" : "B"));

      final MidiIn midiIn = getHost().getMidiInPort(0);

      knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, isA ? 32 : 33));

      return knob;
   }

   private HardwareSlider createSlider(final int index)
   {
      final HardwareSlider slider = mHardwareSurface.createHardwareSlider("Slider" + (index + 1));

      final MidiIn midiIn = getHost().getMidiInPort(0);

      slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 23 + index));
      slider.setLabel(String.valueOf(index + 1));

      return slider;
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
   }

   private void initHardwareLayout()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("Slider1").setBounds(4.0, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider2").setBounds(16.25, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider3").setBounds(28.25, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider4").setBounds(40.5, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider5").setBounds(52.5, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider6").setBounds(64.75, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider7").setBounds(77.0, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("Slider8").setBounds(89.0, 64.0, 7.25, 36.0);
      surface.hardwareElementWithId("CV1").setBounds(2.25, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV2").setBounds(14.5, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV3").setBounds(26.75, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV4").setBounds(39.0, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV5").setBounds(51.25, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV6").setBounds(63.5, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV7").setBounds(75.75, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("CV8").setBounds(88.0, 43.75, 10.0, 10.0);
      surface.hardwareElementWithId("AuxA").setBounds(45.0, 9.75, 10.0, 10.0);
      surface.hardwareElementWithId("AuxB").setBounds(45.25, 23.75, 10.0, 10.0);

   }

   private HardwareSurface mHardwareSurface;

   private final HardwareSlider[] mSliders = new HardwareSlider[8];

   private final AbsoluteHardwareKnob[] mCVInputs = new AbsoluteHardwareKnob[8];

   private AbsoluteHardwareKnob mAuxA, mAuxB;

   private CursorRemoteControlsPage mRemoteControls;

   private TrackBank mTrackBank;

   private UserControlBank mUserControlBank;

   private final Layers mLayers = new Layers(this);

   private Layer mDeviceLayer, mMixerLayer;

   private SettableEnumValue mModeSetting;
}
