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
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class VersaKeyCommonExtension extends ControllerExtension
{
   protected VersaKeyCommonExtension(final VersaKeyCommonDefinition definition,
                                     final ControllerHost host,
                                     final int numKnobs)
   {
      super(definition, host);

      mNumKnobs = numKnobs;
   }

   @Override
   public void init()
   {
      final String    modelName = getExtensionDefinition().getHardwareModel();
      final String    modelNameL= modelName.toLowerCase().replace(" ","-");
      final ControllerHost host = getHost();

      final MidiIn midiIn = host.getMidiInPort(0);

      // To support transport controls
      midiIn.setSysexCallback(this::onSysex);

      final NoteInput keysInput = midiIn.createNoteInput(modelName+" Keys", "80????", "90????");
      final NoteInput padsInput = midiIn.createNoteInput(modelName+" Pads", "89????", "99????");

      keysInput.setShouldConsumeEvents(true);
      padsInput.setShouldConsumeEvents(true);

      final Integer[] keyTranslationTable = new Integer[128];
      for (int i = 0; i < keyTranslationTable.length; ++i)
      {
         keyTranslationTable[i] = 0;
      }

      final int keyC1 = 12 * 3;
      keyTranslationTable[0x30] = keyC1 + 0;
      keyTranslationTable[0x2D] = keyC1 + 1;
      keyTranslationTable[0x31] = keyC1 + 2;
      keyTranslationTable[0x33] = keyC1 + 3;
      keyTranslationTable[0x24] = keyC1 + 4;
      keyTranslationTable[0x26] = keyC1 + 5;
      keyTranslationTable[0x2A] = keyC1 + 6;
      keyTranslationTable[0x2E] = keyC1 + 7;

      padsInput.setKeyTranslationTable(keyTranslationTable);

      // set device to "Bitwig-Mode"
      host.getMidiOutPort(0).sendSysex(EzCreatorCommon.INIT_SYSEX);

      mTransport      = host.createTransport();
      mTrackCursor    = host.createCursorTrack(modelNameL, modelName, 0, 0, true);
      mCursorDevice   = mTrackCursor.createCursorDevice();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(modelName, mNumKnobs, "");

      for (int i = 0; i < mNumKnobs; i++)
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
      for (int i = 0; i < mNumKnobs; i++)
      {
         mMainLayer.bind(mKnobs[i], mRemoteControls.getParameter(i));
      }
      mMainLayer.activate();
   }

   private void createHardwareControls()
   {
      final double knobDistance = 11.5;

      final MidiIn midiIn = getMidiInPort(0);

      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(40 + mNumKnobs*knobDistance, 150);

      mKnobs = new AbsoluteHardwareKnob[mNumKnobs];
      for (int i = 0; i < mNumKnobs; ++i)
      {
         final String               knobName = "Knob-" + i;
         final AbsoluteHardwareKnob knob     = mHardwareSurface.createAbsoluteHardwareKnob(knobName);

         knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x0A + i));
         mKnobs[i] = knob;

         final double knobOffset = 11.5 * i;
         mHardwareSurface.hardwareElementWithId(knobName).setBounds(11.5 + knobOffset, 12.25, 10.0, 10.0);
      }
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

   private void onSysex(final String data)
   {
      switch (data)
      {
         case "f07f7f0605f7":
            mTransport.rewind();
            break;
         case "f07f7f0604f7":
            mTransport.fastForward();
            break;
         case "f07f7f0601f7":
            mTransport.stop();
            break;
         case "f07f7f0602f7":
            mTransport.play();
            break;
         case "f07f7f0606f7":
            mTransport.record();
            break;
      }
   }

   private final int mNumKnobs;
   private Transport                mTransport;
   private CursorTrack              mTrackCursor;
   private PinnableCursorDevice     mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;

   private HardwareSurface          mHardwareSurface;
   private AbsoluteHardwareKnob[]   mKnobs;
   private Layers                   mLayers;
   private Layer                    mMainLayer;
}
