package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplay;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.PostConstruct;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class ViewControl {
   public static final int NUM_PADS_TRACK = 8;
   private static final String ANALOG_LAB_V_DEVICE_ID = "4172747541564953416C617650726F63";

   private final TrackBank viewTrackBank;
   private final PinnableCursorDevice primaryDevice;
   private final MasterTrack masterTrack;
   private final CursorTrack cursorTrack;
   private final TrackBank mixerTrackBank;
   private final Scene sceneTrackItem;
   private final PinnableCursorDevice cursorDevice;
   private final Clip cursorClip;

   public ViewControl(final ControllerHost host, final LcdDisplay lcdDisplay) {
      masterTrack = host.createMasterTrack(2);
      mixerTrackBank = host.createTrackBank(8, 2, 2);
      cursorTrack = host.createCursorTrack(2, 2);

      viewTrackBank = host.createTrackBank(4, 2, 2);
      viewTrackBank.followCursorTrack(cursorTrack);
      mixerTrackBank.followCursorTrack(cursorTrack);

      sceneTrackItem = viewTrackBank.sceneBank().getScene(0);
      cursorClip = host.createLauncherCursorClip(32, 127);

      primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", NUM_PADS_TRACK,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);
      cursorDevice = cursorTrack.createCursorDevice("device-control", "Device Control", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);
      setUpFollowArturiaDevice(host);
   }

   @PostConstruct
   void init() {
      viewTrackBank.canScrollForwards().markInterested();
      viewTrackBank.canScrollBackwards().markInterested();
      cursorTrack.hasNext().markInterested();
      cursorTrack.hasPrevious().markInterested();
   }

   private void setUpFollowArturiaDevice(final ControllerHost host) {
      final DeviceMatcher arturiaMatcher = host.createVST3DeviceMatcher(ANALOG_LAB_V_DEVICE_ID);
      final DeviceBank matcherBank = cursorTrack.createDeviceBank(1);
      matcherBank.setDeviceMatcher(arturiaMatcher);
      final Device matcherDevice = matcherBank.getItemAt(0);
      matcherDevice.exists().markInterested();

      final BooleanValueObject controlsAnalogLab = new BooleanValueObject();

//        controlsAnalogLab.addValueObserver(controlsLab -> sysExHandler.fireArturiaMode(
//                controlsLab ? com.bitwig.extensions.controllers.arturia.minilab3.SysExHandler.GeneralMode.ANALOG_LAB : com.bitwig.extensions.controllers.arturia.minilab3.SysExHandler.GeneralMode.DAW_MODE,
//                arturiaModeLayer.isActive()));

      final BooleanValue onArturiaDevice = cursorDevice.createEqualsValue(matcherDevice);
      cursorTrack.arm()
         .addValueObserver(
            armed -> controlsAnalogLab.set(armed && cursorDevice.exists().get() && onArturiaDevice.get()));
      onArturiaDevice.addValueObserver(
         onArturia -> controlsAnalogLab.set(cursorTrack.arm().get() && cursorDevice.exists().get() && onArturia));
      cursorDevice.exists()
         .addValueObserver(cursorDeviceExists -> controlsAnalogLab.set(
            cursorTrack.arm().get() && cursorDeviceExists && onArturiaDevice.get()));
   }

   public Scene getSceneTrackItem() {
      return sceneTrackItem;
   }

   public TrackBank getViewTrackBank() {
      return viewTrackBank;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public PinnableCursorDevice getPrimaryDevice() {
      return primaryDevice;
   }

   public TrackBank getMixerTrackBank() {
      return mixerTrackBank;
   }

   public MasterTrack getMasterTrack() {
      return masterTrack;
   }

   public void invokeQuantize() {
      cursorClip.quantize(1.0);
      final ClipLauncherSlot slot = cursorClip.clipLauncherSlot();
      slot.showInEditor();
   }
}
