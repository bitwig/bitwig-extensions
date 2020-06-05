package com.bitwig.extensions.controllers.kenton;

import java.util.function.DoubleConsumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayerGroup;
import com.bitwig.extensions.framework.Layers;

public class KillaMixMiniExtension extends ControllerExtension
{
   private static final String DEVICE_MODE_NAME     = "Device";
   private static final String MIXER_MODE_NAME      = "Mixer";

   private static final String SHOW_MODULATION      = "Show modulation";
   private static final String SHOW_PARAMETERS      = "Show parameter only";

   private static final int NUM_KNOBS_AND_BUTTONS   = 9;
   private static final int NUM_DEVICE_KNOBS        = 8;
   private static final int KNOB_CC_BASE            = 1;
   private static final int BUTTON_CC_BASE          = 10;

   private static final int MIXER_MODE_MASTER_KNOB  = 8;
   private static final int DEVICE_MODE_VOLUME_KNOB = 8;

   private static final int MODE_SWITCH_BUTTON      = 8;
   private static final int PREV_TRACK_BUTTON       = 0;
   private static final int NEXT_TRACK_BUTTON       = 1;
   private static final int PREV_DEVICE_BUTTON      = 2;
   private static final int NEXT_DEVICE_BUTTON      = 3;
   private static final int PREV_PAGE_BUTTON        = 4;
   private static final int NEXT_PAGE_BUTTON        = 5;

   protected KillaMixMiniExtension(final KillaMixMiniExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      initHardwareSurface();

      updateActiveLayer();
   }

   @Override
   public void exit()
   { }

   @Override
   public void flush()
   {
      updateDeviceControlRings(mDeviceModeDisplaySetting.get().equals(SHOW_MODULATION));
      updateDeviceButtonLEDs();
   }


   private void initHardwareSurface()
   {
      final ControllerHost host = getHost();

      mMidiOut = host.getMidiOutPort(0);

      mHWSurface = host.createHardwareSurface();
      mHWSurface.setPhysicalSize(320, 64);

      for(int i=0; i < NUM_KNOBS_AND_BUTTONS; i++)
      {
         createKnob(i);
         createButton(i);
         createLED(i);
      }

      createJoystick();

      initHardwareLayout(mHWSurface);

      /////////////////////////////////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////

      final DocumentState settings = host.getDocumentState();

      mCursorTrack    = host.createCursorTrack(3, 0);
      mCursorDevice   = mCursorTrack.createCursorDevice("main", "Main", 0, CursorDeviceFollowMode.FOLLOW_SELECTION);
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      mTrackBank      = host.createTrackBank(8, 0, 0);
      mMasterTrack    = host.createMasterTrack(0);
      mTrackBank.followCursorTrack(mCursorTrack);

      createDeviceLayer();
      createMixerLayer();

      // mode switching
      mModeSetting = settings.getEnumSetting("Mode", "Mode",
         new String[] { DEVICE_MODE_NAME, MIXER_MODE_NAME }, DEVICE_MODE_NAME);

      mModeSetting.addValueObserver(mode -> updateActiveLayer());

      mDeviceModeDisplaySetting = settings.getEnumSetting("Show Modulation", "Mode",
         new String[] { SHOW_PARAMETERS, SHOW_MODULATION }, SHOW_PARAMETERS);

      // since the buttons-LEDs toggle on/off by the hardware itself, we must ensure that
      // we flush/resend the current state to the buttons again, after they have been pressed
      for (int i = 0; i < NUM_DEVICE_KNOBS; i++)
      {
         mButtons[i].isPressed().addValueObserver((boolean b) -> {
            getHost().requestFlush();
         });
      }

      new LayerGroup(mDeviceLayer, mMixerLayer);
   }

