package com.bitwig.extensions.controllers.kenton;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayerGroup;
import com.bitwig.extensions.framework.Layers;

public class KillaMixMiniExtension extends ControllerExtension
{
   private static final int NUM_KNOBS_AND_BUTTONS = 9;
   private static final int NUM_DEVICE_KNOBS      = 8;
   private static final int KNOB_CC_BASE          = 1;
   private static final int BUTTON_CC_BASE        = 10;

   private static final int VOLUME_KNOB           = 8;
   private static final int PREV_TRACK_BUTTON     = 0;
   private static final int NEXT_TRACK_BUTTON     = 1;
   private static final int PREV_DEVICE_BUTTON    = 2;
   private static final int NEXT_DEVICE_BUTTON    = 3;
   private static final int PREV_PAGE_BUTTON      = 4;
   private static final int NEXT_PAGE_BUTTON      = 5;

   protected KillaMixMiniExtension(final KillaMixMiniExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      initHardwareSurface();

      mDeviceLayer.activate();
   }

   @Override
   public void exit()
   { }

   @Override
   public void flush()
   {
      updateDeviceControlRings();
      updateDeviceButtons();
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
      }

      initHardwareLayout(mHWSurface);

      final DocumentState settings = host.getDocumentState();

      mCursorTrack = host.createCursorTrack(3, 0);
      mCursorTrack.hasPrevious().markInterested();
      mCursorTrack.hasNext().markInterested();

      mCursorDevice   = mCursorTrack.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      mTrackBank = host.createTrackBank(8, 0, 0);
      mTrackBank.followCursorTrack(mCursorTrack);

      // global track control
      mButtons[PREV_TRACK_BUTTON].pressedAction().addBinding(mCursorTrack.selectPreviousAction());
      mButtons[NEXT_TRACK_BUTTON].pressedAction().addBinding(mCursorTrack.selectNextAction());

      createDeviceLayer();
   }

   private void initHardwareLayout(final HardwareSurface surface)
   {
      surface.hardwareElementWithId("Knob 1").setBounds(4.0, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 2").setBounds(16.25, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 3").setBounds(28.25, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 4").setBounds(40.5, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 5").setBounds(52.5, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 6").setBounds(64.75, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 7").setBounds(77.0, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 8").setBounds(89.0, 10.0, 36, 36.0);
      surface.hardwareElementWithId("Knob 9").setBounds(101.0, 10.0, 36, 36.0);

      surface.hardwareElementWithId("Button 1").setBounds(4.0  , 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 2").setBounds(16.25, 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 3").setBounds(28.25, 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 4").setBounds(40.5 , 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 5").setBounds(52.5 , 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 6").setBounds(64.75, 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 7").setBounds(77.0 , 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 8").setBounds(89.0 , 50.0, 10, 10.0);
      surface.hardwareElementWithId("Button 8").setBounds(101.0 , 50.0, 10, 10.0);
   }

   private void createDeviceLayer()
   {
      final Layer layer = new Layer(mLayers, "Device");

      // device knobs
      for (int i = 0; i < NUM_DEVICE_KNOBS; i++)
      {
         final AbsoluteHardwareKnob knob   = mKnobs[i];
         final RemoteControl        remote = mRemoteControls.getParameter(i);

         remote.setIndication(true);
         remote.markInterested();

         layer.bind(knob, remote);
      }

      // track volume knob
      final Parameter volumeParameter = mCursorTrack.volume();
      volumeParameter.setIndication(true);
      volumeParameter.markInterested();
      layer.bind(mKnobs[VOLUME_KNOB], volumeParameter);

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

      // since the buttons-LEDs toggle on/off by the hardware itself, we must ensure that
      // we flush/resend the current state to the buttons again, after they have been pressed
      for (int i = 0; i < NUM_KNOBS_AND_BUTTONS; i++)
      {
         final HardwareButton button   = mButtons[i];

         layer.bindReleased(mButtons[i], () -> {
            getHost().requestFlush();
         });
      }

      mDeviceLayer = layer;
   }

   private void createKnob(int index)
   {
      final MidiIn midiIn = getHost().getMidiInPort(0);

      final int knobCC = index+KNOB_CC_BASE;

      final AbsoluteHardwareKnob knob = mHWSurface.createAbsoluteHardwareKnob("Knob " + (index+1));

      knob.disableTakeOver();
      knob.setAdjustValueMatcher( midiIn.createAbsoluteCCValueMatcher(0, knobCC) );

      knob.isUpdatingTargetValue().markInterested();
      knob.targetValue().markInterested();
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

   private void updateDeviceControlRings()
   {
      for (int i = 0; i < NUM_KNOBS_AND_BUTTONS; ++i)
      {
         updateDeviceControlRing(i);
      }
   }

   private void updateDeviceControlRing(final int knobIndex)
   {
      final int knobCC = knobIndex + KNOB_CC_BASE;

      final AbsoluteHardwareKnob knob = mKnobs[knobIndex];

      if(knob.hasTargetValue().get())
      {
         double value = 127 * mKnobs[knobIndex].targetValue().getAsDouble();
         int midiValue = Math.min(Math.max((int)( Math.round(value) ), 0), 127);

         mLedValues[knobIndex] = midiValue;

         System.out.println(value + " x " + midiValue);
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

   private void updateDeviceButtons()
   {
      updateDeviceButton(PREV_TRACK_BUTTON+BUTTON_CC_BASE,  mCursorTrack.hasPrevious().getAsBoolean());
      updateDeviceButton(NEXT_TRACK_BUTTON+BUTTON_CC_BASE,  mCursorTrack.hasNext().getAsBoolean());

      updateDeviceButton(PREV_DEVICE_BUTTON+BUTTON_CC_BASE, mCursorDevice.hasPrevious().getAsBoolean());
      updateDeviceButton(NEXT_DEVICE_BUTTON+BUTTON_CC_BASE, mCursorDevice.hasNext().getAsBoolean());

      updateDeviceButton(PREV_PAGE_BUTTON+BUTTON_CC_BASE, mRemoteControls.hasPrevious().getAsBoolean());
      updateDeviceButton(NEXT_PAGE_BUTTON+BUTTON_CC_BASE, mRemoteControls.hasNext().getAsBoolean());
   }

   private void updateDeviceButton(int buttonCC, boolean isOn)
   {
      System.out.println(buttonCC+" is on "+isOn);
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

   private CursorRemoteControlsPage mRemoteControls;
   private TrackBank                mTrackBank;

   private final Layers                 mLayers    = new Layers(this);
   private final AbsoluteHardwareKnob[] mKnobs     = new AbsoluteHardwareKnob[9];
   private final HardwareButton[]       mButtons   = new HardwareButton[9];
   private final int[]                  mLedValues = new int[9];
}
