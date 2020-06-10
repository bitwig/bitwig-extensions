package com.bitwig.extensions.controllers.devine;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class EzCreatorKeyCommonExtension extends ControllerExtension
{
   public EzCreatorKeyCommonExtension(final EzCreatorKeyCommonDefinition definition,
                                      final ControllerHost host,
                                      int   mainSliderCC)
   {
      super(definition, host);

      mMainSliderCC = mainSliderCC;
   }

   @Override
   public void init()
   {
      final String modelName     = getExtensionDefinition().getName();
      final String    modelNameL= modelName.toLowerCase().replace(" ","-");
      final ControllerHost host  = getHost();
      final MidiIn midiIn        = host.getMidiInPort(0);

      // Note input
      midiIn.createNoteInput(modelName, "80????", "90????").setShouldConsumeEvents(true);

      // set device to "Bitwig-Mode"
      host.getMidiOutPort(0).sendSysex(EzCreatorCommon.INIT_SYSEX);

      CursorTrack              cursorTrack     = host.createCursorTrack(modelNameL, modelName, 0, 0, true);
      PinnableCursorDevice     cursorDevice    = cursorTrack.createCursorDevice();
                               mRemoteControls = cursorDevice.createCursorRemoteControlsPage(modelName, 1, "");

      mParameter = mRemoteControls.getParameter(0);
      mParameter.markInterested();
      mParameter.setIndication(true);

      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(40, 50);

      mSlider = mHardwareSurface.createHardwareSlider("Slider");
      mSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, mMainSliderCC));
      mHardwareSurface.hardwareElementWithId("Slider").setBounds(15, 10, 10, 30);

      mLayers = new Layers(this);
      mMainLayer = new Layer(mLayers, "Main");
      mMainLayer.bind(mSlider, mRemoteControls.getParameter(0));
      mMainLayer.activate();
   }

   @Override
   public void exit()
   {
      getHost().getMidiOutPort(0).sendSysex(EzCreatorCommon.DEINIT_SYSEX);
   }

   @Override
   public void flush()
   { }

   private int mMainSliderCC;

   private CursorRemoteControlsPage mRemoteControls;

   private HardwareSlider           mSlider;
   private HardwareSurface          mHardwareSurface;
   private RemoteControl            mParameter;
   private Layers                   mLayers;
   private Layer                    mMainLayer;
}
