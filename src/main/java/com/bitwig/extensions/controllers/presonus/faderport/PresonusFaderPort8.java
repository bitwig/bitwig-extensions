package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;

public class PresonusFaderPort8 extends PresonusFaderPort
{

   public PresonusFaderPort8(final PresonusFaderPort8Definition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   protected void initHardwareLayout()
   {
//      mHardwareSurface.setPhysicalSize(500, 300);
      mHardwareSurface.setPhysicalSize(334, 301);

      final HardwareSurface surface = mHardwareSurface;

      surface.hardwareElementWithId("arm").setBounds(19.75, 63.25, 12.5, 7.5);
      surface.hardwareElementWithId("master").setBounds(242.5, 141.25, 13.5, 8.5);
      surface.hardwareElementWithId("shift_left").setBounds(19.5, 157.25, 12.0, 12.25);
      surface.hardwareElementWithId("shift_right").setBounds(221.0, 157.25, 13.25, 12.25);
      surface.hardwareElementWithId("play").setBounds(269.25, 192.25, 21.75, 21.25);
      surface.hardwareElementWithId("stop").setBounds(243.25, 188.0, 12.25, 13.5);
      surface.hardwareElementWithId("record").setBounds(304.25, 188.0, 12.25, 13.5);
      surface.hardwareElementWithId("metronome").setBounds(263.0, 141.25, 13.5, 8.5);
      surface.hardwareElementWithId("loop").setBounds(252.75, 169.25, 12.5, 12.25);
      surface.hardwareElementWithId("rewind").setBounds(273.75, 169.25, 12.5, 12.25);
      surface.hardwareElementWithId("fast_forward").setBounds(294.5, 169.25, 12.5, 12.25);
      surface.hardwareElementWithId("clear_solo").setBounds(19.75, 81.25, 12.5, 7.5);
      surface.hardwareElementWithId("clear_mute").setBounds(19.75, 96.0, 12.5, 7.5);
      surface.hardwareElementWithId("track_mode").setBounds(222.25, 19.25, 12.5, 7.5);
      surface.hardwareElementWithId("plugin_mode").setBounds(222.25, 34.25, 12.5, 7.5);
      surface.hardwareElementWithId("sends_mode").setBounds(222.25, 49.25, 12.5, 7.5);
      surface.hardwareElementWithId("pan_mode").setBounds(222.25, 63.5, 12.5, 7.5);
      surface.hardwareElementWithId("scroll_left").setBounds(248.75, 103.5, 12.75, 9.25);
      surface.hardwareElementWithId("scroll_right").setBounds(297.25, 103.5, 12.75, 9.25);
      surface.hardwareElementWithId("channel").setBounds(242.0, 127.0, 13.5, 8.5);
      surface.hardwareElementWithId("zoom").setBounds(262.5, 127.0, 13.5, 8.5);
      surface.hardwareElementWithId("scroll").setBounds(283.25, 127.0, 13.5, 8.5);
      surface.hardwareElementWithId("bank").setBounds(303.0, 127.0, 13.5, 8.5);
      surface.hardwareElementWithId("section").setBounds(283.25, 141.25, 13.5, 8.5);
      surface.hardwareElementWithId("marker").setBounds(303.5, 141.25, 13.5, 8.5);
      surface.hardwareElementWithId("display").setBounds(18.25, 22.75, 13.25, 11.5);
      surface.hardwareElementWithId("transport").setBounds(268.75, 96.25, 21.0, 21.75);
      surface.hardwareElementWithId("automation_on_off").setBounds(294.25, 63.75, 16.0, 7.5);
      surface.hardwareElementWithId("automation_write").setBounds(270.25, 81.75, 16.0, 7.5);
      surface.hardwareElementWithId("automation_touch").setBounds(247.25, 81.75, 16.0, 7.5);
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
      surface.hardwareElementWithId("display1").setBounds(49.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display2").setBounds(69.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display3").setBounds(89.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display4").setBounds(110.25, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display5").setBounds(130.5, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display6").setBounds(150.75, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display7").setBounds(171.0, 20.0, 16.0, 19.25);
      surface.hardwareElementWithId("display8").setBounds(191.25, 20.0, 16.0, 19.25);


   }

}