   private void initHardwareLayout(final HardwareSurface surface)
   {
      surface.hardwareElementWithId("Joystick X").setBounds(13.0, 30, 15.0, 15.0);
      surface.hardwareElementWithId("Joystick Y").setBounds(13.0, 45, 15.0, 15.0);

      surface.hardwareElementWithId("Knob 1").setBounds(40, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 2").setBounds(70, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 3").setBounds(100, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 4").setBounds(130, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 5").setBounds(160, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 6").setBounds(190, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 7").setBounds(220, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 8").setBounds(250, 13.0, 15.0, 15.0);
      surface.hardwareElementWithId("Knob 9").setBounds(280, 13.0, 15.0, 15.0);

      surface.hardwareElementWithId("Button 1").setBounds(40, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 2").setBounds(70, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 3").setBounds(100, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 4").setBounds(130, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 5").setBounds(160,35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 6").setBounds(190,35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 7").setBounds(220, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 8").setBounds(250,35.0, 10.0, 10.0);
      surface.hardwareElementWithId("Button 9").setBounds(280, 35.0, 10.0, 10.0);

      surface.hardwareElementWithId("LED 1").setBounds(40, 52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 2").setBounds(70, 52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 3").setBounds(100,52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 4").setBounds(130, 52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 5").setBounds(160,52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 6").setBounds(190,52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 7").setBounds(220, 52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 8").setBounds(250, 52.0, 10.0, 10.0);
      surface.hardwareElementWithId("LED 9").setBounds(280,52.0, 10.0, 10.0);
   }

   private void updateActiveLayer()
   {
      final String modeName = mModeSetting.get();

      if (DEVICE_MODE_NAME.equals(modeName))
      {
         mLEDs[MODE_SWITCH_BUTTON].isOn().setValue(false);
         mDeviceLayer.activate();
      }
      else if (MIXER_MODE_NAME.equals(modeName))
      {
         mLEDs[MODE_SWITCH_BUTTON].isOn().setValue(true);
         mMixerLayer.activate();
      }
   }

   private void createDeviceLayer()
   {
      final Layer layer = new Layer(mLayers, "Device");

      // device knobs
      for (int i = 0; i < NUM_DEVICE_KNOBS; i++)
      {
         final RelativeHardwareKnob knob   = mKnobs[i];
         final RemoteControl        remote = mRemoteControls.getParameter(i);

         remote.setIndication(true);
         remote.markInterested();

         layer.bind(knob, remote);
      }

      // track volume knob
      final Parameter volumeParameter = mCursorTrack.volume();
      volumeParameter.setIndication(true);
      volumeParameter.markInterested();
      layer.bind(mKnobs[DEVICE_MODE_VOLUME_KNOB], volumeParameter);

      // global track control
      mCursorTrack.hasPrevious().markInterested();
      mCursorTrack.hasNext().markInterested();

      layer.bindPressed(mButtons[PREV_TRACK_BUTTON], mCursorTrack.selectPreviousAction());
      layer.bindPressed(mButtons[NEXT_TRACK_BUTTON], mCursorTrack.selectNextAction());

      // move between devices
      mCursorDevice.hasPrevious().markInterested();
      mCursorDevice.hasNext().markInterested();

      layer.bindPressed(mButtons[PREV_DEVICE_BUTTON], mCursorDevice.selectPreviousAction());
      layer.bindPressed(mButtons[NEXT_DEVICE_BUTTON], mCursorDevice.selectNextAction());

      // toggle device pages
      mRemoteControls.hasPrevious().markInterested();
      mRemoteControls.hasNext().markInterested();

      layer.bindPressed(mButtons[PREV_PAGE_BUTTON], mRemoteControls.selectPreviousAction());
      layer.bindPressed(mButtons[NEXT_PAGE_BUTTON], mRemoteControls.selectNextAction());

      // LED feedback
      layer.bind(mCursorTrack.hasPrevious(),    mLEDs[PREV_TRACK_BUTTON]);
      layer.bind(mCursorTrack.hasNext(),        mLEDs[NEXT_TRACK_BUTTON]);

      layer.bind(mCursorDevice.hasPrevious(),   mLEDs[PREV_DEVICE_BUTTON]);
      layer.bind(mCursorDevice.hasNext(),       mLEDs[NEXT_DEVICE_BUTTON]);

      layer.bind(mRemoteControls.hasPrevious(), mLEDs[PREV_PAGE_BUTTON]);
      layer.bind(mRemoteControls.hasNext(),     mLEDs[NEXT_PAGE_BUTTON]);

      // switch to mixer layer
      layer.bindPressed(mButtons[MODE_SWITCH_BUTTON], ()->{
         mModeSetting.set(MIXER_MODE_NAME);
      });

      mDeviceLayer = layer;
   }

   private void createMixerLayer()
   {
      final Layer layer = new Layer(mLayers, "Mixer");

      for (int i = 0; i < NUM_DEVICE_KNOBS; i++)
      {
         Channel channel = mTrackBank.getItemAt(i);

         layer.bind(mKnobs[i], channel.volume());

         layer.bindPressed(mButtons[i], channel.mute().toggleAction());
         layer.bind(channel.mute(), mLEDs[i]);
      }

      // joystick for track navigation
      layer.bind(mJoystickX, new DoubleConsumer()
      {
         @Override
         public void accept(final double value)
         {
            if(value < 0.25)
            {
               if(!triggered)
               {
                  triggered = true;
                  mCursorTrack.selectPrevious();
               }
            }
            else if(value > 0.75)
            {
               if(!triggered)
               {
                  triggered = true;
                  mCursorTrack.selectNext();
               }
            }
            else
            {
               triggered = false;
            }
         }

         boolean triggered = false;
      });

      layer.bind(mKnobs[MIXER_MODE_MASTER_KNOB], mMasterTrack.volume());

      // switch to device layer
      layer.bindPressed(mButtons[MODE_SWITCH_BUTTON], ()->{
         mModeSetting.set(DEVICE_MODE_NAME);
      });

      mMixerLayer = layer;
   }

   private void createKnob(int index)
   {
      final MidiIn midiIn = getHost().getMidiInPort(0);

      final int knobCC = index+KNOB_CC_BASE;

      final RelativeHardwareKnob knob = mHWSurface.createRelativeHardwareKnob("Knob " + (index+1));

      knob.setAdjustValueMatcher( midiIn.createRelativeSignedBitCCValueMatcher(0, knobCC, 100) );

      knob.isUpdatingTargetValue().markInterested();
      knob.targetValue().markInterested();
      knob.modulatedTargetValue().markInterested();
      knob.hasTargetValue().markInterested();

      mKnobs[index] = knob;
   }

   private void createButton(int index)
   {
      final MidiIn midiIn = getHost().getMidiInPort(0);

      final int buttonCC = index+BUTTON_CC_BASE;

      final HardwareButton button = mHWSurface.createHardwareButton("Button " + (index+1));

      button.pressedAction().setActionMatcher( midiIn.createCCActionMatcher(0, buttonCC) );
      button.releasedAction().setActionMatcher( midiIn.createCCActionMatcher(0, buttonCC) );

      mButtons[index] = button;
   }

   private void createLED(int index)
   {
      final OnOffHardwareLight led = mHWSurface.createOnOffHardwareLight("LED " + (index+1));
      led.setOnColor(Color.fromRGB(0, 1, 0));

      mLEDs[index] = led;
   }

   private void createJoystick()
   {
      final MidiIn midiIn = getHost().getMidiInPort(0);

      mJoystickX = mHWSurface.createAbsoluteHardwareKnob("Joystick X");
      mJoystickX.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 19));

      mJoystickY = mHWSurface.createAbsoluteHardwareKnob("Joystick Y");
      mJoystickY.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 20));
   }

   private void updateDeviceControlRings(boolean showModulation)
   {
      for (int i = 0; i < NUM_KNOBS_AND_BUTTONS; ++i)
      {
         updateDeviceControlRing(i, showModulation);
      }
   }

   private void updateDeviceControlRing(final int knobIndex, boolean showModulation)
   {
      final int knobCC = knobIndex + KNOB_CC_BASE;

      final RelativeHardwareKnob knob = mKnobs[knobIndex];

      // add modulated value
      if(knob.hasTargetValue().get())
      {
         final double knobValue = showModulation ? mKnobs[knobIndex].modulatedTargetValue().getAsDouble()
                                                 : mKnobs[knobIndex].targetValue().getAsDouble();

         double value  = 127 * knobValue;
         int midiValue = Math.min(Math.max((int)( Math.round(value) ), 0), 127);

         mLedValues[knobIndex] = midiValue;
      }
      else
      {
         mLedValues[knobIndex] = 0;
      }

      updateDeviceEncoder(knobCC, mLedValues[knobIndex]);
   }

   private void updateDeviceEncoder(int knobCC, int midiValue)
   {
      sendMidiCC(0, knobCC, midiValue);
   }

   private void updateDeviceButtonLEDs()
   {
      for(int i = 0; i < NUM_KNOBS_AND_BUTTONS; i++)
      {
         updateDeviceButtonLED(i+BUTTON_CC_BASE, mLEDs[i].isOn().currentValue());
      }
   }

   private void updateDeviceButtonLED(int buttonCC, boolean isOn)
   {
      if(isOn)
      {
         sendMidiCC(0, buttonCC, 127);
      }
      else
      {
         sendMidiCC(0, buttonCC, 0);
      }
   }

   private void sendMidiCC(final int channel, final int cc, final int value)
   {
      mMidiOut.sendMidi(0xb0 | channel, cc, value);
   }

   private MidiOut                  mMidiOut;

   private HardwareSurface          mHWSurface;

   private CursorTrack              mCursorTrack;
   private CursorDevice             mCursorDevice;

   private Layer                    mDeviceLayer;
   private Layer                    mMixerLayer;

   private CursorRemoteControlsPage mRemoteControls;
   private TrackBank                mTrackBank;
   private MasterTrack              mMasterTrack;

   private SettableEnumValue        mModeSetting;
   private SettableEnumValue        mDeviceModeDisplaySetting;

   private final Layers                 mLayers    = new Layers(this);
   private       AbsoluteHardwareKnob   mJoystickX;
   private       AbsoluteHardwareKnob   mJoystickY;
   private final RelativeHardwareKnob[] mKnobs     = new RelativeHardwareKnob[9];
   private final HardwareButton[]       mButtons   = new HardwareButton[9];
   private final OnOffHardwareLight[]   mLEDs      = new OnOffHardwareLight[9];
   private final int[]                  mLedValues = new int[9];
}
