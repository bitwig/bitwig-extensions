package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

public class CursorDeviceControl {
   private final DeviceBank deviceBank;
   private final PinnableCursorDevice cursorDevice;

   private final CursorRemoteControlsPage remotes;
   private final CursorTrack cursorTrack;
   private final PinnableCursorDevice primaryDevice;

   private final DrumPadBank drumPadBank;
   private final CursorDeviceLayer drumCursor;
   private final DeviceBank drumDeviceBank;
   private final CursorDeviceLayer cursorLayer;
   private final Device cursorLayerItem;

   public CursorDeviceControl(final CursorTrack cursorTrack, final int size, final int totalChannelsAvailable) {
      this.cursorTrack = cursorTrack;
      cursorDevice = cursorTrack.createCursorDevice();
      primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);

      drumPadBank = primaryDevice.createDrumPadBank(totalChannelsAvailable);

      drumPadBank.setSkipDisabledItems(false);
      drumCursor = primaryDevice.createCursorLayer();

      drumDeviceBank = drumCursor.createDeviceBank(8);

      cursorLayer = cursorDevice.createCursorLayer();
      final DeviceBank layerBank = cursorLayer.createDeviceBank(1);
      cursorLayerItem = layerBank.getItemAt(0);
      cursorLayerItem.name().markInterested();

      cursorDevice.name().markInterested();
      cursorDevice.deviceType().markInterested();
      cursorDevice.isPinned().markInterested();
      cursorDevice.hasDrumPads().markInterested();
      cursorDevice.hasLayers().markInterested();
      cursorDevice.hasSlots().markInterested();
      cursorDevice.slotNames().markInterested();

      deviceBank = cursorDevice.deviceChain().createDeviceBank(8);
      deviceBank.canScrollBackwards().markInterested();
      deviceBank.canScrollForwards().markInterested();
      deviceBank.itemCount().markInterested();
      deviceBank.scrollPosition().markInterested();

      remotes = cursorDevice.createCursorRemoteControlsPage(8);

      for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
         final int index = i;
         final Device device = deviceBank.getDevice(index);
         device.deviceType().markInterested();
         device.name().markInterested();
         device.position().markInterested();
      }

      cursorDevice.position().addValueObserver(cp -> {
         if (cp >= 0) {
            deviceBank.scrollPosition().set(cp - 1);
         }
      });
   }

   public void moveDeviceLeft() {
      final Device previousDevice = deviceBank.getDevice(0);
      previousDevice.beforeDeviceInsertionPoint().moveDevices(cursorDevice);
      cursorDevice.selectPrevious();
      cursorDevice.selectNext();
   }

   public void moveDeviceRight() {
      final Device nextDevice = deviceBank.getDevice(2);
      nextDevice.afterDeviceInsertionPoint().moveDevices(cursorDevice);
      cursorDevice.selectPrevious();
      cursorDevice.selectNext();
   }

   public CursorDeviceLayer getDrumCursor() {
      return drumCursor;
   }

   public DrumPadBank getDrumPadBank() {
      return drumPadBank;
   }

   public DeviceBank getDrumDeviceBank() {
      return drumDeviceBank;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public DeviceBank getDeviceBank() {
      return deviceBank;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public CursorRemoteControlsPage getRemotes() {
      return remotes;
   }

   public Parameter getParameter(final int index) {
      return remotes.getParameter(index);
   }

   public void selectDevice(final Device device) {
      cursorDevice.selectDevice(device);
      cursorDevice.selectInEditor();
   }

   public void focusOnDrumDevice() {
      cursorDevice.selectDevice(drumDeviceBank.getDevice(0));
   }

   public void focusOnPrimary() {
      cursorDevice.selectDevice(primaryDevice);
   }

   public void handleLayerSelection() {
      cursorDevice.selectDevice(cursorLayerItem);
   }

   public void navigateNextInLayer() {
      cursorLayer.selectNext();
   }

   public void navigatePreviousInLayer() {
      cursorLayer.selectPrevious();
   }

   public Device getCursorLayerItem() {
      return cursorLayerItem;
   }

   public String getLayerDeviceInfo() {
      if (cursorDevice.hasLayers().get()) {
         return "SEL=" + cursorLayerItem.name().get();
      }
      return "";
   }

   public void navigateDevice(final int direction, final ModifierValueObject modState) {
      if (modState.isShiftSet()) {
         if (direction > 0) {
            cursorDevice.selectParent();
         } else {
            if (cursorDevice.hasDrumPads().get()) {
               focusOnDrumDevice();
            } else if (cursorDevice.hasLayers().get()) {
               cursorDevice.selectFirstInLayer(0);
            } else if (cursorDevice.hasSlots().get()) {
               final String[] slotNames = cursorDevice.slotNames().get();
               cursorDevice.selectFirstInSlot(slotNames[0]);
            }
         }
      } else {
         if (direction < 0) {
            cursorDevice.selectNext(); // Arrow Button Down is next device
         } else {
            cursorDevice.selectPrevious(); // Arrow Button UP ist previous Device
         }
      }
   }

   public boolean hasDrumPads() {
      return cursorDevice.hasDrumPads().get();
   }
}
