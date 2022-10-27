package com.bitwig.extensions.controllers.icon;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;

public class VCastPro extends VCast
{
   public VCastPro(
      final ControllerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      super.init();

      mArranger = getHost().createArranger();

      initJogWheel();
      initProButtons();
      doProLayout();
      initProLayer();
   }

   private void initJogWheel()
   {
      mJogWheel = mHardwareSurface.createRelativeHardwareKnob("jogWheel");
      mJogWheel.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, 0x3C, 16));
   }

   private void initProButtons()
   {
      mScrubButton = addNoteOnButton("scrub", "Scrub", 0, 0x65);

      mMcuUpButton = addNoteOnButton("mcuUp", "MCU Up", 0, 0x60);
      mMcuDownButton = addNoteOnButton("mcuDown", "MCU Down", 0, 0x61);
      mMcuLeftButton = addNoteOnButton("mcuLeft", "MCU Left", 0, 0x62);
      mMcuRightButton = addNoteOnButton("mcuRight", "MCU Right", 0, 0x63);

      mVerticalZoomButton = addNoteOnButton("vzoom", "VZoom", 0, 0x64);
      mHorizontalZoomButton = addNoteOnButton("hzoom", "HZoom", 0, 0x73);

      mArrangeButton = addNoteOnButton("arrange", "Arrange", 0, 0x50);
      mMixButton = addNoteOnButton("mix", "Mix", 0, 0x51);
      mEditButton = addNoteOnButton("edit", "Edit", 0, 0x52);
   }

   private void doProLayout()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("jogWheel").setBounds(78.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("scrub").setBounds(90.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("mcuUp").setBounds(126.0, 39.5, 10.0, 10.0);
      surface.hardwareElementWithId("mcuDown").setBounds(126.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("mcuLeft").setBounds(114.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("mcuRight").setBounds(138.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("hzoom").setBounds(150.0, 39.5, 10.0, 10.0);
      surface.hardwareElementWithId("vzoom").setBounds(150.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("arrange").setBounds(44.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("mix").setBounds(56.0, 51.5, 10.0, 10.0);
      surface.hardwareElementWithId("edit").setBounds(68.0, 51.5, 10.0, 10.0);
   }

   void initProLayer()
   {
      final Application application = getHost().createApplication();

      final Layer proLayer = new Layer(mLayers, "Pro Layer");

      mScrubButton.isPressed().markInterested();
      mHorizontalZoomButton.isPressed().markInterested();
      mVerticalZoomButton.isPressed().markInterested();
      proLayer.bind(mJogWheel, (delta) -> {
         if (mHorizontalZoomButton.isPressed().get())
         {
            if (delta > 0)
               horizontalZoomIn();
            else
               horizontalZoomOut();
         }
         else if (mVerticalZoomButton.isPressed().get())
         {
            if (delta > 0)
               verticalZoomIn();
            else
               verticalZoomOut();
         }
         else
         {
            mTransport.incPosition(delta * jogStep(), false);
         }
      });

      proLayer.bind(mScrubButton.isPressed(), (OnOffHardwareLight) mScrubButton.backgroundLight());

      proLayer.bindPressed(mMcuUpButton, application::arrowKeyUp);
      proLayer.bindPressed(mMcuDownButton, application::arrowKeyDown);
      proLayer.bindPressed(mMcuLeftButton, application::arrowKeyLeft);
      proLayer.bindPressed(mMcuRightButton, application::arrowKeyRight);

      proLayer.bindPressed(mArrangeButton, () -> application.setPanelLayout(Application.PANEL_LAYOUT_ARRANGE));
      proLayer.bindPressed(mMixButton, () -> application.setPanelLayout(Application.PANEL_LAYOUT_MIX));
      proLayer.bindPressed(mEditButton, () -> application.setPanelLayout(Application.PANEL_LAYOUT_EDIT));

      proLayer.activate();
   }

   private double jogStep()
   {
      return !mScrubButton.isPressed().get() ? 16 : 64;
   }

   private void horizontalZoomIn()
   {
      mArranger.zoomIn();
   }

   private void horizontalZoomOut()
   {
      mArranger.zoomOut();
   }

   private void verticalZoomIn()
   {
      mArranger.zoomInLaneHeightsAll();
   }

   private void verticalZoomOut()
   {
      mArranger.zoomOutLaneHeightsAll();
   }

   private Arranger mArranger;

   private RelativeHardwareKnob mJogWheel;
   private HardwareButton mScrubButton;
   private HardwareButton mMcuUpButton, mMcuDownButton, mMcuLeftButton, mMcuRightButton;
   private HardwareButton mVerticalZoomButton, mHorizontalZoomButton;
   private HardwareButton mArrangeButton, mMixButton, mEditButton;
}
