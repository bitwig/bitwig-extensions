package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;

public class ArturiaKeylab49MkII extends ArturiaKeylabMkII
{
   public ArturiaKeylab49MkII(
      final ArturiaKeylab49MkIIControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   protected void initHardwareLayout(final HardwareSurface surface)
   {
      surface.setPhysicalSize(793, 297);

      surface.hardwareElementWithId("CHORD").setBounds(34.5, 175.5, 14.5, 8.0);
      surface.hardwareElementWithId("TRANS").setBounds(60.0, 175.5, 14.5, 8.0);
      surface.hardwareElementWithId("OCT_MINUS").setBounds(34.5, 197.5, 14.5, 8.0);
      surface.hardwareElementWithId("OCT_PLUS").setBounds(60.0, 197.5, 14.5, 8.0);
      surface.hardwareElementWithId("PAD1").setBounds(75.75, 31.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD2").setBounds(108.75, 31.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD3").setBounds(143.0, 31.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD4").setBounds(175.25, 31.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD5").setBounds(75.75, 57.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD6").setBounds(108.75, 57.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD7").setBounds(143.0, 57.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD8").setBounds(175.25, 57.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD9").setBounds(75.75, 83.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD10").setBounds(108.75, 83.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD11").setBounds(143.0, 83.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD12").setBounds(175.25, 83.25, 24.0, 21.5);
      surface.hardwareElementWithId("PAD13").setBounds(75.75, 111.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD14").setBounds(108.75, 111.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD15").setBounds(143.0, 111.0, 24.0, 21.5);
      surface.hardwareElementWithId("PAD16").setBounds(175.25, 111.0, 24.0, 21.5);
      surface.hardwareElementWithId("SOLO").setBounds(217.75, 47.25, 12.5, 6.0);
      surface.hardwareElementWithId("MUTE").setBounds(242.0, 47.25, 12.5, 6.0);
      surface.hardwareElementWithId("READ").setBounds(292.0, 47.25, 12.5, 6.0);
      surface.hardwareElementWithId("WRITE").setBounds(315.25, 47.25, 12.5, 6.0);
      surface.hardwareElementWithId("SAVE").setBounds(217.0, 76.0, 12.5, 6.0);
      surface.hardwareElementWithId("PUNCH_IN").setBounds(242.0, 76.0, 12.5, 6.0);
      surface.hardwareElementWithId("PUNCH_OUT").setBounds(267.0, 76.0, 12.5, 6.0);
      surface.hardwareElementWithId("METRO").setBounds(292.0, 76.0, 12.5, 6.0);
      surface.hardwareElementWithId("UNDO").setBounds(316.75, 76.0, 12.5, 6.0);
      surface.hardwareElementWithId("REWIND").setBounds(214.25, 117.5, 13.0, 10.5);
      surface.hardwareElementWithId("FORWARD").setBounds(234.75, 117.5, 13.0, 10.5);
      surface.hardwareElementWithId("STOP").setBounds(255.5, 117.5, 13.0, 10.5);
      surface.hardwareElementWithId("PLAY_OR_PAUSE").setBounds(276.0, 117.5, 13.0, 10.5);
      surface.hardwareElementWithId("RECORD").setBounds(296.75, 117.5, 13.0, 10.5);
      surface.hardwareElementWithId("LOOP").setBounds(317.25, 117.5, 13.0, 10.5);
      surface.hardwareElementWithId("WHEEL_CLICK").setBounds(428.5, 86.0, 10.0, 10.0);
      surface.hardwareElementWithId("NEXT").setBounds(477.5, 38.5, 10.0, 10.0);
      surface.hardwareElementWithId("PREVIOUS").setBounds(477.5, 67.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT1").setBounds(505.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT2").setBounds(536.0, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT3").setBounds(566.0, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT4").setBounds(596.25, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT5").setBounds(626.5, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT6").setBounds(656.5, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT7").setBounds(686.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT8").setBounds(716.75, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("SELECT_MULTI").setBounds(747.0, 121.0, 10.0, 10.0);
      surface.hardwareElementWithId("encoder1").setBounds(504.25, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder2").setBounds(534.5, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder3").setBounds(564.5, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder4").setBounds(594.75, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder5").setBounds(624.75, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder6").setBounds(655.0, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder7").setBounds(685.0, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder8").setBounds(715.25, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("encoder9").setBounds(745.25, 28.5, 13.5, 13.5);
      surface.hardwareElementWithId("fader1").setBounds(506.5, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader2").setBounds(536.25, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader3").setBounds(566.0, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader4").setBounds(595.75, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader5").setBounds(625.5, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader6").setBounds(655.25, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader7").setBounds(685.0, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader8").setBounds(714.75, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("fader9").setBounds(744.5, 56.75, 10.0, 51.0);
      surface.hardwareElementWithId("wheel").setBounds(386.75, 77.25, 38.5, 28.0);
      surface.hardwareElementWithId("display").setBounds(375.5, 41.75, 67.25, 15.5);
      surface.hardwareElementWithId("display").setBounds(375.5, 41.75, 67.25, 15.5);
      surface.hardwareElementWithId("piano").setBounds(101.0, 157.75, 669.0, 138.75);

   }
}
