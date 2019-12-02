package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;

public class PresonusFaderPort16 extends PresonusFaderPort
{

   public PresonusFaderPort16(final PresonusFaderPort16Definition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   protected void initHardwareLayout()
   {
      mHardwareSurface.setPhysicalSize(500, 300);

      final HardwareSurface surface = mHardwareSurface;

      surface.hardwareElementWithId("arm").setBounds(19.75, 63.25, 12.5, 7.5);
      surface.hardwareElementWithId("master").setBounds(407.5, 139.75, 13.5, 8.5);
      surface.hardwareElementWithId("shift_left").setBounds(19.5, 157.25, 12.0, 12.25);
      surface.hardwareElementWithId("shift_right").setBounds(387.25, 156.5, 13.25, 12.25);
      surface.hardwareElementWithId("play").setBounds(434.25, 190.75, 21.75, 21.25);
      surface.hardwareElementWithId("stop").setBounds(408.25, 186.5, 12.25, 13.5);
      surface.hardwareElementWithId("record").setBounds(469.25, 186.5, 12.25, 13.5);
      surface.hardwareElementWithId("metronome").setBounds(428.0, 139.75, 13.5, 8.5);
      surface.hardwareElementWithId("loop").setBounds(417.75, 167.75, 12.5, 12.25);
      surface.hardwareElementWithId("rewind").setBounds(438.75, 167.75, 12.5, 12.25);
      surface.hardwareElementWithId("fast_forward").setBounds(459.5, 167.75, 12.5, 12.25);
      surface.hardwareElementWithId("clear_solo").setBounds(19.75, 81.25, 12.5, 7.5);
      surface.hardwareElementWithId("clear_mute").setBounds(19.75, 96.0, 12.5, 7.5);
      surface.hardwareElementWithId("bypass").setBounds(19.75, 111.25, 12.5, 7.5);
      surface.hardwareElementWithId("macro").setBounds(19.75, 126.5, 12.5, 7.5);
      surface.hardwareElementWithId("link").setBounds(19.75, 141.5, 12.5, 7.5);
      surface.hardwareElementWithId("audio").setBounds(386.5, 80.25, 12.5, 7.5);
      surface.hardwareElementWithId("VI").setBounds(386.5, 95.25, 12.5, 7.5);
      surface.hardwareElementWithId("bus").setBounds(386.5, 110.5, 12.5, 7.5);
      surface.hardwareElementWithId("VCA").setBounds(386.5, 125.5, 12.5, 7.5);
      surface.hardwareElementWithId("all").setBounds(386.5, 140.75, 12.5, 7.5);
      surface.hardwareElementWithId("track_mode").setBounds(386.5, 17.5, 12.5, 7.5);
      surface.hardwareElementWithId("plugin_mode").setBounds(386.5, 32.5, 12.5, 7.5);
      surface.hardwareElementWithId("sends_mode").setBounds(386.5, 47.5, 12.5, 7.5);
      surface.hardwareElementWithId("pan_mode").setBounds(386.5, 61.75, 12.5, 7.5);
      surface.hardwareElementWithId("scroll_left").setBounds(413.75, 102.0, 12.75, 9.25);
      surface.hardwareElementWithId("scroll_right").setBounds(462.25, 102.0, 12.75, 9.25);
      surface.hardwareElementWithId("channel").setBounds(407.0, 125.5, 13.5, 8.5);
      surface.hardwareElementWithId("zoom").setBounds(427.5, 125.5, 13.5, 8.5);
      surface.hardwareElementWithId("scroll").setBounds(448.25, 125.5, 13.5, 8.5);
      surface.hardwareElementWithId("bank").setBounds(468.0, 125.5, 13.5, 8.5);
      surface.hardwareElementWithId("section").setBounds(448.25, 139.75, 13.5, 8.5);
      surface.hardwareElementWithId("marker").setBounds(468.5, 139.75, 13.5, 8.5);
      surface.hardwareElementWithId("display").setBounds(18.25, 22.75, 13.25, 11.5);
      surface.hardwareElementWithId("transport").setBounds(433.75, 94.75, 21.0, 21.75);
      surface.hardwareElementWithId("automation_write").setBounds(435.25, 80.25, 16.0, 7.5);
      surface.hardwareElementWithId("automation_touch").setBounds(412.25, 80.25, 16.0, 7.5);
      surface.hardwareElementWithId("automation_latch").setBounds(412.25, 62.25, 16.0, 7.5);
      surface.hardwareElementWithId("automation_on_off").setBounds(458.75, 62.25, 16.0, 7.5);
      surface.hardwareElementWithId("solo1").setBounds(59.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute1").setBounds(48.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select1").setBounds(50.0, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader1").setBounds(49.5, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo2").setBounds(79.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute2").setBounds(68.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select2").setBounds(70.5, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader2").setBounds(70.0, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo3").setBounds(99.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute3").setBounds(89.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select3").setBounds(90.75, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader3").setBounds(90.25, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo4").setBounds(120.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute4").setBounds(109.5, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select4").setBounds(111.25, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader4").setBounds(110.75, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo5").setBounds(140.5, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute5").setBounds(130.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select5").setBounds(131.5, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader5").setBounds(131.0, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo6").setBounds(160.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute6").setBounds(150.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select6").setBounds(152.0, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader6").setBounds(151.5, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo7").setBounds(181.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute7").setBounds(170.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select7").setBounds(172.5, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader7").setBounds(171.75, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo8").setBounds(201.5, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute8").setBounds(191.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select8").setBounds(192.75, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader8").setBounds(192.25, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo9").setBounds(222.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute9").setBounds(211.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select9").setBounds(213.25, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader9").setBounds(212.5, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo10").setBounds(242.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute10").setBounds(232.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select10").setBounds(233.75, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader10").setBounds(233.0, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo11").setBounds(262.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute11").setBounds(252.5, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select11").setBounds(254.0, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader11").setBounds(253.25, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo12").setBounds(283.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute12").setBounds(273.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select12").setBounds(274.5, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader12").setBounds(273.75, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo13").setBounds(303.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute13").setBounds(293.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select13").setBounds(295.0, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader13").setBounds(294.0, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo14").setBounds(323.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute14").setBounds(313.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select14").setBounds(315.25, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader14").setBounds(314.5, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo15").setBounds(344.0, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute15").setBounds(334.25, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select15").setBounds(335.75, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader15").setBounds(334.75, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("solo16").setBounds(364.5, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("mute16").setBounds(354.75, 81.25, 6.5, 7.0);
      surface.hardwareElementWithId("select16").setBounds(356.25, 61.75, 13.5, 10.0);
      surface.hardwareElementWithId("fader16").setBounds(355.25, 112.25, 14.25, 130.0);
      surface.hardwareElementWithId("display1").setBounds(49.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display1").setBounds(49.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display2").setBounds(69.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display2").setBounds(69.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display3").setBounds(89.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display3").setBounds(89.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display4").setBounds(110.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display4").setBounds(110.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display5").setBounds(130.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display5").setBounds(130.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display6").setBounds(150.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display6").setBounds(150.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display7").setBounds(171.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display7").setBounds(171.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display8").setBounds(191.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display8").setBounds(191.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display9").setBounds(211.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display9").setBounds(211.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display10").setBounds(232.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display10").setBounds(232.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display11").setBounds(252.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display11").setBounds(252.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display12").setBounds(272.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display12").setBounds(272.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display13").setBounds(292.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display13").setBounds(292.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display14").setBounds(313.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display14").setBounds(313.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display15").setBounds(333.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display15").setBounds(333.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display16").setBounds(353.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display16").setBounds(353.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("automation_trim").setBounds(435.25, 62.25, 16.0, 7.5);
      surface.hardwareElementWithId("automation_read").setBounds(458.75, 80.25, 16.0, 7.5);

   }
}
