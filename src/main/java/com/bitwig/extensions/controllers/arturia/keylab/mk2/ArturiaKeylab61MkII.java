package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;

public class ArturiaKeylab61MkII extends ArturiaKeylabMkII
{
   public ArturiaKeylab61MkII(
      final ArturiaKeylab61MkIIControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   protected void initHardwareLayout(final HardwareSurface surface)
   {
      surface.setPhysicalSize(875, 297);

      /*surface.hardwareElementWithId("CHORD").setBounds(44.0, 28.5, 14.5, 8.0);
      surface.hardwareElementWithId("TRANS").setBounds(69.5, 28.5, 14.5, 8.0);
      surface.hardwareElementWithId("OCT_MINUS").setBounds(44.0, 50.5, 14.5, 8.0);
      surface.hardwareElementWithId("OCT_PLUS").setBounds(69.5, 50.5, 14.5, 8.0);*/
      surface.hardwareElementWithId("PAD1").setBounds(135.25, 27.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD2").setBounds(168.25, 27.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD3").setBounds(202.5, 27.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD4").setBounds(234.75, 27.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD5").setBounds(135.25, 53.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD6").setBounds(168.25, 53.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD7").setBounds(202.5, 53.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD8").setBounds(234.75, 53.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD9").setBounds(135.25, 79.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD10").setBounds(168.25, 79.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD11").setBounds(202.5, 79.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD12").setBounds(234.75, 79.75, 24.0, 21.5);
      surface.hardwareElementWithId("PAD13").setBounds(135.25, 107.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD14").setBounds(168.25, 107.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD15").setBounds(202.5, 107.5, 24.0, 21.5);
      surface.hardwareElementWithId("PAD16").setBounds(234.75, 107.5, 24.0, 21.5);
      surface.hardwareElementWithId("SOLO1").setBounds(280.75, 45.75, 12.5, 6.0);
      surface.hardwareElementWithId("MUTE1").setBounds(305.0, 45.75, 12.5, 6.0);
      surface.hardwareElementWithId("READ").setBounds(355.0, 45.75, 12.5, 6.0);
      surface.hardwareElementWithId("WRITE").setBounds(378.25, 45.75, 12.5, 6.0);
      surface.hardwareElementWithId("SAVE").setBounds(280.0, 74.5, 12.5, 6.0);
      surface.hardwareElementWithId("PUNCH_IN").setBounds(305.0, 74.5, 12.5, 6.0);
      surface.hardwareElementWithId("PUNCH_OUT").setBounds(330.0, 74.5, 12.5, 6.0);
      surface.hardwareElementWithId("METRO").setBounds(355.0, 74.5, 12.5, 6.0);
      surface.hardwareElementWithId("UNDO").setBounds(379.75, 74.5, 12.5, 6.0);
      surface.hardwareElementWithId("REWIND").setBounds(276.75, 114.0, 13.0, 10.5);
      surface.hardwareElementWithId("FORWARD").setBounds(297.25, 114.0, 13.0, 10.5);
      surface.hardwareElementWithId("STOP").setBounds(318.0, 114.0, 13.0, 10.5);
      surface.hardwareElementWithId("PLAY_OR_PAUSE").setBounds(338.5, 114.0, 13.0, 10.5);
      surface.hardwareElementWithId("RECORD").setBounds(359.25, 114.0, 13.0, 10.5);
      surface.hardwareElementWithId("LOOP").setBounds(379.75, 114.0, 13.0, 10.5);
      surface.hardwareElementWithId("WHEEL_CLICK").setBounds(488.0, 82.5, 10.0, 10.0);
      surface.hardwareElementWithId("NEXT").setBounds(553.25, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("PREVIOUS").setBounds(553.25, 63.5, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT1").setBounds(581.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT2").setBounds(612.0, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT3").setBounds(642.0, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT4").setBounds(672.25, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT5").setBounds(702.25, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT6").setBounds(732.5, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT7").setBounds(762.5, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT8").setBounds(792.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT_MULTI").setBounds(822.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("encoder1").setBounds(580.25, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder2").setBounds(610.5, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder3").setBounds(640.5, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder4").setBounds(670.75, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder5").setBounds(700.75, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder6").setBounds(731.0, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder7").setBounds(761.0, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder8").setBounds(791.25, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("encoder9").setBounds(821.25, 26.0, 13.5, 13.5);
      surface.hardwareElementWithId("fader1").setBounds(583.25, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader2").setBounds(613.0, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader3").setBounds(642.75, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader4").setBounds(672.5, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader5").setBounds(702.25, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader6").setBounds(732.0, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader7").setBounds(761.75, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader8").setBounds(791.5, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("fader9").setBounds(821.25, 53.25, 10.0, 51.0);
      surface.hardwareElementWithId("wheel").setBounds(447.5, 73.25, 38.5, 28.0);
      surface.hardwareElementWithId("display").setBounds(435.0, 38.25, 67.25, 15.5);
      surface.hardwareElementWithId("display").setBounds(435.0, 38.25, 67.25, 15.5);
      surface.hardwareElementWithId("piano").setBounds(25.0, 154.0, 831.25, 138.5);


   }
}
