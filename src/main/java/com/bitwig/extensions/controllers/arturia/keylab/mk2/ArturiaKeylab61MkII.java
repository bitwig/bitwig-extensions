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

      surface.hardwareElementWithId("PAD1").setBounds(137.0, 30.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAD2").setBounds(174.5, 31.25, 10.0, 10.0);
      surface.hardwareElementWithId("PAD3").setBounds(206.75, 30.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAD4").setBounds(238.25, 32.0, 10.0, 10.0);
      surface.hardwareElementWithId("PAD5").setBounds(140.75, 59.25, 10.0, 10.0);
      surface.hardwareElementWithId("PAD6").setBounds(175.0, 63.25, 10.0, 10.0);
      surface.hardwareElementWithId("PAD7").setBounds(207.75, 60.0, 10.0, 10.0);
      surface.hardwareElementWithId("PAD8").setBounds(243.5, 60.0, 10.0, 10.0);
      surface.hardwareElementWithId("PAD9").setBounds(142.25, 85.25, 10.0, 10.0);
      surface.hardwareElementWithId("PAD10").setBounds(172.75, 89.75, 10.0, 10.0);
      surface.hardwareElementWithId("PAD11").setBounds(202.5, 87.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAD12").setBounds(238.25, 91.25, 10.0, 10.0);
      surface.hardwareElementWithId("PAD13").setBounds(137.0, 115.75, 10.0, 10.0);
      surface.hardwareElementWithId("PAD14").setBounds(169.75, 112.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAD15").setBounds(204.75, 113.25, 10.0, 10.0);
      surface.hardwareElementWithId("PAD16").setBounds(235.75, 111.0, 10.0, 10.0);
      surface.hardwareElementWithId("UNDO").setBounds(378.5, 74.5, 10.0, 10.0);
      surface.hardwareElementWithId("REWIND").setBounds(276.75, 114.0, 10.0, 10.0);
      surface.hardwareElementWithId("FORWARD").setBounds(297.75, 115.5, 10.0, 10.0);
      surface.hardwareElementWithId("STOP").setBounds(317.25, 118.0, 10.0, 10.0);
      surface.hardwareElementWithId("PLAY_OR_PAUSE").setBounds(337.75, 117.0, 10.0, 10.0);
      surface.hardwareElementWithId("RECORD").setBounds(362.25, 114.5, 10.0, 10.0);
      surface.hardwareElementWithId("LOOP").setBounds(382.5, 118.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT1").setBounds(581.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT2").setBounds(610.5, 121.5, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT3").setBounds(640.0, 122.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT4").setBounds(673.75, 124.25, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT5").setBounds(703.5, 123.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT6").setBounds(732.0, 122.5, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT7").setBounds(761.5, 123.5, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT8").setBounds(792.75, 123.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT_MULTI").setBounds(823.0, 122.5, 10.0, 10.0);
      surface.hardwareElementWithId("encoder1").setBounds(580.25, 26.0, 10.0, 10.0);
      surface.hardwareElementWithId("encoder2").setBounds(611.5, 29.5, 10.0, 10.0);
      surface.hardwareElementWithId("encoder3").setBounds(642.5, 28.0, 10.0, 10.0);
      surface.hardwareElementWithId("encoder4").setBounds(674.25, 29.75, 10.0, 10.0);
      surface.hardwareElementWithId("encoder5").setBounds(704.0, 33.75, 10.0, 10.0);
      surface.hardwareElementWithId("encoder6").setBounds(733.5, 28.5, 10.0, 10.0);
      surface.hardwareElementWithId("encoder7").setBounds(767.25, 28.5, 10.0, 10.0);
      surface.hardwareElementWithId("encoder8").setBounds(794.75, 29.0, 10.0, 10.0);
      surface.hardwareElementWithId("encoder9").setBounds(823.0, 32.25, 10.0, 10.0);
      surface.hardwareElementWithId("fader1").setBounds(583.25, 54.75, 10.0, 50.0);
      surface.hardwareElementWithId("fader2").setBounds(612.5, 53.75, 10.0, 50.0);
      surface.hardwareElementWithId("fader3").setBounds(644.75, 54.75, 10.0, 50.0);
      surface.hardwareElementWithId("fader4").setBounds(671.25, 53.75, 10.0, 50.0);
      surface.hardwareElementWithId("fader5").setBounds(702.75, 57.25, 10.0, 50.0);
      surface.hardwareElementWithId("fader6").setBounds(731.5, 53.25, 10.0, 50.0);
      surface.hardwareElementWithId("fader7").setBounds(762.75, 55.25, 10.0, 50.0);
      surface.hardwareElementWithId("fader8").setBounds(790.75, 53.75, 10.0, 50.0);
      surface.hardwareElementWithId("fader9").setBounds(819.75, 56.75, 10.0, 50.0);
      surface.hardwareElementWithId("wheel").setBounds(447.5, 73.25, 38.5, 28.0);
      surface.hardwareElementWithId("WRITE").setBounds(379.75, 45.75, 10.0, 10.0);
      surface.hardwareElementWithId("READ").setBounds(356.0, 48.0, 10.0, 10.0);
      surface.hardwareElementWithId("SAVE").setBounds(280.0, 75.25, 10.0, 10.0);
      surface.hardwareElementWithId("SOLO").setBounds(280.75, 46.5, 10.0, 10.0);
      surface.hardwareElementWithId("MUTE").setBounds(306.75, 47.25, 10.0, 10.0);
      surface.hardwareElementWithId("METRO").setBounds(355.25, 76.0, 10.0, 10.0);
      surface.hardwareElementWithId("PUNCH_IN").setBounds(305.0, 76.75, 10.0, 10.0);
      surface.hardwareElementWithId("PUNCH_OUT").setBounds(332.0, 76.0, 10.0, 10.0);
      surface.hardwareElementWithId("NEXT").setBounds(553.25, 35.0, 10.0, 10.0);
      surface.hardwareElementWithId("PREVIOUS").setBounds(555.25, 64.0, 10.0, 10.0);
      surface.hardwareElementWithId("OCT_MINUS").setBounds(44.0, 52.0, 10.0, 10.0);
      surface.hardwareElementWithId("OCT_PLUS").setBounds(71.0, 53.5, 10.0, 10.0);
      surface.hardwareElementWithId("CHORD").setBounds(46.0, 30.5, 10.0, 10.0);
      surface.hardwareElementWithId("TRANS").setBounds(71.0, 29.0, 10.0, 10.0);


   }
}
