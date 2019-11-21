package com.bitwig.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class EzCreatorPlus extends ControllerExtension
{
   public EzCreatorPlus(final EzCreatorPlusDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);
      final NoteInput keysInput = midiIn.createNoteInput("EZ-Creator Plus Keys", "80????", "90????");
      final NoteInput padsInput = midiIn.createNoteInput("EZ-Creator Plus Pads", "89????", "99????");

      keysInput.setShouldConsumeEvents(true);
      padsInput.setShouldConsumeEvents(true);

      final Integer[] keyTranslationTable = new Integer[128];
      for (int i = 0; i < keyTranslationTable.length; ++i)
         keyTranslationTable[i] = 0;

      final int keyC1 = 12 * 3;
      keyTranslationTable[36] = keyC1 + 0;
      keyTranslationTable[38] = keyC1 + 1;
      keyTranslationTable[42] = keyC1 + 2;
      keyTranslationTable[46] = keyC1 + 3;
      keyTranslationTable[48] = keyC1 + 4;
      keyTranslationTable[45] = keyC1 + 5;
      keyTranslationTable[49] = keyC1 + 6;
      keyTranslationTable[51] = keyC1 + 7;

      padsInput.setKeyTranslationTable(keyTranslationTable);

      host.getMidiOutPort(0).sendSysex(EzCreatorCommon.INIT_SYSEX);

      mTrackCursor = host.createCursorTrack("ez-creator-plus-cursor", "EZ-Creator Plus", 0, 0, true);
      mCursorDevice = mTrackCursor.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage("EZ-Creator Plus", 8, "");

      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);
         parameter.setIndication(true);
      }

      createHardwareControls();
      createLayers();
   }

   private void createLayers()
   {
      mLayers = new Layers(this);
      mMainLayer = new Layer(mLayers, "Main");
      for (int i = 0; i < 4; ++i)
      {
         mMainLayer.bind(mKnobs[i], mRemoteControls.getParameter(i));
         mMainLayer.bind(mSliders[i], mRemoteControls.getParameter(i + 4));
      }
      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      final MidiIn midiIn = getMidiInPort(0);
      mHardwareSurface = getHost().createHardwareSurface();
      mKnobs = new AbsoluteHardwareKnob[4];
      mSliders = new HardwareSlider[4];
      for (int i = 0; i < 4; ++i)
      {
         final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("Knob-" + i);
         knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x0E + i));
         mKnobs[i] = knob;

         final HardwareSlider slider = mHardwareSurface.createHardwareSlider("Slider-" + i);
         slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x03 + i));
         mSliders[i] = slider;
      }

      mHardwareSurface.setPhysicalSize(250, 150);
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("Knob-0").setBounds(11.5, 12.25, 10.0, 10.0);
      surface.hardwareElementWithId("Slider-0").setBounds(11.5, 34.75, 10.0, 50.0);
      surface.hardwareElementWithId("Knob-1").setBounds(35.5, 12.25, 10.0, 10.0);
      surface.hardwareElementWithId("Slider-1").setBounds(35.5, 34.75, 10.0, 50.0);
      surface.hardwareElementWithId("Knob-2").setBounds(59.5, 12.25, 10.0, 10.0);
      surface.hardwareElementWithId("Slider-2").setBounds(59.5, 34.75, 10.0, 50.0);
      surface.hardwareElementWithId("Knob-3").setBounds(83.5, 12.25, 10.0, 10.0);
      surface.hardwareElementWithId("Slider-3").setBounds(83.5, 34.75, 10.0, 50.0);
   }

   @Override
   public void exit()
   {
      getHost().getMidiOutPort(0).sendSysex(EzCreatorCommon.DEINIT_SYSEX);
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   private CursorTrack mTrackCursor;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;

   private HardwareSurface mHardwareSurface;
   private AbsoluteHardwareKnob[] mKnobs;
   private HardwareSlider[] mSliders;
   private Layers mLayers;
   private Layer mMainLayer;
}
